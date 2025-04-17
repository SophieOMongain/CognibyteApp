package models;

public class LessonCompleted {
    public String lessonTitle;
    public boolean isEnabled;

    public LessonCompleted(String lessonTitle, boolean isEnabled) {
        this.lessonTitle = lessonTitle;
        this.isEnabled = isEnabled;
    }
}
