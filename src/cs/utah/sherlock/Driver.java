package cs.utah.sherlock;

import org.apache.xpath.SourceTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Driver {
    /**
     * Parses a single question file
     * @param questionFile The name of the file containing questions
     * @return A list of question objects
     */
    public static List<Story.Question> readQuestions(String questionFile, boolean hasAnswers) {
        ArrayList<Story.Question> questions = new ArrayList<>();
        try (Scanner in  = new Scanner(new File(questionFile))) {
            while(in.hasNextLine()) {
                String id = in.nextLine().split("\\s*:\\s*", -1)[1];

                String question = in.nextLine().split("\\s*:\\s*", -1)[1];

                String answer = hasAnswers ? in.nextLine().split("\\s*:\\s*", -1)[1] : null;

                int difficulty = in.nextLine().split("\\s*:\\s*", -1)[1].equals("Easy") ? 0 : 1;

                questions.add(new Story.Question(id, question, difficulty, answer));

                in.skip("\\n");
            }
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return questions;
    }

    public static Story readStory(String baseName) {
        try (Scanner in  = new Scanner(new File(baseName+".story"))) {
            String headline = in.nextLine().split("\\s*:\\s*", -1)[1];
            String date = in.nextLine().split("\\s*:\\s*", -1)[1];
            String id = in.nextLine().split("\\s*:\\s*", -1)[1];

            in.skip("\\n");
            in.skip("TEXT:\\n");
            in.skip("\\n");

            StringBuilder textBuilder = new StringBuilder();
            while(in.hasNextLine()) {
                textBuilder.append(in.nextLine());
                textBuilder.append(" ");
            }

            boolean answersExists = new File(baseName+".answers").exists();

            List<Story.Question> questions = answersExists ?
                    readQuestions(baseName+".answers", true) :
                    readQuestions(baseName+".questions", false);

            return new Story(headline, date, id, textBuilder.toString(), questions);
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public static List<Story> readStories(String prefix, List<String> ids) {
        return ids.stream().map(id -> readStory(prefix + id)).collect(Collectors.toList());
    }

    public static List<String> readInputFile(String filename) {
        List<String> ids = new ArrayList<>();
        try (Scanner in = new Scanner(new File(filename))) {
            if(in.hasNextLine()) ids.add(new File(in.nextLine()).getCanonicalPath() + File.separator);
            while(in.hasNextLine()) {
                ids.add(in.nextLine());
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return ids;
    }

    public static void alphabetizeManifest(String manifest){
        List<String> strings = new ArrayList<>();
        try (Scanner in = new Scanner(new File(manifest))) {
            if(in.hasNextLine()) strings.add(new File(in.nextLine()).getCanonicalPath() + File.separator);
            while(in.hasNextLine()) {
                strings.add(in.nextLine());
            }

            Collections.sort(strings);

            for(String s : strings)
                System.out.println(s);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static String answerQuestions(Story story, Sherlock sherlock){
        StringBuilder answers = new StringBuilder();

        Map<Story.Question, String> answerKey = sherlock.processStory(story);

        for(Story.Question question : story.questions) {
            String answer = answerKey.get(question);

            answers.append("QuestionID: " + question.id + "\n");
            answers.append("Answer: " + answer + "\n\n");
        }
        return answers.toString();
    }

    public static String generateAnswers(List<Story> stories){
        StringBuilder answers = new StringBuilder();
        Sherlock sherlock = new Sherlock("stop-words.txt");

        for(Story story : stories){
            answers.append(answerQuestions(story, sherlock));
        }

        return answers.toString();
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Expected input file");
            System.exit(1);
        }

        if(args.length > 1) {
            System.out.println("WARNING: More than one input file. Only the first file will be read.");
        }

        List<String> storyIDs = readInputFile(args[0]);
        String directory = storyIDs.remove(0);

        List<Story> stories = readStories(directory, storyIDs);

        System.out.println(generateAnswers(stories));
//        alphabetizeManifest("developset-manifest");
    }
}
