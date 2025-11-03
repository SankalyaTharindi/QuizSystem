package src;

import java.io.Serializable;
import java.util.Arrays;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private String questionText;
    private String[] options;
    private int correctOption; // zero-based index

    public Question(String questionText, String[] options, int correctOption) {
        this.questionText = questionText;
        this.options = options;
        this.correctOption = correctOption;
    }

    public String getQuestionText() { return questionText; }
    public String[] getOptions() { return options; }
    public int getCorrectOption() { return correctOption; }

    @Override
    public String toString() {
        return "Question{" + questionText + ", options=" + Arrays.toString(options)
                + ", correct=" + correctOption + '}';
    }
}
