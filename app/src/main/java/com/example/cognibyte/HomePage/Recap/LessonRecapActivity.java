package com.example.cognibyte.HomePage.Recap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LessonRecapActivity extends AppCompatActivity {
    private ImageView backArrow;
    private Spinner chapterSpinner, lessonSpinner;
    private Button btnViewLessonRecap;
    private TextView tvLessonRecap;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    private Map<Integer, Integer> chapterProgressMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_recap);

        backArrow = findViewById(R.id.back_arrow);
        chapterSpinner = findViewById(R.id.spinnerChapter);
        lessonSpinner = findViewById(R.id.spinnerLesson);
        btnViewLessonRecap = findViewById(R.id.btnViewLessonRecap);
        tvLessonRecap = findViewById(R.id.tvLessonRecap);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(LessonRecapActivity.this, RecapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnViewLessonRecap.setOnClickListener(v -> {
            Integer selectedChapter = (Integer) chapterSpinner.getSelectedItem();
            Integer selectedLesson = (Integer) lessonSpinner.getSelectedItem();
            if (selectedChapter == null || selectedLesson == null) {
                Toast.makeText(LessonRecapActivity.this, "Please select both chapter and lesson", Toast.LENGTH_SHORT).show();
                return;
            }

            String language = "Java";

            firestore.collection("ChapterContent")
                    .document(language)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", selectedChapter)
                    .whereEqualTo("lessonNumber", selectedLesson)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String recap = doc.getString("lessonRecap");
                            if (recap != null && !recap.isEmpty()) {
                                tvLessonRecap.setText(recap);
                            } else {
                                tvLessonRecap.setText("No recap available.");
                            }
                        } else {
                            tvLessonRecap.setText("No recap found.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvLessonRecap.setText("Error loading recap: " + e.getMessage());
                    });
        });

        loadUserProgress();
    }

    private void loadUserProgress() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    chapterProgressMap.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long chapNumLong = doc.getLong("chapterNumber");
                        Long lessonNumLong = doc.getLong("lessonNumber");
                        if (chapNumLong != null && lessonNumLong != null) {
                            int chapNum = chapNumLong.intValue();
                            int lessonNum = lessonNumLong.intValue();
                            if (chapterProgressMap.containsKey(chapNum)) {
                                int currentMax = chapterProgressMap.get(chapNum);
                                if (lessonNum > currentMax) {
                                    chapterProgressMap.put(chapNum, lessonNum);
                                }
                            } else {
                                chapterProgressMap.put(chapNum, lessonNum);
                            }
                        }
                    }
                    if (chapterProgressMap.isEmpty()) {
                        Toast.makeText(LessonRecapActivity.this, "No progress found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    populateChapterSpinner();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LessonRecapActivity.this, "Error loading progress: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void populateChapterSpinner() {
        List<Integer> chapters = new ArrayList<>(chapterProgressMap.keySet());
        Collections.sort(chapters);
        ArrayAdapter<Integer> chapterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chapters);
        chapterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chapterSpinner.setAdapter(chapterAdapter);
        chapterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Integer selectedChapter = (Integer) parent.getItemAtPosition(position);
                populateLessonSpinner(selectedChapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        if (!chapters.isEmpty()) {
            chapterSpinner.setSelection(0);
            populateLessonSpinner(chapters.get(0));
        }
    }

    private void populateLessonSpinner(int chapterNumber) {
        int maxLesson = chapterProgressMap.get(chapterNumber);
        List<Integer> lessons = new ArrayList<>();
        for (int i = 1; i <= maxLesson; i++) {
            lessons.add(i);
        }
        ArrayAdapter<Integer> lessonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lessons);
        lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lessonSpinner.setAdapter(lessonAdapter);
    }
}
