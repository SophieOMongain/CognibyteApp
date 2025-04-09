package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.Map;

@IgnoreExtraProperties
public class UserQuiz implements Serializable {
    private String userId;
    private int chapterNumber;
    private Map<String, QuizData> quizzes;

    public UserQuiz() {}

    public UserQuiz(String userId, int chapterNumber, Map<String, QuizData> quizzes) {
        this.userId = userId;
        this.chapterNumber = chapterNumber;
        this.quizzes = quizzes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public Map<String, QuizData> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(Map<String, QuizData> quizzes) {
        this.quizzes = quizzes;
    }
}
