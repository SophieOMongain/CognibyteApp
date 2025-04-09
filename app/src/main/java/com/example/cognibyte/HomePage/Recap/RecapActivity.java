package com.example.cognibyte.HomePage.Recap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;

public class RecapActivity extends AppCompatActivity {

    private ImageView backArrow;
    private Button btnLessonRecap;
    private Button btnQuizRecap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recap);

        backArrow = findViewById(R.id.back_arrow);
        btnLessonRecap = findViewById(R.id.btn_lesson_recap);
        btnQuizRecap = findViewById(R.id.btn_quiz_recap);

        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnLessonRecap.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, LessonRecapActivity.class);
            startActivity(intent);
        });

        btnQuizRecap.setOnClickListener(v -> {
            Intent intent = new Intent(RecapActivity.this, ViewQuizRecapActivity.class);
            startActivity(intent);
        });
    }
}
