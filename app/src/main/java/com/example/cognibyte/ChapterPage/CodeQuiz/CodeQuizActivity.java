package com.example.cognibyte.ChapterPage.CodeQuiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.CodeQuizAdapter;
import com.example.cognibyte.Client.JDoodleClient;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;
import java.util.ArrayList;
import java.util.List;

public class CodeQuizActivity extends AppCompatActivity {
    private EditText etCodeInput;
    private Spinner spinnerLanguage;
    private Button btnRunCode, btnBack, btnSubmit;
    private TextView tvOutput;
    private RecyclerView recyclerView;
    private CodeQuizAdapter quizAdapter;
    private List<String> quizQuestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_quiz);
        etCodeInput = findViewById(R.id.etCodeInput);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnRunCode = findViewById(R.id.btnRunCode);
        btnBack = findViewById(R.id.btnBack);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvOutput = findViewById(R.id.tvOutput);
        recyclerView = findViewById(R.id.recyclerViewQuestions);
        etCodeInput.setText("// Write your code here...\n");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        quizAdapter = new CodeQuizAdapter(quizQuestions);
        recyclerView.setAdapter(quizAdapter);
        int lessonNumber = getIntent().getIntExtra("lessonNumber", 1);
        fetchQuizQuestion(lessonNumber);
        btnRunCode.setOnClickListener(v -> executeCode());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(CodeQuizActivity.this, SelectCodeQuizActivity.class));
            finish();
        });
        btnSubmit.setOnClickListener(v ->
                Toast.makeText(CodeQuizActivity.this, "Quiz submitted!", Toast.LENGTH_SHORT).show()
        );
    }

    private void fetchQuizQuestion(int lessonNumber) {
        SharedPreferences prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        String language = prefs.getString("selected_language", "Java");
        // Updated call: OpenAIClient.generateCodingQuiz expects only language and lessonNumber
        OpenAIClient.generateCodingQuiz(language, String.valueOf(lessonNumber), new OpenAIClient.CompletionCallback() {
            @Override
            public void onSuccess(String codingChallenge) {
                runOnUiThread(() -> {
                    quizQuestions.clear();
                    quizQuestions.add(codingChallenge);
                    quizAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvOutput.setText("Error fetching quiz: " + error));
            }
        });
    }

    private void executeCode() {
        String code = etCodeInput.getText().toString();
        String selectedLanguage = spinnerLanguage.getSelectedItem().toString();
        if (code.isEmpty()) {
            tvOutput.setText("Please enter some code to execute.");
            return;
        }
        JDoodleClient.executeCode(code, getJDoodleLanguage(selectedLanguage), new JDoodleClient.JDoodleCallback() {
            @Override
            public void onSuccess(String output) {
                runOnUiThread(() -> tvOutput.setText("Output:\n" + output));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvOutput.setText("Error: " + error));
            }
        });
    }

    private String getJDoodleLanguage(String selectedLanguage) {
        switch (selectedLanguage) {
            case "Java":
                return "java";
            case "Python":
                return "python3";
            case "HTML":
                return "html";
            case "JavaScript":
                return "nodejs";
            default:
                return "java";
        }
    }
}
