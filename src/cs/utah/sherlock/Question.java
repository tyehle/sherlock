package cs.utah.sherlock;

/**
 * @author Tobin Yehle
 */
public class Question {
    private String id, question;
    private int difficulty;

    public Question(String id, String question, int difficulty) {
        this.id = id;
        this.question = question;
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "Question(id="+id+", question="+question+", difficulty="+difficulty+")";
    }
}
