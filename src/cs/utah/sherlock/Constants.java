package cs.utah.sherlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author Tobin Yehle
 */
public class Constants {
    public static final Set<String> STOP_WORDS = new HashSet<>(readLines("stop-words.txt"));

    /**
     * Reads lines from a file
     * @param filename The name of the file to read
     * @return A list of all the lines in the file
     */
    public static List<String> readLines(String filename) {
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
