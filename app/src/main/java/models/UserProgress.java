package models;

public class UserProgress {
    private String chapterId;
    private int chapterNumber;
    private int lessonNumber;
    private boolean progress;
    private String language;

    public UserProgress() { }

    public UserProgress(String chapterId, int chapterNumber, int lessonNumber, boolean progress, String language) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.lessonNumber = lessonNumber;
        this.progress = progress;
        this.language = language;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
