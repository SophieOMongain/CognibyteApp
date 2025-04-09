package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.ChapterPage.CodeQuiz.WeeklyQuizActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChapterActivity extends AppCompatActivity {
    private ImageView imgLanguageLogo;
    private TextView tvLanguageName;
    private Button btnChapter1, btnChapter2, btnChapter3, btnChapter4;
    private ImageView btnHome, btnProfile, btnStats, btnCodeQuiz;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    private String selectedLanguage;
    private String skillLevel;

    private static final String TAG = "ChapterActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        imgLanguageLogo = findViewById(R.id.imgLanguageLogo);
        tvLanguageName = findViewById(R.id.tvLanguageName);
        btnChapter1 = findViewById(R.id.btnChapter1);
        btnChapter2 = findViewById(R.id.btnChapter2);
        btnChapter3 = findViewById(R.id.btnChapter3);
        btnChapter4 = findViewById(R.id.btnChapter4);
        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);
        btnStats = findViewById(R.id.btnStats);
        btnCodeQuiz = findViewById(R.id.btncodeQuiz);

        sharedPreferences = getSharedPreferences("LessonProgress", MODE_PRIVATE);

        loadUserSelectedLanguageAndSkillLevel();

        btnChapter1.setOnClickListener(v -> startChapter(1));
        btnChapter2.setOnClickListener(v -> startChapter(2));
        btnChapter3.setOnClickListener(v -> startChapter(3));
        btnChapter4.setOnClickListener(v -> startChapter(4));

        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        btnStats.setOnClickListener(v -> navigateTo(StatsActivity.class));
        btnCodeQuiz.setOnClickListener(v -> navigateTo(WeeklyQuizActivity.class));
    }

    private void loadUserSelectedLanguageAndSkillLevel() {
        firestore.collection("Languages").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                            documentSnapshot.contains("language") &&
                            documentSnapshot.contains("skillLevel")) {
                        selectedLanguage = documentSnapshot.getString("language");
                        skillLevel = documentSnapshot.getString("skillLevel");

                        tvLanguageName.setText(selectedLanguage);
                        imgLanguageLogo.setImageResource(getLanguageIcon(selectedLanguage));

                        updateChapterButtons();
                    } else {
                        Toast.makeText(this, "Language and skill level not found. Please select a language.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading language data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching language selection: " + e.getMessage());
                });
    }

    private int getLanguageIcon(String language) {
        Map<String, Integer> languageIcons = new HashMap<>();
        languageIcons.put("JavaScript", R.drawable.javascript_icon);
        languageIcons.put("Python", R.drawable.python_icon);
        languageIcons.put("Java", R.drawable.java_icon);
        languageIcons.put("C++", R.drawable.cpp_icon);
        languageIcons.put("HTML", R.drawable.html_icon);
        return languageIcons.getOrDefault(language, R.drawable.home);
    }

    private void updateChapterButtons() {
        firestore.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean chapter1Completed = documentSnapshot.getBoolean("Chapter1") != null &&
                            documentSnapshot.getBoolean("Chapter1");
                    boolean chapter2Completed = documentSnapshot.getBoolean("Chapter2") != null &&
                            documentSnapshot.getBoolean("Chapter2");
                    boolean chapter3Completed = documentSnapshot.getBoolean("Chapter3") != null &&
                            documentSnapshot.getBoolean("Chapter3");

                    btnChapter1.setEnabled(true);
                    updateButtonState(btnChapter2, chapter1Completed);
                    updateButtonState(btnChapter3, chapter2Completed);
                    updateButtonState(btnChapter4, chapter3Completed);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load chapter progress: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading progress: " + e.getMessage());
                });
    }

    private void updateButtonState(Button button, boolean isEnabled) {
        button.setEnabled(isEnabled);
        button.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private void startChapter(int chapterNumber) {
        firestore.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (chapterNumber > 1) {
                        Boolean prevChapterCompleted = documentSnapshot.getBoolean("Chapter" + (chapterNumber - 1));
                        if (prevChapterCompleted == null || !prevChapterCompleted) {
                            Toast.makeText(this, "Complete Chapter " + (chapterNumber - 1) + " first!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Intent intent = new Intent(ChapterActivity.this, com.example.cognibyte.ChapterPage.LessonActivity.class);
                    intent.putExtra("chapterNumber", chapterNumber);
                    intent.putExtra("language", selectedLanguage);
                    intent.putExtra("skillLevel", skillLevel);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check chapter progress.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching chapter progress: " + e.getMessage());
                });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(ChapterActivity.this, activityClass);
        startActivity(intent);
    }
}
