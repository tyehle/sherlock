package cs.utah.sherlock;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Sherlock {

    public final Set<String> stopWords;
    private AbstractSequenceClassifier<CoreLabel> ner;

    private Map<String, Set<String>> nerFilter;
    private StanfordCoreNLP pipeline;

    public Sherlock(String stopWordsFile, String nerModelFile) throws IOException, ClassNotFoundException {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        // using ner "muc7" model
        props.put("ner.model", "ner-models/english.muc.7class.distsim.crf.ser.gz");

        props.put("annotators", "tokenize, ssplit, ner, parse, dcoref");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");

        pipeline = new StanfordCoreNLP(props);

        ner = CRFClassifier.getClassifier(nerModelFile);

        this.stopWords = new HashSet<>(Util.readLines(stopWordsFile));

        // build the ner filter
        nerFilter = Util.mapOF(Util.pairOf("who", Util.setOf("PERSON", "ORGANIZATION")),
                               Util.pairOf("where", Util.setOf("LOCATION", "ORGANIZATION")));
    }


    // TODO: Use coreference resolution to replace later instances with original, this will improve bagging
    // TODO: Get wordnet, use it to semantic classification of the head noun in each NP

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
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        //List<String> sentences = breakSentences(story.text);
        //List<List<CoreLabel>> textTokens = sentences.stream().map(Sherlock::tokenizeString).collect(Collectors.toList());

        // answer each question
        for(Story.Question question : story.questions) {
            List<CoreLabel> questionTokens = tokenizeString(question.question);
            String questionType = questionTokens.get(0).word().toLowerCase();

            // ignore the first word of the question when doing bagging
            questionTokens.remove(0);

            // TODO: calculate a bag of words that also contains all coreferent terms
            CoreMap bestSentence = findBestByBagging(questionTokens, sentences);

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
     * @param sentences All the sentences in the document
     * @return The best sentence in the document
     */
    private CoreMap findBestByBagging(List<CoreLabel> question, List<CoreMap> sentences) {
        Set<String> questionBag = getBagOfWords(question);

        int bestIntersectionSize = 0;
        CoreMap bestAnswer = null;

        for(CoreMap sentence : sentences) {
            Set<String> intersection = getBagOfWords(sentence.get(CoreAnnotations.TokensAnnotation.class));
            intersection.retainAll(questionBag);

            // TODO: If the sizes are the same, prefer sentences earlier in the document and with longer words.
            // For now prefer shorter sentences
            if(intersection.size() > bestIntersectionSize ||
                    (intersection.size() == bestIntersectionSize && bestAnswer != null && sentence.size() < bestAnswer.size())) {
                bestAnswer = sentence;
                bestIntersectionSize = intersection.size();
            }
        }

        return bestAnswer;
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
                String annotation = token.get(CoreAnnotations.AnswerAnnotation.class);
                return nerFilter.get(key).contains(annotation);
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

    /***** HELPER FUNCTIONS *****/

    /**
     * Generates sentence tokens from the given corpus
     * @param document The document.
     * @return List of "sentences" (token list)
     */
    public static List<String> breakSentences(String document) {
        return Arrays.asList(document.split("\\.|!|\\?"));
    }

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

    /**
     * Builds a list of tokens from a string of input.
     * @param input The string to tokenize
     * @return The list of tokens
     */
    public static List<CoreLabel> tokenizeString(String input) {
        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(input),
                new CoreLabelTokenFactory(), "invertible=true");
        List<CoreLabel> out = new ArrayList<>();
        while(tokenizer.hasNext()) {
            out.add(tokenizer.next());
        }
        return out;
    }
}
