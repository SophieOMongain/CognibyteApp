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

public class QuizRecapActivity extends AppCompatActivity {

    private ImageView backArrow;
    private Spinner spinnerLanguage, spinnerSkillLevel, spinnerChapter, spinnerLesson;
    private Button btnViewQuizRecap, btnRetakeQuiz;
    private TextView tvQuizRecap;
    private FirebaseFirestore firestore;
    private String selectedLanguage;
    private String selectedSkillLevel;
    private Map<Integer, List<String>> completedChapters = new HashMap<>();
    private List<Integer> sortedChapterNumbers = new ArrayList<>();
    private Map<Integer, String> chapterTitleMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_recap);

        initViews();
        setupFirebase();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupListeners();
        setupLanguageSpinner();
        setupSkillLevelSpinner();
    }

    private void initViews() {
        backArrow = findViewById(R.id.back_arrow);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        btnViewQuizRecap = findViewById(R.id.btnViewQuizRecap);
        btnRetakeQuiz = findViewById(R.id.btnRetakeQuiz);
        tvQuizRecap = findViewById(R.id.tvQuizRecap);
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupListeners() {
        backArrow.setOnClickListener(v -> startActivity(
                new Intent(QuizRecapActivity.this, RecapActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        btnViewQuizRecap.setOnClickListener(v -> loadAndDisplayQuiz());

        btnRetakeQuiz.setOnClickListener(v -> {
            String chapter = (String) spinnerChapter.getSelectedItem();
            String lesson = (String) spinnerLesson.getSelectedItem();

            if (selectedLanguage == null || selectedSkillLevel == null || chapter == null || lesson == null) {
                Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, FullQuizRetakeActivity.class);
            intent.putExtra("language", selectedLanguage);
            intent.putExtra("skill", selectedSkillLevel);
            intent.putExtra("chapter", chapter);
            intent.putExtra("lesson", lesson);
            startActivity(intent);
        });
    }

    private void setupLanguageSpinner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        firestore.collection("Languages").document(user.getUid()).get().addOnSuccessListener(doc -> {
            List<String> userLanguages = (List<String>) doc.get("languages");
            if (userLanguages == null || userLanguages.isEmpty()) return;

            spinnerLanguage.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, userLanguages));

            spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedLanguage = userLanguages.get(position);
                    loadUserProgress(selectedLanguage);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            spinnerLanguage.setSelection(0);
            selectedLanguage = userLanguages.get(0);
            loadUserProgress(selectedLanguage);
        });
    }

    private void setupSkillLevelSpinner() {
        String[] skills = {"Beginner", "Intermediate", "Expert"};
        spinnerSkillLevel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, skills));
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSkillLevel = skills[position];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerSkillLevel.setSelection(0);
        selectedSkillLevel = skills[0];
    }

    private void loadUserProgress(String language) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        firestore.collection("UserProgress").document(user.getUid()).collection("Languages")
                .document(language).collection("Chapters").whereEqualTo("progress", true)
                .get().addOnSuccessListener(q -> {
                    completedChapters.clear();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        int chapter = d.getLong("chapterNumber").intValue();
                        String lesson = d.getString("lessonTitle");
                        completedChapters.computeIfAbsent(chapter, k -> new ArrayList<>()).add(lesson);
                    }
                    sortedChapterNumbers = new ArrayList<>(completedChapters.keySet());
                    Collections.sort(sortedChapterNumbers);
                    fetchCompletedChapterTitles(language);
                });
    }

    private void fetchCompletedChapterTitles(String language) {
        chapterTitleMap.clear();
        for (int chapNum : sortedChapterNumbers) {
            firestore.collection("ChapterContent").document(language).collection("Chapters")
                    .whereEqualTo("chapterNumber", chapNum).limit(1).get().addOnSuccessListener(q -> {
                        if (!q.isEmpty()) {
                            String title = q.getDocuments().get(0).getString("chapterTitle");
                            chapterTitleMap.put(chapNum, title);
                        }
                        if (chapterTitleMap.size() == sortedChapterNumbers.size()) populateChapterSpinner();
                    });
        }
    }

    private void populateChapterSpinner() {
        List<String> titles = new ArrayList<>();
        sortedChapterNumbers.forEach(num -> titles.add(chapterTitleMap.get(num)));

        spinnerChapter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles));
        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int chapNum = sortedChapterNumbers.get(pos);
                populateLessonSpinner(chapNum);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerChapter.setSelection(0);
    }

    private void populateLessonSpinner(int chapterNumber) {
        spinnerLesson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, completedChapters.get(chapterNumber)));
        spinnerLesson.setSelection(0);
    }

    private void loadAndDisplayQuiz() {
        String selectedChapter = (String) spinnerChapter.getSelectedItem();
        String selectedLesson = (String) spinnerLesson.getSelectedItem();

        if (selectedLanguage == null || selectedLanguage.isEmpty() ||
                selectedSkillLevel == null || selectedSkillLevel.isEmpty() ||
                selectedChapter == null || selectedChapter.isEmpty() ||
                selectedLesson == null || selectedLesson.isEmpty()) {
            Toast.makeText(this, "Please select language, skill, chapter & lesson", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("QuizContent")
                .document(selectedLanguage)
                .collection("Quizzes")
                .whereEqualTo("chapterTitle", selectedChapter)
                .whereEqualTo("lessonTitle", selectedLesson)
                .whereEqualTo("skillLevel", selectedSkillLevel)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        tvQuizRecap.setText("No quiz found for those selections.");
                        return;
                    }
                    DocumentSnapshot quizDoc = qs.getDocuments().get(0);
                    DocumentReference quizRef = quizDoc.getReference();

                    quizRef.collection("Questions")
                            .orderBy(FieldPath.documentId())
                            .get()
                            .addOnSuccessListener(q2 -> {
                                if (!q2.isEmpty()) {
                                    displayFromSnapshots(q2.getDocuments());
                                } else {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String,Object>> rawQs = (List<Map<String,Object>>) quizDoc.get("questions");
                                    if (rawQs != null && !rawQs.isEmpty()) {
                                        displayFromMaps(rawQs);
                                    } else {
                                        tvQuizRecap.setText("Quiz exists but has no questions.");
                                    }
                                }
                            })
                            .addOnFailureListener(e ->
                                    tvQuizRecap.setText("Error loading questions: " + e.getMessage())
                            );
                })
                .addOnFailureListener(e ->
                        tvQuizRecap.setText("Error loading quiz: " + e.getMessage())
                );
    }

    private void displayFromSnapshots(List<DocumentSnapshot> docs) {
        StringBuilder out = new StringBuilder();
        int qNo = 1;
        for (DocumentSnapshot d : docs) {
            String question = d.getString("question");
            @SuppressWarnings("unchecked")
            List<String> opts = (List<String>) d.get("options");
            String answer = d.getString("answer");
            String explanation = d.getString("explanation");

            out.append(qNo++).append(". ").append(question).append("\n");
            char label = 'A';
            for (String o : opts) {
                out.append("   ").append(label++).append(") ").append(o).append("\n");
            }
            out.append("Answer: ").append(answer).append("\n")
                    .append("Explanation: ").append(explanation).append("\n\n");
        }
        tvQuizRecap.setText(out.toString().trim());
    }

    private void displayFromMaps(List<Map<String,Object>> rawQs) {
        StringBuilder out = new StringBuilder();
        int qNo = 1;
        for (Map<String,Object> q : rawQs) {
            String question = (String) q.get("question");
            @SuppressWarnings("unchecked")
            List<String> opts = (List<String>) q.get("options");
            String answer = (String) q.get("answer");
            String explanation = (String) q.get("explanation");

            out.append(qNo++).append(". ").append(question).append("\n");
            char label = 'A';
            for (String o : opts) {
                out.append("   ").append(label++).append(") ").append(o).append("\n");
            }
            out.append("Answer: ").append(answer).append("\n")
                    .append("Explanation: ").append(explanation).append("\n\n");
        }
        tvQuizRecap.setText(out.toString().trim());
    }
}
