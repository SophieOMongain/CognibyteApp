package models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Chapters {
    private String chapterId;
    private int chapterNumber;
    private String chapterTitle;
    private String chapterKeyWord;
    private int lessonNumber;
    private String lessonTitle;
    private String description;
    private String lessonContent;
    private String lessonRecap;
    private Timestamp lastEdit;

    public Chapters() {}

    public Chapters(String chapterId, int chapterNumber, String chapterTitle, String chapterKeyWord, int lessonNumber, String lessonTitle, String description, String lessonContent, String lessonRecap, Timestamp lastEdit) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.chapterKeyWord = chapterKeyWord;
        this.lessonNumber = lessonNumber;
        this.lessonTitle = lessonTitle;
        this.description = description;
        this.lessonContent = lessonContent;
        this.lessonRecap = lessonRecap;
        this.lastEdit = lastEdit;
    }

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

    public String getChapterTitle() {
        return chapterTitle;
    }
    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
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

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getLessonContent() {
        return lessonContent;
    }
    public void setLessonContent(String lessonContent) {
        this.lessonContent = lessonContent;
    }

    public String getLessonRecap() {
        return lessonRecap;
    }
    public void setLessonRecap(String lessonRecap) {
        this.lessonRecap = lessonRecap;
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
                ", chapterTitle='" + chapterTitle + '\'' +
                ", chapterKeyWord='" + chapterKeyWord + '\'' +
                ", lessonNumber=" + lessonNumber +
                ", lessonTitle='" + lessonTitle + '\'' +
                ", description='" + description + '\'' +
                ", lessonContent='" + lessonContent + '\'' +
                ", lessonRecap='" + lessonRecap + '\'' +
                ", lastEdit=" + lastEdit +
                '}';
    }
}
