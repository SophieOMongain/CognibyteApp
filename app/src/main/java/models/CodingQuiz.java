package models;

import java.util.List;

public class CodingQuiz {
    private String question;
    private String sampleSolution;
    private String explanation;
    private List<String> testCases;

    public CodingQuiz() {}

    public CodingQuiz(String question, String sampleSolution, String explanation, List<String> testCases) {
        this.question = question;
        this.sampleSolution = sampleSolution;
        this.explanation = explanation;
        this.testCases = testCases;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSampleSolution() {
        return sampleSolution;
    }

    public void setSampleSolution(String sampleSolution) {
        this.sampleSolution = sampleSolution;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<String> testCases) {
        this.testCases = testCases;
    }
}
