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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Question question1 = (Question) o;

            if (difficulty != question1.difficulty) return false;
            if (id != null ? !id.equals(question1.id) : question1.id != null) return false;
            if (question != null ? !question.equals(question1.question) : question1.question != null) return false;
            return !(answer != null ? !answer.equals(question1.answer) : question1.answer != null);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (question != null ? question.hashCode() : 0);
            result = 31 * result + (answer != null ? answer.hashCode() : 0);
            result = 31 * result + difficulty;
            return result;
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

    @Override

    public String toString() {
        return "Story{" +
                "headline='" + headline + '\'' +
                ", date='" + date + '\'' +
                ", id='" + id + '\'' +
                ", questions=" + questions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Story story = (Story) o;

        if (headline != null ? !headline.equals(story.headline) : story.headline != null) return false;
        if (date != null ? !date.equals(story.date) : story.date != null) return false;
        if (id != null ? !id.equals(story.id) : story.id != null) return false;
        if (text != null ? !text.equals(story.text) : story.text != null) return false;
        return !(questions != null ? !questions.equals(story.questions) : story.questions != null);

    }

    @Override
    public int hashCode() {
        int result = headline != null ? headline.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (questions != null ? questions.hashCode() : 0);
        return result;
    }
}
