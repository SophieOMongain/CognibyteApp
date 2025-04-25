package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class UserQuiz implements Serializable {
    private String userId;
    private String language;
    private int chapterNumber;
    private String chapterTitle;
    private int lessonNumber;
    private String lessonTitle;

    public UserQuiz() { }

    public UserQuiz(String userId, String language, int chapterNumber, String chapterTitle, int lessonNumber, String lessonTitle) {
        this.userId = userId;
        this.language = language;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.lessonNumber = lessonNumber;
        this.lessonTitle = lessonTitle;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }

    public int getLessonNumber() { return lessonNumber; }
    public void setLessonNumber(int lessonNumber) { this.lessonNumber = lessonNumber; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    @Override
    public String toString() {
        return "UserQuiz{" +
                "userId='" + userId + '\'' +
                ", language='" + language + '\'' +
                ", chapterNumber=" + chapterNumber +
                ", chapterTitle='" + chapterTitle + '\'' +
                ", lessonNumber=" + lessonNumber +
                ", lessonTitle='" + lessonTitle + '\'' +
                '}';
    }
}
