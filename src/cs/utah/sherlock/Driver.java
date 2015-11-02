package cs.utah.sherlock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Tobin Yehle
 */
public class Driver {
    public static List<Question> readQuestions(String questionFile) {
        ArrayList<Question> questions = new ArrayList<>();
        try (Scanner in  = new Scanner(new File(questionFile))) {
            while(in.hasNext()) {
                String id = in.nextLine().split("\\s*:\\s*")[1];
                String question = in.nextLine().split("\\s*:\\s*")[1];
                int difficulty = in.nextLine().split("\\s*:\\s*")[1].equals("Easy") ? 0 : 1;

                questions.add(new Question(id, question, difficulty));

                in.skip("\\n");
            }
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return questions;
    }

    public static List<Question> readManyQuestions(List<String> inputFiles) {
        ArrayList<Question> questions =  new ArrayList<>();
        for(String inputFile : inputFiles) {
            questions.addAll(readQuestions(inputFile));
        }
        return questions;
    }

    public static void main(String[] args) {
        System.out.println("HI THERE! WHAT IS THIS DOiNG?");

        List<Question> questions = readQuestions("developset/1999-W02-5.questions");

        System.out.println(questions);
    }
}
