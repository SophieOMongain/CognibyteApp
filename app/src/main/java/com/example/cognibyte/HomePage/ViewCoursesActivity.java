package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public class ViewCoursesActivity extends AppCompatActivity {

    private Spinner spinnerLanguage, spinnerChapter;
    private LinearLayout lessonList;
    private FirebaseFirestore firestore;
    private String userId;
    private String selectedLanguage = "";
    private VideoView viewCoursesVideo;
    private final Map<Integer, List<String>> completedLessonsMap = new HashMap<>();
    private final Map<Integer, String> chapterTitleMap = new HashMap<>();
    private List<Integer> sortedChapters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_courses);

        spinnerLanguage = findViewById(R.id.spinner_language);
        spinnerChapter = findViewById(R.id.spinner_chapter);
        lessonList = findViewById(R.id.lesson_list);
        viewCoursesVideo = findViewById(R.id.view_courses_video);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();
        setupLanguageSpinner();
        setupBackButton();
        setupVideoAnimation();
    }

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupVideoAnimation() {
        String path = "android.resource://" + getPackageName() + "/" + R.raw.robotwaving;
        viewCoursesVideo.setVideoURI(Uri.parse(path));
        viewCoursesVideo.setBackgroundColor(Color.TRANSPARENT);

        viewCoursesVideo.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            viewCoursesVideo.start();
        });

        viewCoursesVideo.setOnErrorListener((mp, what, extra) -> {
            viewCoursesVideo.setVisibility(View.GONE);
            return true;
        });
    }

    private void setupLanguageSpinner() {
        firestore.collection("Languages").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> languages = new ArrayList<>();
                    Object langObj = doc.get("languages");
                    if (langObj instanceof List<?>) {
                        for (Object o : (List<?>) langObj) {
                            if (o instanceof String) languages.add((String) o);
                        }
                    }

                    if (languages.isEmpty()) {
                        Toast.makeText(this, "No languages found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerLanguage.setAdapter(adapter);
                    spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onNothingSelected(AdapterView<?> parent) {}
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedLanguage = languages.get(position);
                            loadUserProgress();
                        }
                    });

                    spinnerLanguage.setSelection(0);
                    selectedLanguage = languages.get(0);
                    loadUserProgress();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load languages", Toast.LENGTH_SHORT).show());
    }

    private void loadUserProgress() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("progress", true)
                .get()
                .addOnSuccessListener(qs -> {
                    completedLessonsMap.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Long chapterNum = doc.getLong("chapterNumber");
                        String lessonTitle = doc.getString("lessonTitle");
                        if (chapterNum != null && lessonTitle != null) {
                            int chap = chapterNum.intValue();
                            completedLessonsMap.computeIfAbsent(chap, k -> new ArrayList<>()).add(lessonTitle);
                        }
                    }

                    if (completedLessonsMap.isEmpty()) {
                        Toast.makeText(this, "No completed lessons found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sortedChapters = new ArrayList<>(completedLessonsMap.keySet());
                    Collections.sort(sortedChapters);
                    fetchChapterTitles();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading progress: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchChapterTitles() {
        chapterTitleMap.clear();
        int totalToFetch = sortedChapters.size();
        int[] fetched = {0};

        for (int chapNum : sortedChapters) {
            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", chapNum)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (!qs.isEmpty()) {
                            DocumentSnapshot doc = qs.getDocuments().get(0);
                            String title = doc.getString("chapterTitle");
                            Long num = doc.getLong("chapterNumber");
                            if (title != null && num != null) {
                                chapterTitleMap.put(num.intValue(), title);
                            }
                        }
                        if (++fetched[0] == totalToFetch) {
                            populateChapterSpinner();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading chapter titles", Toast.LENGTH_SHORT).show();
                        if (++fetched[0] == totalToFetch) {
                            populateChapterSpinner();
                        }
                    });
        }
    }

    private void populateChapterSpinner() {
        List<String> titles = new ArrayList<>();
        for (int chap : sortedChapters) {
            String title = chapterTitleMap.getOrDefault(chap, "Chapter " + chap);
            titles.add(title);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChapter.setAdapter(adapter);

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int chap = sortedChapters.get(position);
                displayLessons(chap);
            }
        });

        if (!sortedChapters.isEmpty()) {
            displayLessons(sortedChapters.get(0));
        }
    }

    private void displayLessons(int chapterNumber) {
        lessonList.removeAllViews();
        List<String> lessons = completedLessonsMap.getOrDefault(chapterNumber, Collections.emptyList());
        if (lessons.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No lessons completed in this chapter.");
            tv.setTextColor(getResources().getColor(R.color.primary_blue));
            tv.setPadding(16, 8, 0, 8);
            lessonList.addView(tv);
        } else {
            for (String lesson : lessons) {
                TextView tv = new TextView(this);
                tv.setText("• " + lesson);
                tv.setTextColor(getResources().getColor(R.color.primary_blue));
                tv.setTextSize(16);
                tv.setPadding(16, 8, 0, 8);
                lessonList.addView(tv);
            }
        }
    }
}
