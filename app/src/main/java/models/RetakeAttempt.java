package models;

import java.io.Serializable;

public class RetakeAttempt implements Serializable {

    private int score;
    private int totalQuestions;
    private String timeTaken;
    private long timeMillis;
    private String date;
    private String language;
    private String chapter;
    private String lesson;
    private String userId;

    public RetakeAttempt() {
    }

    public RetakeAttempt(int score, int totalQuestions, String timeTaken, long timeMillis, String date, String language, String chapter, String lesson) {
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timeTaken = timeTaken;
        this.timeMillis = timeMillis;
        this.date = date;
        this.language = language;
        this.chapter = chapter;
        this.lesson = lesson;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getLesson() {
        return lesson;
    }

    public void setLesson(String lesson) {
        this.lesson = lesson;
    }
}
