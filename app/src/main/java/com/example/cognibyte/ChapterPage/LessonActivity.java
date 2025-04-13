package com.example.cognibyte.ChapterPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.LessonSelectionAdapter;
import com.example.cognibyte.ChapterPage.CodeQuiz.WeeklyQuizActivity;
import com.example.cognibyte.HomePage.ChapterActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.HomePage.ProfileActivity;
import com.example.cognibyte.HomePage.SettingsActivity;
import com.example.cognibyte.HomePage.StatsActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.LessonCompleted;

public class LessonActivity extends AppCompatActivity {

    private ImageView btnBack, btnHome, btnProfile, btnStats, btnCodeQuiz;
    private TextView tvChapterTitle;
    private RecyclerView recyclerViewLessons;
    private LessonSelectionAdapter lessonAdapter;
    private List<LessonCompleted> lessonItems;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    private String selectedLanguage;
    private String skillLevel;
    private int chapterNumber;
    private static final String TAG = "LessonActivity";
    private static final int TOTAL_LESSONS = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();
        btnBack = findViewById(R.id.btnBack);
        tvChapterTitle = findViewById(R.id.tvChapterTitle);
        recyclerViewLessons = findViewById(R.id.recyclerViewLessons);
        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);
        btnStats = findViewById(R.id.btnStats);
        btnCodeQuiz = findViewById(R.id.btncodeQuiz);
        chapterNumber = getIntent().getIntExtra("chapterNumber", 1);
        tvChapterTitle.setText("CHAPTER " + chapterNumber);
        btnBack.setOnClickListener(v -> finish());
        recyclerViewLessons.setLayoutManager(new LinearLayoutManager(this));
        lessonItems = new ArrayList<>();
        for (int i = 1; i <= TOTAL_LESSONS; i++) {
            boolean enabled = (i == 1);
            lessonItems.add(new LessonCompleted(i, enabled));
        }
        lessonAdapter = new LessonSelectionAdapter(lessonItems, this::startLesson);
        recyclerViewLessons.setAdapter(lessonAdapter);
        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        btnStats.setOnClickListener(v -> navigateTo(StatsActivity.class));
        btnCodeQuiz.setOnClickListener(v -> navigateTo(WeeklyQuizActivity.class));
        loadUserSelectedLanguageAndSkillLevel();
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
                        updateLessonButtons();
                    } else {
                        Toast.makeText(this, "Language and skill level not found. Please select a language.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading language data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching language selection: " + e.getMessage());
                });
    }

    private void updateLessonButtons() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<Integer, Boolean> lessonProgress = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long lessonNum = doc.getLong("lessonNumber");
                        if (lessonNum != null) {
                            Boolean completed = doc.getBoolean("progress");
                            lessonProgress.put(lessonNum.intValue(), completed != null && completed);
                        }
                    }
                    for (int i = 1; i <= TOTAL_LESSONS; i++) {
                        boolean isEnabled = (i == 1) || lessonProgress.getOrDefault(i - 1, false);
                        lessonItems.get(i - 1).isEnabled = isEnabled;
                    }
                    lessonAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load lesson progress: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading progress: " + e.getMessage());
                });
    }

    private void startLesson(int lessonNumber) {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<Integer, Boolean> lessonProgress = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long lessonNum = doc.getLong("lessonNumber");
                        if (lessonNum != null) {
                            Boolean completed = doc.getBoolean("progress");
                            lessonProgress.put(lessonNum.intValue(), completed != null && completed);
                        }
                    }
                    if (lessonNumber > 1) {
                        Boolean prevCompleted = lessonProgress.get(lessonNumber - 1);
                        if (prevCompleted == null || !prevCompleted) {
                            Toast.makeText(this, "Complete Lesson " + (lessonNumber - 1) + " first!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else if (lessonNumber == 1 && chapterNumber > 1) {
                    }
                    proceedToLesson(lessonNumber);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check lesson progress.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching lesson progress: " + e.getMessage());
                });
    }

    private void proceedToLesson(int lessonNumber) {
        Intent intent = new Intent(LessonActivity.this, LessonPageActivity.class);
        intent.putExtra("lessonNumber", lessonNumber);
        intent.putExtra("chapterNumber", chapterNumber);
        intent.putExtra("language", selectedLanguage);
        intent.putExtra("skillLevel", skillLevel);
        startActivity(intent);
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(LessonActivity.this, activityClass);
        startActivity(intent);
    }
}
