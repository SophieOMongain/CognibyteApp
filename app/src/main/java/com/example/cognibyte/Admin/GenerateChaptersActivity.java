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
    private EditText etChapterTitle, etChapterNumber;
    private EditText etLessonTitle, etLessonNumber;
    private EditText etDescription;
    private Button btnGenerateContent, btnViewContent;
    private String selectedLanguage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_chapters);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etChapterTitle = findViewById(R.id.etChapterTitle);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonTitle = findViewById(R.id.etLessonTitle);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        etDescription = findViewById(R.id.etDescription);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLanguage = languages[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnBack.setOnClickListener(v -> finish());

        btnGenerateContent.setOnClickListener(v -> {
            String chapterTitle = etChapterTitle.getText().toString().trim();
            String lesssonTitle = etLessonTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String chapterNumber = etChapterNumber.getText().toString().trim();
            String lessonNumber = etLessonNumber.getText().toString().trim();

            if (chapterTitle.isEmpty() || lesssonTitle.isEmpty() || description.isEmpty()
                    || chapterNumber.isEmpty() || lessonNumber.isEmpty()) {
                Toast.makeText(this,
                        "Fill in chapter #, lesson #, titles & description",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int chapterNum, lesNum;
            try {
                chapterNum = Integer.parseInt(chapterNumber);
                lesNum = Integer.parseInt(lessonNumber);
            } catch (NumberFormatException e) {
                Toast.makeText(this,
                        "Chapter/Lesson numbers must be integers",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (chapterNum < 1 || chapterNum > 5 || lesNum < 1 || lesNum > 5) {
                Toast.makeText(this,
                        "Numbers must be between 1 and 5",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerateContent.setEnabled(false);

            OpenAIClient.generateLesson(chapterNum, lesNum, chapterTitle, lesssonTitle, description, selectedLanguage,
                    new OpenAIClient.CompletionCallback() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> {
                                Toast.makeText(GenerateChaptersActivity.this,
                                        "Content generated and saved!", Toast.LENGTH_LONG).show();
                                btnGenerateContent.setEnabled(true);
                            });
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(GenerateChaptersActivity.this,
                                        "Error: " + error, Toast.LENGTH_LONG).show();
                                btnGenerateContent.setEnabled(true);
                            });
                        }
                    }
            );
        });

        btnViewContent.setOnClickListener(v ->
                startActivity(new Intent(this, ViewChapterContentActivity.class))
        );
    }
}
