package cs.utah.sherlock;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Sherlock {

    public final Set<String> stopWords;

    public Sherlock(String stopWordsFile){
        this.stopWords = new HashSet<>(readLines(stopWordsFile));
    }

    public Map<Story.Question, String> processStory(Story story) {
        Map<Story.Question, String> questionAnswers = new HashMap<>();
        List<String> sentences = breakSentences(story.text);
        List<List<CoreLabel>> textTokens = sentences.stream().map(this::tokenizeSentence).collect(Collectors.toList());

        for(Story.Question question : story.questions) {
            List<CoreLabel> questionTokens = tokenizeSentence(question.question);
            Set<String> questionBag = getBagOfWords(questionTokens);

            int bestIntersectionSize = 0;
            String bestAnswer = "Canada";
            for(List<CoreLabel> sentence : textTokens){
                Set<String> sentenceBag = getBagOfWords(sentence);

                // Find the intersection of the questionBag and the sentenceBag
                Set<String> intersection = getBagOfWords(sentence);
                intersection.retainAll(questionBag);

                if(intersection.size() > bestIntersectionSize) {
                    bestIntersectionSize = intersection.size();
                    bestAnswer = rebuildSentence(sentence);
                }
                else if (sentenceBag.size() == bestIntersectionSize) {
                    // TODO: If the sizes are the same, prefer sentences earlier in the document and with longer words.
                }

            }
            questionAnswers.put(question, bestAnswer);
        }

        return questionAnswers;
    }

    private Set<String> getBagOfWords(List<CoreLabel> sentence){
        //Set<CoreLabel> coreLabels = new HashSet<>(sentence);
        Set<String> bagOfWords = sentence.stream().map(word -> word.word()).collect(Collectors.toSet());

        // Remove all stop words from the bag
        bagOfWords.removeAll(stopWords);

        return bagOfWords;
    }

    /**
     * Generates sentence tokens from the given corpus
     * @param document The document.
     * @return List of "sentences" (token list)
     */
    private List<String> breakSentences(String document) {
        return Arrays.asList(document.split("\\.|!|\\?"));
    }

    private List<CoreLabel> tokenizeSentence(String sentence) {
        return tokenizeString(sentence);
    }

    private String rebuildSentence(List<CoreLabel> sentence) {
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


    public static List<CoreLabel> tokenizeString(String input) {
        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(input),
                new CoreLabelTokenFactory(), "invertible=true");
        List<CoreLabel> out = new ArrayList<>();
        while(tokenizer.hasNext()) {
            out.add(tokenizer.next());
        }
        return out;
    }

    /***** HELPER FUNCTIONS *****/

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
