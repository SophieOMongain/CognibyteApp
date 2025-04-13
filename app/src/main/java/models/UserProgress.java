package models;

public class UserProgress {
    private String chapterId;
    private int chapterNumber;
    private int lessonNumber;
    private boolean progress;

    public UserProgress() { }

    public UserProgress(String chapterId, int chapterNumber, int lessonNumber, boolean progress) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.lessonNumber = lessonNumber;
        this.progress = progress;
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

    public int getLessonNumber() {
        return lessonNumber;
    }

    public void setLessonNumber(int lessonNumber) {
        this.lessonNumber = lessonNumber;
    }

    public boolean isProgress() {
        return progress;
    }

    public void setProgress(boolean progress) {
        this.progress = progress;
    }
}
