package com.example.cognibyte.ChapterPage.CodeQuiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;
import java.util.ArrayList;
import java.util.List;

public class SelectCodeQuizActivity extends AppCompatActivity {

    private LinearLayout quizButtonContainer;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_code_quiz);

        quizButtonContainer = findViewById(R.id.quizButtonContainer);
        btnBack = findViewById(R.id.btnBack);

        addQuizButtons();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SelectCodeQuizActivity.this, /**WeeklyQuizActivity*/ HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void addQuizButtons() {
        List<String> lessons = new ArrayList<>();
        lessons.add("Quiz 1");
        lessons.add("Quiz 2");
        lessons.add("Quiz 3");
        lessons.add("Quiz 4");

        for (int i = 0; i < lessons.size(); i++) {
            String quizTitle = lessons.get(i);
            int lessonNumber = i + 1;

            Button quizButton = new Button(this);
            quizButton.setText(quizTitle);
            quizButton.setTextSize(18);
            quizButton.setAllCaps(false);
            quizButton.setBackgroundResource(R.drawable.rounded_button);
            quizButton.setPadding(16, 16, 16, 16);
            quizButton.setTextColor(getResources().getColor(R.color.primary_blue));

            quizButton.setOnClickListener(v -> startCodeQuiz(lessonNumber));

            quizButtonContainer.addView(quizButton);
        }
    }

    private void startCodeQuiz(int lessonNumber) {
        Intent intent = new Intent(SelectCodeQuizActivity.this, CodeQuizActivity.class);
        intent.putExtra("lessonNumber", lessonNumber);
        startActivity(intent);
    }
}
