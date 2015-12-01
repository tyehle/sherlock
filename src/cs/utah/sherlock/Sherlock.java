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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Sherlock {

    public final Set<String> stopWords;
    private final Set<String> questionWords;

    private final double baggingWeight = 3;
    private final int clue = 3, good_clue = 4, confident = 6, slam_dunk = 20;

    private final Set<String> verbTags;
    private final Set<String> locationPrepositions;
    private final Set<List<String>> monthNames;
    private final Set<List<String>> days;

    private Map<String, Set<String>> nerFilter;
    private StanfordCoreNLP pipeline;
    private double verbWeight;
    private Morphology morph;

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

        locationPrepositions = Util.setOf("in", "at", "near", "inside"); // TODO: Make this list bigger

        questionWords = Util.setOf("who", "where", "when", "what", "why", "how");

        // build the ner filter
        // NER-TAGS: Location, Person, Organization, Money, Percent, Date, Time
        nerFilter = Util.mapOf(Util.pairOf("who", Util.setOf("PERSON", "ORGANIZATION")),
                               Util.pairOf("where", Util.setOf("LOCATION", "ORGANIZATION")),
                               Util.pairOf("when", Util.setOf("DATE", "TIME")),
                               Util.pairOf("how", Util.setOf("MONEY", "PERCENT"))); // TODO: Change this to how much

        monthNames = makePhrases(Util.setOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"));
        days = makePhrases(Util.setOf("today", "yesterday", "tomorrow"));
        days.add(Util.listOf("last", "night"));

        morph = new Morphology();
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
            CoreMap annotatedQuestion = getSentence(annotationObject, 0);

            String questionType = getQuestionType(annotatedQuestion);

            List<CoreLabel> bestSentence = findBestSentence(annotatedQuestion, document);

            // Might remove everything
            List<CoreLabel> filtered = applyNERFilter(questionType, bestSentence);

            // Best is not necessarily the whole sentence; it might be just people/organizations, depending on the question
            List<CoreLabel> best = filtered.size() > 0 ? filtered : bestSentence;

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
    private List<CoreLabel> findBestSentence(CoreMap question, Annotation document) {

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        double bestScore = 0;
        int bestSize = 0;
        int bestIndex = -1;
        for(int sentenceNum = 0; sentenceNum < sentences.size(); sentenceNum++) {
            CoreMap sentence = getSentence(document, sentenceNum);

            // Gives you the score based on intersection after bagging
            double score = getPointsByBagging(document, sentenceNum, question);

            score += getPointsByQuestionType(question, sentence);

            int sentenceSize = replaceCorefMentions(document, sentenceNum).size();
            // TODO: If the sizes are the same, prefer sentences earlier in the document and with longer words.
            // For now prefer shorter sentences
            if(score > bestScore ||
                    (score == bestScore && sentenceSize < bestSize)) {
                bestIndex = sentenceNum;
                bestScore = score;
                bestSize = sentenceSize;
            }
        }

//        return replaceCorefMentions(document, bestIndex);
        return getTokens(getSentence(document, bestIndex));
    }

    /**
     * Calculate intersection of bagged words to compute a score
     * @param document All sentences
     * @param sentenceNum The sentence to consider
     * @param question The question to consider
     * @return score
     */
    private double getPointsByBagging(Annotation document, int sentenceNum, CoreMap question){
        List<CoreLabel> tokensCopy = new ArrayList<>(getTokens(question));
        tokensCopy.remove(0);
        Set<String> questionBag = getBagOfWords(tokensCopy);

        // Split into lists of verbs and not verbs
        Util.Pair<List<CoreLabel>, List<CoreLabel>> verbNotVerb = getVerbsAndNotVerbs(replaceCorefMentions(document, sentenceNum));

        // Weigh the verbs higher than words that are not verbs, as per Ellen's paper
        Set<String> verbIntersection = getBagOfWords(verbNotVerb.first());
        Set<String> notVerbIntersection = getBagOfWords(verbNotVerb.second());
        verbIntersection.retainAll(questionBag);
        notVerbIntersection.retainAll(questionBag);

        return baggingWeight*(verbIntersection.size()*verbWeight + notVerbIntersection.size());
    }

    private double getPointsByQuestionType(CoreMap question, CoreMap sentence){
        String questionType = getQuestionType(question);

        switch (questionType) {
            case "what":
                return getPointsForWhat(question, sentence);
            case "who":
                return getPointsForWho(question, sentence);
            case "where":
                return getPointsForWhere(question, sentence);
            case "when":
                return getPointsForWhen(question, sentence);
            case "why":
                return getPointsForWhy(question, sentence);
            case "how":
                return getPointsForHow(question, sentence);
            default:
                //System.out.println("Question type not found: " + questionType);
                return 0;
        }
    }


    private int getPointsForWhat(CoreMap question, CoreMap sentence){
        int score = 0;

        // If question contains month AND sentence contains today, yesterday, tomorrow, or last night, then it's a clue
        if(sentenceContainsAny(monthNames, question) && sentenceContainsAny(days, sentence)){
            score += clue;
        }

        // If question contains kind AND sentence contains call or from, then it's a good clue
        if(sentenceContainsAny(Util.setOf(Util.listOf("kind")), question)
                && sentenceContainsAny(makePhrases(Util.setOf("call", "from")), sentence))
            score += good_clue;

        // If question contains name AND sentence contains name, call, or known, then it's a slam dunk
        if(sentenceContainsAny(Util.setOf(Util.listOf("name")), question)
                && sentenceContainsAny(makePhrases(Util.setOf("name", "call", "from")), sentence))
            score += slam_dunk;

        // If question contains name+PP AND sentence contains proper noun AND proper noun contains head(PP), then it's a slam dunk
        // TODO: Finish this if

        return score;
    }

    private int getPointsForWho(CoreMap question, CoreMap sentence){
        int score = 0;
        // If question doesn't contain NAME AND sentence contains NAME, then we're confident
        if(!containsNamedEntity(Util.setOf("PERSON", "ORGANIZATION"), question)
                && containsNamedEntity(Util.setOf("PERSON", "ORGANIZATION"), sentence))
            score += confident;

        // If question doesn't contain NAME AND sentence contains name, then it's a good clue
        if(!containsNamedEntity(Util.setOf("PERSON", "ORGANIZATION"), question)
                && sentenceContainsAny(Util.setOf(Util.listOf("name")), sentence))
            score += good_clue;

        // If sentence contains NAME or HUMAN, then it's a good clue
        if(containsNamedEntity(Util.setOf("PERSON", "ORGANIZATION"), question))
            score += good_clue;

        return score;
    }

    private double getPointsForWhere(CoreMap question, CoreMap sentence){
        int score = 0;

        // If sentence contains LocationPrep, good clue
        if(sentenceContainsAny(makePhrases(locationPrepositions), sentence))
            score += good_clue;

        // If sentence contains LOCATION, confident
        if(containsNamedEntity(Util.setOf("LOCATION", "ORGANIZATION"), sentence))
            score += confident;

        return score;
    }

    private double getPointsForWhen(CoreMap question, CoreMap sentence){
        int score = 0;

        // If sentence contains TIME, good_clue
        if(containsNamedEntity(Util.setOf("DATE", "TIME"), sentence))
            score += good_clue;

        // If question contains "the last" AND sentence contains first, last, since, or ago, slam_dunk
        if(sentenceContainsAny(Util.setOf(Util.listOf("the", "last")), question) && sentenceContainsAny(makePhrases(Util.setOf("first", "last", "since", "ago")), sentence))
            score += slam_dunk;

        // If question contains start or begin AND sentence contains start, begin, since, or year, slam_dunk
        if(sentenceContainsAny(makePhrases(Util.setOf("start", "begin")), question) && sentenceContainsAny(makePhrases(Util.setOf("start", "begin", "since", "year")), sentence))
            score += slam_dunk;

        return score;
    }

    private double getPointsForWhy(CoreMap question, CoreMap sentence){
        int score = 0;

        // TODO: not really sure how to implement this with our current framework

        return score;
    }

    private double getPointsForHow(CoreMap question, CoreMap sentence) {
        double score = 0;

        if(sentenceContainsAny(makePhrases(Util.setOf("much", "many")), question) && containsNamedEntity(Util.setOf("MONEY", "PERCENT"), sentence))
            score += confident;

        return score;
    }

    /**
     * Checks if any of a set of phrases exist in a sentence.
     * @param phrases The phrases to check for
     * @param sentence The sentence to check in
     * @return If any of the phrases were in the sentence
     */
    private boolean sentenceContainsAny(Set<List<String>> phrases, CoreMap sentence){
        List<String> tokens = getTokens(sentence).stream().map(this::stem).collect(Collectors.toList());

        for(int tokenNum = 0; tokenNum < tokens.size(); tokenNum++){
            for(List<String> phrase : phrases){
                if(tokens.size() >= tokenNum + phrase.size() &&
                        tokens.subList(tokenNum, tokenNum+phrase.size()).equals(phrase.stream()
                                .map(this::stem)
                                .collect(Collectors.toList())))
                    return true;
            }
        }

        return false;
    }

    /**
     * Checks inf a named entity tag exists in a sentence.
     * @param nerTags The NER tags to check for
     * @param sentence The sentence to check in
     * @return If any of the words in the sentence were tagged with the given NER tags
     */
    private boolean containsNamedEntity(Set<String> nerTags, CoreMap sentence){
        List<String> tokenNerTags = getTokens(sentence).stream().map(token -> token.get(CoreAnnotations.NamedEntityTagAnnotation.class))
                .collect(Collectors.toList());
        tokenNerTags.retainAll(nerTags);
        return tokenNerTags.size() > 0;
    }

    /**
     * Builds phrases out of single words.
     * @param words The words to build the phrases from
     * @return A set of lists, each of which contains a single word
     */
    private Set<List<String>> makePhrases(Set<String> words){
        return words.stream().map(Util::listOf).collect(Collectors.toSet());
    }

    /**
     * Get the sentence at the index in the document
     * @param document All sentences
     * @param index The index of the sentence to get
     * @return the target sentence
     */
    private CoreMap getSentence(Annotation document, int index) {
        return document.get(CoreAnnotations.SentencesAnnotation.class).get(index);
    }

    /**
     * Get the tokens from the core map
     * @param annotatedSentence The sentence to the the tokens from
     * @return List of tokens in the core map
     */
    private List<CoreLabel> getTokens(CoreMap annotatedSentence) {
        return annotatedSentence.get(CoreAnnotations.TokensAnnotation.class);
    }

    /**
     * Gets the stem of the given word.
     * @param word The word to stem
     * @return The stem of the word
     */
    private String stem(CoreLabel word){
        return morph.stem(word.word());
    }

    /**
     * Gets the stem of the given word.
     * @param word The word to stem
     * @return The stem of the word
     */
    private String stem(String word){
        return morph.stem(word);
    }

    /**
     * Find the type of question asked by a given sentence
     * @param question The question to look at
     * @return The type of the question. This is usually the first word of the question, ie. Why.
     */
    // TODO: Change this to figure out question type more smartly
    private String getQuestionType(CoreMap question) {
        Stream<String> words = getTokens(question).stream().map(word -> word.word().toLowerCase());
        Stream<String> askingWords = words.filter(questionWords::contains);
        Optional<String> firstAsk = askingWords.findFirst();

        return firstAsk.orElseGet(() -> getTokens(question).get(0).word());
    }

    /**
     * Gets a sentence where all co-referent mentions are replaced with their representative mention.
     * @param document The document to use
     * @param sentenceIndex The index of the sentence to do stuff with.
     * @return The sentence with replaced mentions
     */
    private List<CoreLabel> replaceCorefMentions(Annotation document, int sentenceIndex) {
        Collection<CorefChain> chains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();

        List<Util.Pair<Util.Pair<Integer, Integer>, List<CoreLabel>>> toReplace = Util.listOf();

        // Find all replacements we might have to do
        for(CorefChain chain : chains) {
            CorefChain.CorefMention representative = chain.getRepresentativeMention();

            if(representative.sentNum - 1 == sentenceIndex)
                continue;

            for(CorefChain.CorefMention mention : chain.getMentionsInTextualOrder()) {
                if(mention.sentNum - 1 == sentenceIndex) {
                    List<CoreLabel> tokens = getTokensBetween(document, representative.sentNum - 1, representative.startIndex - 1, representative.endIndex - 1);
                    toReplace.add(Util.pairOf(Util.pairOf(mention.startIndex - 1, mention.endIndex - 1), tokens));
                }
            }
        }

        // sort by start index
        toReplace.sort((a, b) -> a.first().first().compareTo(b.first().first())); //////// WUT

        List<CoreLabel> sentence = getTokens(getSentence(document, sentenceIndex));


        // Build the new sentence
        if(toReplace.isEmpty()) {
            return sentence;
        }
        else {
            List<CoreLabel> output = new ArrayList<>(sentence.subList(0, toReplace.get(0).first().first()));

            while (!toReplace.isEmpty()) {
                output.addAll(toReplace.get(0).second());

                // Remove all replacements that overlap with this one
                while(toReplace.size() > 1 && toReplace.get(1).first().first() < toReplace.get(0).first().second())
                    toReplace.remove(1);

                // Add the tokens in between this one and the next one
                if(toReplace.size() == 1) {
                    output.addAll(sentence.subList(toReplace.get(0).first().second(), sentence.size()));
                }
                else {
                    output.addAll(sentence.subList(toReplace.get(0).first().second(), toReplace.get(1).first().first()));
                }

                toReplace.remove(0);
            }

            return output;
        }
    }

    // TODO: Maybe use this to try to find sentences that are coreferent with the question
    /**
     * Tells if some sentences contain coreferent items.
     * @param document The document
     * @param sentences The potentially coreferent sentences
     * @return If the sentences all contained the same coreferent element
     */
    private boolean areCoreferent(Annotation document, int... sentences) {
        Collection<CorefChain> chains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();
        for(CorefChain chain : chains) {
            List<Integer> sentenceNumbers = chain.getMentionsInTextualOrder().stream()
                    .map(mention -> mention.sentNum - 1)
                    .collect(Collectors.toList());
            if(sentenceNumbers.containsAll(Arrays.asList(sentences)))
                return true;
        }
        return false;
    }

    /**
     * Gets some tokens from a sentence in a document
     * @param document All sentences
     * @param sentenceNumber The sentence number to extract tokens from
     * @param start The token start index
     * @param end The token end index
     * @return A chunk of the tokens in the sentence
     */
    private List<CoreLabel> getTokensBetween(Annotation document, int sentenceNumber, int start, int end) {
        return getTokens(getSentence(document, sentenceNumber)).subList(start, end);
    }

    /**
     * Applies a filter to a sentence based on the NER tags of the tokens. Does nothing if the given key was not found.
     * @param key The key to use when looking for an NER filter
     * @param sentence The sentence to filter
     * @return All the words matching the allowed annotations, or the sentence if the key was not valid
     */
    private List<CoreLabel> applyNERFilter(String key, List<CoreLabel> sentence) {
        if(nerFilter.containsKey(key)) {
            return sentence.stream().filter(token -> {
                String nerTag = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                return nerFilter.get(key).contains(nerTag);
            }).collect(Collectors.toList());
        }
        else {
            return sentence;
        }
    }

    /**
     * Turns a list of tokens into a set of strings.
     * @param sentence The tokens to bag
     * @return the bag of words
     */
    private Set<String> getBagOfWords(List<CoreLabel> sentence) {
        Set<String> bagOfWords = sentence.stream().map(this::stem).collect(Collectors.toSet());

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
     * @return A pair of all the verbs in the sentence, and all the other words in the sentence
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
