package models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Chapters {
    private String chapterId;
    private int chapterNumber;
    private String chapterKeyWord;
    private int lessonNumber;
    private String lessonTitle;
    private String lessonContent;
    private Timestamp lastEdit;

    public Chapters() {
    }

    public Chapters(String chapterId, int chapterNumber, String chapterKeyWord, int lessonNumber, String lessonTitle, String lessonContent, Timestamp lastEdit) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.chapterKeyWord = chapterKeyWord;
        this.lessonNumber = lessonNumber;
        this.lessonTitle = lessonTitle;
        this.lessonContent = lessonContent;
        this.lastEdit = lastEdit;
    }

    // Getters and setters
    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getChapterKeyWord() {
        return chapterKeyWord;
    }

    public void setChapterKeyWord(String chapterKeyWord) {
        this.chapterKeyWord = chapterKeyWord;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public void setLessonNumber(int lessonNumber) {
        this.lessonNumber = lessonNumber;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public String getLessonContent() {
        return lessonContent;
    }

    public void setLessonContent(String lessonContent) {
        this.lessonContent = lessonContent;
    }

    public Timestamp getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(Timestamp lastEdit) {
        this.lastEdit = lastEdit;
    }

    @Override
    public String toString() {
        return "Chapters{" +
                "chapterId='" + chapterId + '\'' +
                ", chapterNumber=" + chapterNumber +
                ", chapterKeyWord='" + chapterKeyWord + '\'' +
                ", lessonNumber=" + lessonNumber +
                ", lessonTitle='" + lessonTitle + '\'' +
                ", lessonContent='" + lessonContent + '\'' +
                ", lastEdit=" + lastEdit +
                '}';
    }
}
