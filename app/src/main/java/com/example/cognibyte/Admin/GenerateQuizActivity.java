package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateQuizActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerSkillLevel, spinnerChapter, spinnerLesson;
    private Button btnGenerateContent, btnViewContent;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "";
    private String selectedSkillLevel = "";
    private String selectedChapter = "";
    private String selectedLesson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_quiz);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);
        firestore = FirebaseFirestore.getInstance();

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                languages
        );
        spinnerLanguage.setAdapter(langAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLanguage = languages[pos];
                loadChapterTitles();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] skillLevels = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                skillLevels
        );
        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedSkillLevel = skillLevels[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedChapter = (String) p.getItemAtPosition(pos);
                loadLessonTitles(selectedChapter);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLesson = (String) p.getItemAtPosition(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnBack.setOnClickListener(v -> finish());
        btnGenerateContent.setOnClickListener(v -> {
            if (selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
                Toast.makeText(this,
                        "Please select a chapter and lesson",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            btnGenerateContent.setEnabled(false);

            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterTitle", selectedChapter)
                    .whereEqualTo("lessonTitle", selectedLesson)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (qs.isEmpty()) {
                            Toast.makeText(this, "Lesson not found", Toast.LENGTH_SHORT).show();
                            btnGenerateContent.setEnabled(true);
                            return;
                        }
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        String lessonContent = doc.getString("lessonContent");
                        String chapterId = doc.getString("chapterId");
                        String lessonTitle = doc.getString("lessonTitle");
                        Long chapNum = doc.getLong("chapterNumber");
                        Long lesNum = doc.getLong("lessonNumber");
                        int chapterNumber = chapNum != null ? chapNum.intValue() : 0;
                        int lessonNumber = lesNum != null ? lesNum.intValue() : 0;

                        if (lessonContent == null || lessonContent.isEmpty()) {
                            Toast.makeText(this, "Lesson content is empty", Toast.LENGTH_SHORT).show();
                            btnGenerateContent.setEnabled(true);
                            return;
                        }

                        requestQuizGeneration(chapterNumber, lessonNumber, selectedLanguage, selectedSkillLevel, lessonContent, chapterId, lessonTitle);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Error fetching lesson: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnGenerateContent.setEnabled(true);
                    });
        });

        btnViewContent.setOnClickListener(v ->
                startActivity(new Intent(this, ViewQuizContentActivity.class))
        );
    }

    private void loadChapterTitles() {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> chapters = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String t = d.getString("chapterTitle");
                        if (t != null && !chapters.contains(t)) chapters.add(t);
                    }
                    spinnerChapter.setAdapter(new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            chapters
                    ));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading chapters: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void loadLessonTitles(String chapterTitle) {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterTitle", chapterTitle)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> lessons = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String t = d.getString("lessonTitle");
                        if (t != null && !lessons.contains(t)) lessons.add(t);
                    }
                    spinnerLesson.setAdapter(new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            lessons
                    ));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading lessons: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void requestQuizGeneration(int chapterNumber, int lessonNumber, String language, String skillLevel, String lessonContent, String chapterId, String lessonTitle) {
        OpenAIClient.generateQuiz(chapterNumber, lessonNumber, language, skillLevel, lessonContent, new OpenAIClient.QuizCallback() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> questions) {
                        OpenAIClient.saveQuizToFirestore(chapterNumber, lessonNumber, language, selectedChapter, lessonTitle, skillLevel, chapterId, questions);
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    GenerateQuizActivity.this,
                                    "Quiz generated and saved successfully",
                                    Toast.LENGTH_LONG
                            ).show();
                            btnGenerateContent.setEnabled(true);
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    GenerateQuizActivity.this,
                                    "Quiz generation error: " + error,
                                    Toast.LENGTH_LONG
                            ).show();
                            btnGenerateContent.setEnabled(true);
                        });
                    }
                }
        );
    }
}
