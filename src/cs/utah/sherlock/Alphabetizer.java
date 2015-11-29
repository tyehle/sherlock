package cs.utah.sherlock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * @author Tobin Yehle
 */
public class Alphabetizer {
    public static void main(String[] args) {
        String manifest = "all-data-manifest";

        List<String> lines = new ArrayList<>();

        try (Scanner in = new Scanner(new File(manifest))) {
            if(in.hasNextLine()) lines.add(new File(in.nextLine()).getCanonicalPath() + File.separator);
            while(in.hasNextLine()) {
                lines.add(in.nextLine());
            }

            Collections.sort(lines);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(new File(manifest)))) {
            for(String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
