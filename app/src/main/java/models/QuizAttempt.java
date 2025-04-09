package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class QuizAttempt implements Serializable {
    private List<Question> questionsAsked;
    private List<Question> wrongQuestions;
    private int score;

    public QuizAttempt() {}

    public QuizAttempt(List<Question> questionsAsked, List<Question> wrongQuestions, int score) {
        this.questionsAsked = questionsAsked;
        this.wrongQuestions = wrongQuestions;
        this.score = score;
    }

    public List<Question> getQuestionsAsked() {
        return questionsAsked;
    }

    public void setQuestionsAsked(List<Question> questionsAsked) {
        this.questionsAsked = questionsAsked;
    }

    public List<Question> getWrongQuestions() {
        return wrongQuestions;
    }

    public void setWrongQuestions(List<Question> wrongQuestions) {
        this.wrongQuestions = wrongQuestions;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
