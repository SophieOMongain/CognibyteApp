package com.example.cognibyte.HomePage.Stats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;

public class StatsPageActivity extends AppCompatActivity {

    private Button btnViewStats;
    private Button btnViewQuizStats;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_page);

        btnViewStats = findViewById(R.id.btnViewStats);
        btnViewQuizStats = findViewById(R.id.btnViewQuizStats);
        btnBack = findViewById(R.id.btnBack);

        btnViewStats.setOnClickListener(v -> {
            Intent intent = new Intent(StatsPageActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        btnViewQuizStats.setOnClickListener(v -> {
            Intent intent = new Intent(StatsPageActivity.this, QuizStatsActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
