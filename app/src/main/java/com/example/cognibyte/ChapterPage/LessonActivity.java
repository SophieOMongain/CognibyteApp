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
import com.example.cognibyte.HomePage.ChapterActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.HomePage.ProfileActivity;
import com.example.cognibyte.HomePage.Recap.RecapActivity;
import com.example.cognibyte.HomePage.Stats.StatsActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.LessonCompleted;

public class LessonActivity extends AppCompatActivity {

    private static final String TAG = "LessonActivity";
    private ImageView btnBack, btnHome, btnProfile, btnStats, btnCodeQuiz;
    private TextView tvChapterTitle;
    private RecyclerView recyclerViewLessons;
    private LessonSelectionAdapter lessonAdapter;
    private final List<LessonCompleted> lessonItems = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    private String selectedLanguage, skillLevel;
    private int chapterNumber;

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
        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);
        btnStats = findViewById(R.id.btnStats);
        btnCodeQuiz = findViewById(R.id.btncodeQuiz);
        tvChapterTitle = findViewById(R.id.tvChapterTitle);
        recyclerViewLessons = findViewById(R.id.recyclerViewLessons);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(LessonActivity.this, ChapterActivity.class));
            finish();
        });
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        btnCodeQuiz.setOnClickListener(v -> startActivity(new Intent(this, RecapActivity.class)));

        chapterNumber = getIntent().getIntExtra("chapterNumber", 1);
        tvChapterTitle.setText("CHAPTER " + chapterNumber);

        String intentLang = getIntent().getStringExtra("language");
        String intentLevel = getIntent().getStringExtra("skillLevel");
        if (intentLang != null && intentLevel != null) {
            selectedLanguage = intentLang;
            skillLevel = intentLevel;
            initLessonList();
        } else {
            loadUserSelectedLanguageAndSkillLevel();
        }
    }

    private void loadUserSelectedLanguageAndSkillLevel() {
        firestore.collection("Languages")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String lang = doc.getString("language");
                    String skill = doc.getString("skillLevel");

                    List<String> langsList = new ArrayList<>();
                    Object obj = doc.get("languages");
                    if (obj instanceof List<?>) {
                        for (Object o : (List<?>) obj) {
                            if (o instanceof String) langsList.add((String) o);
                        }
                    }

                    if (lang == null && !langsList.isEmpty()) {
                        lang = langsList.get(0);
                    }

                    if (lang == null || skill == null) {
                        Toast.makeText(this, "Select a language & level first.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedLanguage = lang;
                    skillLevel = skill;
                    initLessonList();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading language prefs", e);
                    Toast.makeText(this, "Error loading settings.", Toast.LENGTH_SHORT).show();
                });
    }

    private void initLessonList() {
        lessonAdapter = new LessonSelectionAdapter(lessonItems, this::startLesson);
        recyclerViewLessons.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLessons.setAdapter(lessonAdapter);
        loadLessons();
    }

    private void loadLessons() {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(this::onLessonsFetched)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch lessons", e);
                    Toast.makeText(this, "Could not load lessons.", Toast.LENGTH_SHORT).show();
                });
    }

    private void onLessonsFetched(QuerySnapshot snaps) {
        lessonItems.clear();
        List<DocumentSnapshot> docs = new ArrayList<>(snaps.getDocuments());
        Collections.sort(docs, Comparator.comparingInt(d -> d.getLong("lessonNumber").intValue()));
        for (int i = 0; i < docs.size(); i++) {
            String title = docs.get(i).getString("lessonTitle");
            boolean enabled = (i == 0);
            lessonItems.add(new LessonCompleted(title, enabled));
        }
        lessonAdapter.notifyDataSetChanged();
        updateLessonButtons();
    }

    private void updateLessonButtons() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Boolean> done = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String t = d.getString("lessonTitle");
                        Boolean p = d.getBoolean("progress");
                        if (t != null && p != null) done.put(t, p);
                    }
                    for (int i = 1; i < lessonItems.size(); i++) {
                        String prev = lessonItems.get(i - 1).lessonTitle;
                        lessonItems.get(i).isEnabled = done.getOrDefault(prev, false);
                    }
                    lessonAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Progress load failed", e));
    }

    private void startLesson(String lessonTitle) {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Boolean> done = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String t = d.getString("lessonTitle");
                        Boolean p = d.getBoolean("progress");
                        if (t != null && p != null) done.put(t, p);
                    }
                    int idx = -1;
                    for (int i = 0; i < lessonItems.size(); i++) {
                        if (lessonItems.get(i).lessonTitle.equals(lessonTitle)) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx > 0) {
                        String prev = lessonItems.get(idx - 1).lessonTitle;
                        if (!done.getOrDefault(prev, false)) {
                            Toast.makeText(this, "Complete " + prev + " first!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Intent i = new Intent(this, LessonPageActivity.class);
                    i.putExtra("chapterNumber", chapterNumber);
                    i.putExtra("language", selectedLanguage);
                    i.putExtra("skillLevel", skillLevel);
                    i.putExtra("lessonTitle", lessonTitle);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Progress check failed", e);
                    Toast.makeText(this, "Error checking progress.", Toast.LENGTH_SHORT).show();
                });
    }
}
