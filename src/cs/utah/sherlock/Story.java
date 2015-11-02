package cs.utah.sherlock;

import java.util.List;

/**
 * @author Tobin Yehle
 * @author Dasha Pruss
 */
public class Story {
    public static class Question {
        public final String id, question, answer;
        public final int difficulty;

        public Question(String id, String question, int difficulty) {
            this(id, question, difficulty, null);
        }

        public Question(String id, String question, int difficulty, String answer) {
            this.id = id;
            this.question = question;
            this.difficulty = difficulty;
            this.answer = answer;
        }

        @Override
        public String toString() {
            return "Question{" +
                    "id='" + id + '\'' +
                    ", question='" + question + '\'' +
                    ", answer='" + answer + '\'' +
                    ", difficulty=" + difficulty +
                    '}';
        }
    }

    public final String headline, date, id, text;
    public final List<Question> questions;

    public Story(String headline, String date, String id, String text, List<Question> questions) {
        this.headline = headline;
        this.date = date;
        this.id = id;
        this.text = text;
        this.questions = questions;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    @Override

    public String toString() {
        return "Story{" +
                "headline='" + headline + '\'' +
                ", date='" + date + '\'' +
                ", id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", questions=" + questions +
                '}';
    }

    public String getId() {
        return id;
    }
}
