package cs.utah.sherlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Dasha on 11/7/2015.
 */
public class Sherlock {

    public final Set<String> stopWords;
    public final List<Story> stories;

    public Sherlock(String stopWordsFile,  List<Story> stories ){
        this.stopWords = new HashSet<>(readLines(stopWordsFile));
        this.stories = stories;
    }

    public void processStories(){
        List<Set<String>> bagOfWords;

        for(Story story : this.stories){
            bagOfWords = getBagOfWords(story);
        }
    }

    /**
     * Generates sentence tokens from the given corpus
     * @param text
     * @return List of "sentences" (token list)
     */
    private List<List<String>> tokenize(String text){
        List<List<String>> sentenceTokens = new ArrayList<>();
        // TODO: Stanford tokenizer stuff
        return null;
    }

    /**
     * Generates a bag of words (set) for each sentence in the file (list)
     * @param story
     */
    private List<Set<String>> getBagOfWords(Story story){
        List<Set<String>> bagOfWords = new ArrayList<>();
        List<List<String>> sentences = tokenize(story.text);

        for(List<String> sentence : sentences){
            bagOfWords.add(new HashSet(sentence));
        }

        return bagOfWords;
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
