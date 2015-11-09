package cs.utah.sherlock;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;

import java.io.File;
import java.io.FileNotFoundException;
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

    public Sherlock(String stopWordsFile) {
        this.stopWords = new HashSet<>(readLines(stopWordsFile));
    }

    /**
     * Answers the the questions about a story.
     * @param story The story to answer questions about.
     * @return A map of the questions to the answers.
     */
    public Map<Story.Question, String> processStory(Story story) {
        Map<Story.Question, String> questionAnswers = new HashMap<>();

        List<String> sentences = breakSentences(story.text);

        // Create necessary objects for parsing
        String parserModel = "parser-models/englishPCFG.ser.gz";
        LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);

        List<List<CoreLabel>> textTokens = sentences.stream().map(Sherlock::tokenizeString).collect(Collectors.toList());

        for(Story.Question question : story.questions) {
            List<CoreLabel> questionTokens = tokenizeString(question.question);

            // Get NPs using sentence parsing
            List<Tree> questionNPs = getNounPhrases(lp.apply(questionTokens));
            // ArrayList<Word>
            //questionNPs.get(1).yieldWords();
            //parseQuestion.pennPrint();

            Set<String> questionBag = getBagOfWords(questionTokens, true);

            int bestIntersectionSize = 0;
            String bestAnswer = "Canada";
            for(List<CoreLabel> sentenceTokens : textTokens) {
                // Get NPs using sentence parsing
                List<Tree> sentenceNPs = getNounPhrases(lp.apply(sentenceTokens));
                //parseSentence.pennPrint();

                // Find the intersection of the questionBag and the sentenceBag
                Set<String> intersection = getBagOfWords(sentenceTokens, true);
                intersection.retainAll(questionBag);

                if(intersection.size() > bestIntersectionSize) {
                    bestIntersectionSize = intersection.size();
                    bestAnswer = rebuildSentence(sentenceTokens);
                }
                else if (intersection.size() == bestIntersectionSize && bestIntersectionSize != 0) {
                    // TODO: If the sizes are the same, prefer sentences earlier in the document and with longer words.
                    // For now prefer shorter sentences
                    String newAnswer = rebuildSentence(sentenceTokens);
                    if(newAnswer.length() < bestAnswer.length()) {
                        bestIntersectionSize = intersection.size();
                        bestAnswer = newAnswer;
                    }
                }

            }
            questionAnswers.put(question, bestAnswer);
        }

        return questionAnswers;
    }

    private List<Tree> getNounPhrases(Tree parent)
    {
        List<Tree> nounPhrases = parent.stream().filter(child -> child.label().value().equals("NP")).collect(Collectors.toList());

//        for (Tree child : parent)
//            if(child.label().value().equals("NP"))
//                nounPhrases.add(child);

        return nounPhrases;
    }

    /**
     * Turns a list of tokens into a set of strings.
     * @param sentence The tokens to bag
     * @param stem If the words in the bag should be the stems of the original words
     * @return the bag of words
     */
    private Set<String> getBagOfWords(List<CoreLabel> sentence, boolean stem) {
        //Set<CoreLabel> coreLabels = new HashSet<>(sentence);

        Morphology morph = new Morphology();
        Function<CoreLabel, String> extractor = stem ? word -> morph.stem(word.word()) : CoreLabel::word;
        Set<String> bagOfWords = sentence.stream().map(extractor).collect(Collectors.toSet());

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

    /**
     * Reads lines from a file
     * @param filename The name of the file to read
     * @return A list of all the lines in the file
     */
    private static List<String> readLines(String filename) {
        ArrayList<String> out = new ArrayList<>();
        try(Scanner in  = new Scanner(new File(filename))) {
            while(in.hasNextLine()) {
                out.add(in.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }
}
