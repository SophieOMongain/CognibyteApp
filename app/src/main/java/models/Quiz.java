package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class Quiz implements Serializable {
    private int chapterNumber;
    private int lessonNumber;
    private String language;
    private String skillLevel;
    private List<Question> questions;

    public Quiz() {}

    public Quiz(int chapterNumber, int lessonNumber, String language, String skillLevel, List<Question> questions) {
        this.chapterNumber = chapterNumber;
        this.lessonNumber = lessonNumber;
        this.language = language;
        this.skillLevel = skillLevel;
        this.questions = questions;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public void setLessonNumber(int lessonNumber) {
        this.lessonNumber = lessonNumber;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "chapterNumber=" + chapterNumber +
                ", lessonNumber=" + lessonNumber +
                ", language='" + language + '\'' +
                ", skillLevel='" + skillLevel + '\'' +
                ", questions=" + questions +
                '}';
    }
}
