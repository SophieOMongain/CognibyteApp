package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class QuizData implements Serializable {
    private List<QuizAttempt> attempts;

    public QuizData() {}

    public QuizData(List<QuizAttempt> attempts) {
        this.attempts = attempts;
    }

    public List<QuizAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<QuizAttempt> attempts) {
        this.attempts = attempts;
    }
}
