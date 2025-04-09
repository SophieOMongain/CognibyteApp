package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class GenerateQuizActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerSkillLevel;
    private EditText etChapterNumber, etLessonNumber;
    private Button btnGenerateContent, btnViewContent;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "Select Language";
    private String selectedSkillLevel = "Select Skill Level";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_quiz);
        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);
        firestore = FirebaseFirestore.getInstance();
        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedLanguage = languages[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        String[] skillLevels = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, skillLevels);
        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedSkillLevel = skillLevels[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        btnBack.setOnClickListener(v -> finish());
        btnGenerateContent.setOnClickListener(v -> {
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            if(chapterStr.isEmpty() || lessonStr.isEmpty()){
                Toast.makeText(GenerateQuizActivity.this, "Please enter both chapter and lesson numbers", Toast.LENGTH_SHORT).show();
                return;
            }
            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
            } catch (NumberFormatException e) {
                Toast.makeText(GenerateQuizActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return;
            }
            if(chapterNumber < 1 || chapterNumber > 10 || lessonNumber < 1 || lessonNumber > 10){
                Toast.makeText(GenerateQuizActivity.this, "Chapter and Lesson numbers must be between 1 and 10", Toast.LENGTH_SHORT).show();
                return;
            }
            btnGenerateContent.setEnabled(false);
            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", chapterNumber)
                    .whereEqualTo("lessonNumber", lessonNumber)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if(!querySnapshot.isEmpty()){
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            String lessonContent = document.getString("lessonContent");
                            String chapterId = document.getString("chapterId");
                            String lessonTitle = document.getString("lessonTitle");
                            if(lessonContent != null && !lessonContent.isEmpty() && lessonTitle != null && !lessonTitle.isEmpty()){
                                requestQuizGeneration(selectedLanguage, selectedSkillLevel, lessonContent, chapterId, lessonTitle, chapterNumber, lessonNumber);
                            } else {
                                Toast.makeText(GenerateQuizActivity.this, "Lesson content or title is empty", Toast.LENGTH_SHORT).show();
                                btnGenerateContent.setEnabled(true);
                            }
                        } else {
                            Toast.makeText(GenerateQuizActivity.this, "Lesson content not found", Toast.LENGTH_SHORT).show();
                            btnGenerateContent.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GenerateQuizActivity.this, "Error fetching lesson: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnGenerateContent.setEnabled(true);
                    });
        });
        btnViewContent.setOnClickListener(v -> {
            Intent intent = new Intent(GenerateQuizActivity.this, ViewQuizContentActivity.class);
            startActivity(intent);
        });
    }

    private void requestQuizGeneration(String language, String skillLevel, String lessonContent, String chapterId, String lessonTitle, int chapterNumber, int lessonNumber) {
        OpenAIClient.generateQuiz(chapterNumber, lessonNumber, language, lessonContent, new OpenAIClient.QuizCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> questions) {
                OpenAIClient.saveQuizToFirestore(chapterNumber, lessonNumber, language, lessonTitle, skillLevel, chapterId, questions);
                runOnUiThread(() -> {
                    Toast.makeText(GenerateQuizActivity.this, "Quiz generated and saved successfully", Toast.LENGTH_LONG).show();
                    btnGenerateContent.setEnabled(true);
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(GenerateQuizActivity.this, "Quiz generation error: " + error, Toast.LENGTH_LONG).show();
                    btnGenerateContent.setEnabled(true);
                });
            }
        });
    }
}
