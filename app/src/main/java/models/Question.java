package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class Question implements Serializable {
    private String question;
    private List<String> options;
    private String answer;
    private String explanation;

    public Question() {}

    public Question(String question, List<String> options, String answer, String explanation) {
        this.question = question;
        this.options = options;
        this.answer = answer;
        this.explanation = explanation;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", options=" + options +
                ", answer='" + answer + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
