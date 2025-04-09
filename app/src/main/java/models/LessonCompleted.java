package models;

public class LessonCompleted {
    public int lessonNumber;
    public boolean isEnabled;

    public LessonCompleted(int lessonNumber, boolean isEnabled) {
        this.lessonNumber = lessonNumber;
        this.isEnabled = isEnabled;
    }
}
