package cs.utah.sherlock;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
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

    public void processStory(Story story) {
        List<String> sentences = breakSentences(story.text);
        List<List<CoreLabel>> textTokens = sentences.stream().map(this::tokenizeSentence).collect(Collectors.toList());

        for(Story.Question question : story.questions) {
            List<CoreLabel> questionTokens = tokenizeSentence(question.question);

            // TODO: compare the question tokens to each sentence and pick one
        }
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
