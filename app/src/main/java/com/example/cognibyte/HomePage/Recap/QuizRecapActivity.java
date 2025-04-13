package com.example.cognibyte.HomePage.Recap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.example.cognibyte.HomePage.Recap.RecapActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizRecapActivity extends AppCompatActivity {
    private static final String TAG = "QuizRecapActivity";
    private ImageView backArrow;
    private Spinner spinnerChapter, spinnerLesson, spinnerLanguage, spinnerSkillLevel;
    private Button btnViewQuizRecap;
    private TextView tvQuizRecap;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    private Map<Integer, Integer> progressMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_recap);
        backArrow = findViewById(R.id.back_arrow);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        btnViewQuizRecap = findViewById(R.id.btnViewQuizRecap);
        tvQuizRecap = findViewById(R.id.tvQuizRecap);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();
        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(QuizRecapActivity.this, RecapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        loadUserLanguages();
        List<String> skillLevels = new ArrayList<>();
        skillLevels.add("Beginner");
        skillLevels.add("Intermediate");
        skillLevels.add("Expert");
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, skillLevels);
        skillAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkillLevel.setAdapter(skillAdapter);
        btnViewQuizRecap.setOnClickListener(v -> {
            Integer selectedChapter = (Integer) spinnerChapter.getSelectedItem();
            Integer selectedLesson = (Integer) spinnerLesson.getSelectedItem();
            String selectedLanguage = (String) spinnerLanguage.getSelectedItem();
            String selectedSkill = (String) spinnerSkillLevel.getSelectedItem();
            if (selectedChapter == null || selectedLesson == null || selectedLanguage == null || selectedSkill == null) {
                Toast.makeText(QuizRecapActivity.this, "Please select chapter, quiz, language, and skill level.", Toast.LENGTH_SHORT).show();
                return;
            }
            firestore.collection("QuizContent")
                    .whereEqualTo("chapterNumber", selectedChapter)
                    .whereEqualTo("lessonNumber", selectedLesson)
                    .whereEqualTo("language", selectedLanguage)
                    .whereEqualTo("skillLevel", selectedSkill)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String quizQuestions = doc.getString("quizQuestions");
                            if (quizQuestions != null && !quizQuestions.isEmpty()) {
                                tvQuizRecap.setText(quizQuestions);
                            } else {
                                tvQuizRecap.setText("No quiz questions available.");
                            }
                        } else {
                            tvQuizRecap.setText("No quiz found for the selected options.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvQuizRecap.setText("Error loading quiz: " + e.getMessage());
                        Log.e(TAG, "Error loading quiz", e);
                    });
        });
        loadUserProgress();
    }

    private void loadUserLanguages() {
        firestore.collection("Language")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> languages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String lang = doc.getString("language");
                        if (lang != null && !languages.contains(lang)) {
                            languages.add(lang);
                        }
                    }
                    if (languages.isEmpty()) {
                        languages.add("Java");
                    }
                    Collections.sort(languages);
                    ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
                    langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguage.setAdapter(langAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(QuizRecapActivity.this, "Error loading languages: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadUserProgress() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressMap.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long chapNumLong = doc.getLong("chapterNumber");
                        Long lessonNumLong = doc.getLong("lessonNumber");
                        if (chapNumLong != null && lessonNumLong != null) {
                            int chapNum = chapNumLong.intValue();
                            int lessonNum = lessonNumLong.intValue();
                            if (progressMap.containsKey(chapNum)) {
                                int currentMax = progressMap.get(chapNum);
                                if (lessonNum > currentMax) {
                                    progressMap.put(chapNum, lessonNum);
                                }
                            } else {
                                progressMap.put(chapNum, lessonNum);
                            }
                        }
                    }
                    if (progressMap.isEmpty()) {
                        Toast.makeText(QuizRecapActivity.this, "No progress found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    populateChapterSpinner();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(QuizRecapActivity.this, "Error loading progress: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading progress", e);
                });
    }

    private void populateChapterSpinner() {
        List<Integer> chapters = new ArrayList<>(progressMap.keySet());
        Collections.sort(chapters);
        ArrayAdapter<Integer> chapterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chapters);
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChapter.setAdapter(chapterAdapter);
        spinnerChapter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Integer selectedChapter = (Integer) parent.getItemAtPosition(position);
                populateLessonSpinner(selectedChapter);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        if (!chapters.isEmpty()) {
            spinnerChapter.setSelection(0);
            populateLessonSpinner(chapters.get(0));
        }
    }

    private void populateLessonSpinner(int chapterNumber) {
        int maxLesson = progressMap.get(chapterNumber);
        List<Integer> lessons = new ArrayList<>();
        for (int i = 1; i <= maxLesson; i++) {
            lessons.add(i);
        }
        ArrayAdapter<Integer> lessonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lessons);
        lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLesson.setAdapter(lessonAdapter);
    }
}
