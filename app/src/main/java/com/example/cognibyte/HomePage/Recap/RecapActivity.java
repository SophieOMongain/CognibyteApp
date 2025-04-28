package com.example.cognibyte.HomePage.Recap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;

public class RecapActivity extends AppCompatActivity {

    private ImageView backArrow;
    private TextView btnLessonRecap;
    private TextView btnQuizRecap;
    private ImageView arrowLesson;
    private ImageView arrowQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recap);

        backArrow = findViewById(R.id.back_arrow);
        btnLessonRecap = findViewById(R.id.btn_lesson_recap);
        btnQuizRecap = findViewById(R.id.btn_quiz_recap);
        arrowLesson = findViewById(R.id.arrow_lesson);
        arrowQuiz = findViewById(R.id.arrow_quiz);

        backArrow.setOnClickListener(v -> finish());

        btnLessonRecap.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, LessonRecapActivity.class);
            startActivity(intent);
        });

        arrowLesson.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, LessonRecapActivity.class);
            startActivity(intent);
        });

        btnQuizRecap.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, QuizRecapActivity.class);
            startActivity(intent);
        });

        arrowQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, QuizRecapActivity.class);
            startActivity(intent);
        });
    }
}
