package cs.utah.sherlock;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Sherlock {

    public final Set<String> stopWords;
    private final Set<String> verbTags;

    private Map<String, Set<String>> nerFilter;
    private StanfordCoreNLP pipeline;
    private double verbWeight;

    public Sherlock(String stopWordsFile) throws IOException, ClassNotFoundException {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        // using ner "muc7" model
        props.put("ner.model", "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz");

        props.put("annotators", "tokenize, ssplit, pos, ner, parse, dcoref");
//        props.put("annotators", "tokenize, ssplit, pos, ner");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");

        verbWeight = 2; // TODO: DOOO SOME CV

        pipeline = new StanfordCoreNLP(props);

        this.stopWords = new HashSet<>(Util.readLines(stopWordsFile));

        this.verbTags = Util.setOf("VB", "VBD", "VBG", "VBN", "VBP", "VBZ");

        // build the ner filter
        // NER-TAGS: Location, Person, Organization, Money, Percent, Date, Time
        nerFilter = Util.mapOf(Util.pairOf("who", Util.setOf("PERSON", "ORGANIZATION")),
                               Util.pairOf("where", Util.setOf("LOCATION", "ORGANIZATION")),
                               Util.pairOf("when", Util.setOf("DATE", "TIME")),
                               Util.pairOf("how", Util.setOf("MONEY", "PERCENT"))); // TODO: Change this to how much
    }


    // TODO: Get wordnet, use it to semantic classification of the head noun in each NP
    // TODO: Implement Ellen's rules for different question types

    /**
     * Answers the the questions about a story.
     * @param story The story to answer questions about.
     * @return A map of the questions to the answers.
     */
    public Map<Story.Question, String> processStory(Story story) {
        Map<Story.Question, String> questionAnswers = new HashMap<>();

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(story.text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types

        // answer each question
        for(Story.Question question : story.questions) {
            // run the question through the pipeline
            Annotation annotationObject = new Annotation(question.question);
            pipeline.annotate(annotationObject);
            CoreMap annotatedQuestion = annotationObject.get(CoreAnnotations.SentencesAnnotation.class).get(0);

            List<CoreLabel> questionTokens = annotatedQuestion.get(CoreAnnotations.TokensAnnotation.class);

            String questionType = questionTokens.get(0).word().toLowerCase();

            // ignore the first word of the question when doing bagging
            questionTokens.remove(0);

            CoreMap bestSentence = findBestByBagging(annotatedQuestion, document);

            // Might remove everything
            List<CoreLabel> filtered = applyNERFilter(questionType, bestSentence);

            // Best is not necessarily the whole sentence; it might be just people/organizations, depending on the question
            List<CoreLabel> best = filtered.size() > 0 ? filtered : bestSentence.get(CoreAnnotations.TokensAnnotation.class);

            questionAnswers.put(question, rebuildSentence(best));
        }

        return questionAnswers;
    }

    /**
     * Finds the best sentence in the document by comparing bags of words in the question to bags of words of each
     * sentence.
     * @param question The question to compare with
     * @param document All the sentences in the document
     * @return The best sentence in the document
     */
    private CoreMap findBestByBagging(CoreMap question, Annotation document) {
        Set<String> questionBag = getBagOfWords(question.get(CoreAnnotations.TokensAnnotation.class));

        Collection<CorefChain> corefChains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();

        Map<List<Integer>, List<CoreLabel>> corefMap = Util.mapOf();

        for(CorefChain chain : corefChains) {
            // Get the list of indices which corefer to the given chain
            List<Integer> sentenceIndices = chain.getMentionsInTextualOrder().stream()
                    .map(mention -> mention.sentNum - 1)
                    .collect(Collectors.toList());
            // Get the list of tokens which corefer to the given chain
            List<CoreLabel> tokens = findAllMentions(document, chain);
            corefMap.put(sentenceIndices, tokens);
        }

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        double bestScore = 0;
        int bestSize = 0;
        CoreMap bestAnswer = null;
        for(int sentenceNum = 0; sentenceNum < sentences.size(); sentenceNum++){
            CoreMap sentence = sentences.get(sentenceNum);

            // Find which tokens this sentence contains references to
            List<CoreLabel> corefTokens = new ArrayList<>();
            for(Map.Entry<List<Integer>, List<CoreLabel>> entry : corefMap.entrySet()) {
                List<Integer> indices = entry.getKey();
                if(indices.contains(sentenceNum)) {
                    corefTokens.addAll(entry.getValue());
                }
            }

            // Split into lists of verbs and not verbs
            corefTokens.addAll(sentence.get(CoreAnnotations.TokensAnnotation.class));
            Util.Pair<List<CoreLabel>, List<CoreLabel>> verbNotVerb = getVerbsAndNotVerbs(corefTokens);

            // Weigh the verbs higher than words that are not verbs, as per Ellen's paper
            int sentenceSize = verbNotVerb.first().size() + verbNotVerb.second().size();
            Set<String> verbIntersection = getBagOfWords(verbNotVerb.first());
            Set<String> notVerbIntersection = getBagOfWords(verbNotVerb.second());
            verbIntersection.retainAll(questionBag);
            notVerbIntersection.retainAll(questionBag);

            double score = verbIntersection.size()*verbWeight + notVerbIntersection.size();

            // TODO: If the sizes are the same, prefer sentences earlier in the document and with longer words.
            // For now prefer shorter sentences
            if(score > bestScore ||
                    (score == bestScore && bestAnswer != null && sentenceSize < bestSize)) {
                bestAnswer = sentence;
                bestScore = score;
                bestSize = sentenceSize;
            }
        }

        return bestAnswer;
    }

    /**
     * Finds all the mentions of the chain in the document
     * @param document
     * @param chain
     * @return
     */
    private List<CoreLabel> findAllMentions(Annotation document, CorefChain chain) {
        return chain.getMentionsInTextualOrder().stream()
                .flatMap(mention -> getTokensBetween(document, mention.sentNum - 1, mention.startIndex - 1, mention.endIndex - 1).stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets some tokens from a sentence in a document
     * @param document
     * @param sentenceNumber
     * @param start The token start index
     * @param end The token end index
     * @return A chunk of the tokens in the sentence
     */
    private List<CoreLabel> getTokensBetween(Annotation document, int sentenceNumber, int start, int end) {
        CoreMap sentence = document.get(CoreAnnotations.SentencesAnnotation.class).get(sentenceNumber);
        return sentence.get(CoreAnnotations.TokensAnnotation.class).subList(start, end);
    }

    /**
     * Applies a filter to a sentence based on the NER tags of the tokens. Does nothing if the given key was not found.
     * @param key The key to use when looking for an NER filter
     * @param sentence The sentence to filter
     * @return All the words matching the allowed annotations, or the sentence if the key was not valid
     */
    private List<CoreLabel> applyNERFilter(String key, CoreMap sentence) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        if(nerFilter.containsKey(key)) {
            return tokens.stream().filter(token -> {
                String nerTag = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                return nerFilter.get(key).contains(nerTag);
            }).collect(Collectors.toList());
        }
        else {
            return tokens;
        }
    }

    /**
     * Turns a list of tokens into a set of strings.
     * @param sentence The tokens to bag
     * @return the bag of words
     */
    private Set<String> getBagOfWords(List<CoreLabel> sentence) {
        Morphology morph = new Morphology();
        Function<CoreLabel, String> stemmer = word -> morph.stem(word.word());
        Set<String> bagOfWords = sentence.stream().map(stemmer).collect(Collectors.toSet());

        // Remove all stop words from the bag
        bagOfWords.removeAll(stopWords);
        return bagOfWords;
    }

    /* [class edu.stanford.nlp.ling.CoreAnnotations$TextAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetBeginAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetEndAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$TokensAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$TokenBeginAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$TokenEndAnnotation,
    class edu.stanford.nlp.ling.CoreAnnotations$SentenceIndexAnnotation,
    class edu.stanford.nlp.trees.TreeCoreAnnotations$TreeAnnotation,
    class edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations$CollapsedDependenciesAnnotation,
    class edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations$BasicDependenciesAnnotation,
    class edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations$CollapsedCCProcessedDependenciesAnnotation,
    class edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations$AlternativeDependenciesAnnotation */

    /**
     * Returns a pair of lists: the tokens that are verbs and the tokens that are not verbs, respectively.
     * @param sentence - the sentence tokens plus the coreference tokens
     * @return
     */
    private Util.Pair<List<CoreLabel>, List<CoreLabel>> getVerbsAndNotVerbs(List<CoreLabel> sentence){
        List<CoreLabel> verbs = Util.listOf();
        List<CoreLabel> notVerbs = Util.listOf();

        // Get words that match our verb tags, and not
        for(CoreLabel word : sentence){
            if(verbTags.contains(word.get(CoreAnnotations.PartOfSpeechAnnotation.class)))
                verbs.add(word);
            else
                notVerbs.add(word);
        }

        return Util.pairOf(verbs, notVerbs);
    }



    /***** HELPER FUNCTIONS *****/

    /**
     * Rebuilds a sentence from a list of tokens.
     * This requires the tokens to have been tokenized with the invertible flag
     * @param sentence The list of tokens to reconstruct
     * @return The sentence the list of tokens came from
     */
    public static String rebuildSentence(List<CoreLabel> sentence) {
        StringBuilder out = new StringBuilder();

        out.append(sentence.get(0).get(CoreAnnotations.BeforeAnnotation.class));

        for(CoreLabel label : sentence) {
            String originalWord = label.get(CoreAnnotations.OriginalTextAnnotation.class);
            String spaceAfter = label.get(CoreAnnotations.AfterAnnotation.class);

            out.append(originalWord);
            out.append(spaceAfter);
        }

        return out.toString();
    }
}
