package com.example.cognibyte.HomePage.Recap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public class LessonRecapActivity extends AppCompatActivity {
    private ImageView backArrow;
    private Spinner chapterSpinner, lessonSpinner;
    private Button btnViewLessonRecap;
    private TextView tvLessonRecap;

    private FirebaseFirestore firestore;
    private String userId;
    private String language = "Java";

    private final Map<Integer,List<String>> completedMap = new HashMap<>();
    private List<Integer> sortedChapters = new ArrayList<>();
    private final Map<Integer,String> chapterTitleMap = new HashMap<>();

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
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(this,"User not logged in",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = u.getUid();

        backArrow.setOnClickListener(v -> {
            Intent i = new Intent(LessonRecapActivity.this, RecapActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        });

        btnViewLessonRecap.setOnClickListener(v -> fetchRecap());
        loadUserProgress();
    }

    private void loadUserProgress() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .whereEqualTo("progress", true)
                .get()
                .addOnSuccessListener(qs -> {
                    completedMap.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Long chapL = d.getLong("chapterNumber");
                        String lt  = d.getString("lessonTitle");
                        if (chapL == null || lt == null) continue;
                        int chap = chapL.intValue();
                        completedMap
                                .computeIfAbsent(chap, k -> new ArrayList<>())
                                .add(lt);
                    }
                    if (completedMap.isEmpty()) {
                        Toast.makeText(this,"No completed lessons yet.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sortedChapters = new ArrayList<>(completedMap.keySet());
                    Collections.sort(sortedChapters);
                    fetchChapterTitles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,"Error loading progress: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchChapterTitles() {
        chapterTitleMap.clear();
        for (int chapNum : sortedChapters) {
            firestore.collection("ChapterContent")
                    .document(language)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", chapNum)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (!qs.isEmpty()) {
                            DocumentSnapshot d = qs.getDocuments().get(0);
                            String ct = d.getString("chapterTitle");
                            if (ct != null) chapterTitleMap.put(chapNum, ct);
                        }
                        if (chapterTitleMap.size() == sortedChapters.size()) {
                            populateChapterSpinner();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,"Error fetching title for chap "+chapNum,
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void populateChapterSpinner() {
        List<String> titles = new ArrayList<>();
        for (int chapNum : sortedChapters) {
            titles.add(chapterTitleMap.getOrDefault(chapNum, "Chapter "+chapNum));
        }
        ArrayAdapter<String> ca = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, titles
        );
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chapterSpinner.setAdapter(ca);

        chapterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view,
                                                 int position, long id) {
                int chap = sortedChapters.get(position);
                populateLessonSpinner(chap);
            }
        });
        chapterSpinner.setSelection(0);
    }

    private void populateLessonSpinner(int chapterNumber) {
        List<String> completedLessons = completedMap.getOrDefault(chapterNumber, Collections.emptyList());

        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Pair<Integer,String>> pairs = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Long lnL = d.getLong("lessonNumber");
                        String lt = d.getString("lessonTitle");
                        if (lnL != null && lt != null && completedLessons.contains(lt)) {
                            pairs.add(new Pair<>(lnL.intValue(), lt));
                        }
                    }
                    Collections.sort(pairs, Comparator.comparingInt(p -> p.first));
                    List<String> sortedTitles = new ArrayList<>();
                    for (Pair<Integer,String> p : pairs) sortedTitles.add(p.second);

                    ArrayAdapter<String> la = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, sortedTitles
                    );
                    la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    lessonSpinner.setAdapter(la);
                    if (!sortedTitles.isEmpty()) lessonSpinner.setSelection(0);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,"Error loading lessons: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchRecap() {
        String chapTitle = (String) chapterSpinner.getSelectedItem();
        String lessonTitle = (String) lessonSpinner.getSelectedItem();
        if (chapTitle == null || lessonTitle == null) {
            Toast.makeText(this,"Please select both chapter & lesson",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int chapNum = sortedChapters.get(chapterSpinner.getSelectedItemPosition());
        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapNum)
                .whereEqualTo("lessonTitle", lessonTitle)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        tvLessonRecap.setText("No recap found.");
                    } else {
                        String recap = qs.getDocuments().get(0).getString("lessonRecap");
                        tvLessonRecap.setText(
                                (recap == null || recap.isEmpty())
                                        ? "No recap available."
                                        : recap
                        );
                    }
                })
                .addOnFailureListener(e ->
                        tvLessonRecap.setText("Error loading recap: "+e.getMessage())
                );
    }

    private static class Pair<F,S> {
        final F first; final S second;
        Pair(F f, S s) { first = f; second = s; }
    }
}
