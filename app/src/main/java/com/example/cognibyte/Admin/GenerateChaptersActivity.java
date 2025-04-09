package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;

public class GenerateChaptersActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage;
    private EditText etChapterNumber, etLessonNumber;
    private Button btnGenerateContent, btnViewContent;

    private String selectedLanguage = "Select Language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_chapters);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = languages[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnGenerateContent.setOnClickListener(v -> {
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            if (chapterStr.isEmpty() || lessonStr.isEmpty()) {
                Toast.makeText(GenerateChaptersActivity.this, "Please enter both chapter and lesson numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
            } catch (NumberFormatException e) {
                Toast.makeText(GenerateChaptersActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chapterNumber < 1 || chapterNumber > 10 || lessonNumber < 1 || lessonNumber > 10) {
                Toast.makeText(GenerateChaptersActivity.this, "Chapter and Lesson numbers must be between 1 and 10", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerateContent.setEnabled(false);

            OpenAIClient.generateLesson(chapterNumber, lessonNumber, selectedLanguage, new OpenAIClient.CompletionCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        Toast.makeText(GenerateChaptersActivity.this, "Content generated and saved successfully", Toast.LENGTH_LONG).show();
                        btnGenerateContent.setEnabled(true);
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(GenerateChaptersActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        btnGenerateContent.setEnabled(true);
                    });
                }
            });
        });

        btnViewContent.setOnClickListener(v -> {
            Intent intent = new Intent(GenerateChaptersActivity.this, ViewChapterContentActivity.class);
            startActivity(intent);
        });
    }
}
