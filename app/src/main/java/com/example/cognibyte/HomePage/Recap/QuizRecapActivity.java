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
    private String selectedLanguage = "";
    private String selectedSkillLevel = "";
    private String selectedChapter = "";
    private String selectedLesson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_recap);

        backArrow = findViewById(R.id.back_arrow);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        btnViewQuizRecap = findViewById(R.id.btnViewQuizRecap);
        btnRetakeQuiz = findViewById(R.id.btnRetakeQuiz);
        tvQuizRecap = findViewById(R.id.tvQuizRecap);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backArrow.setOnClickListener(v -> startActivity(
                new Intent(QuizRecapActivity.this, RecapActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ));

        setupLanguageSpinner();
        setupSkillLevelSpinner();

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedChapter = (String) parent.getItemAtPosition(position);
                loadLessonTitles(selectedLanguage, selectedChapter);
            }
        });

        spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLesson = (String) parent.getItemAtPosition(position);
            }
        });

        btnViewQuizRecap.setOnClickListener(v -> loadAndDisplayQuiz());

        btnRetakeQuiz.setOnClickListener(v -> {
            if (selectedLanguage.isEmpty() || selectedSkillLevel.isEmpty() || selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
                Toast.makeText(this, "Please make all selections first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(QuizRecapActivity.this, FullQuizRetakeActivity.class);
            intent.putExtra("language", selectedLanguage);
            intent.putExtra("skill", selectedSkillLevel);
            intent.putExtra("chapter", selectedChapter);
            intent.putExtra("lesson", selectedLesson);
            startActivity(intent);
        });
    }

    private void setupLanguageSpinner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();

        firestore.collection("Languages").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> userLanguages = new ArrayList<>();
                    Object arr = doc.get("languages");
                    if (arr instanceof List<?>) {
                        for (Object o : (List<?>) arr) {
                            if (o instanceof String) userLanguages.add((String) o);
                        }
                    }
                    if (userLanguages.isEmpty()) {
                        Toast.makeText(this, "No languages added yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayAdapter<String> langAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_dropdown_item, userLanguages
                    );
                    spinnerLanguage.setAdapter(langAdapter);

                    spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onNothingSelected(AdapterView<?> parent) {}
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedLanguage = userLanguages.get(position);
                            loadChapterTitles(selectedLanguage);
                        }
                    });

                    spinnerLanguage.setSelection(0);
                    selectedLanguage = userLanguages.get(0);
                    loadChapterTitles(selectedLanguage);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading languages: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void setupSkillLevelSpinner() {
        String[] skills = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, skills
        );
        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSkillLevel = skills[position];
            }
        });
        spinnerSkillLevel.setSelection(0);
        selectedSkillLevel = skills[0];
    }

    private void loadChapterTitles(String language) {
        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> chapters = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String title = d.getString("chapterTitle");
                        if (title != null && !chapters.contains(title)) {
                            chapters.add(title);
                        }
                    }
                    Collections.sort(chapters);
                    ArrayAdapter<String> chapAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_dropdown_item, chapters
                    );
                    spinnerChapter.setAdapter(chapAdapter);
                    if (!chapters.isEmpty()) {
                        spinnerChapter.setSelection(0);
                        selectedChapter = chapters.get(0);
                        loadLessonTitles(language, selectedChapter);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading chapters: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void loadLessonTitles(String language, String chapterTitle) {
        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterTitle", chapterTitle)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> lessons = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        String lt = d.getString("lessonTitle");
                        if (lt != null && !lessons.contains(lt)) {
                            lessons.add(lt);
                        }
                    }
                    Collections.sort(lessons);
                    ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_dropdown_item, lessons
                    );
                    spinnerLesson.setAdapter(lessonAdapter);
                    if (!lessons.isEmpty()) {
                        spinnerLesson.setSelection(0);
                        selectedLesson = lessons.get(0);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading lessons: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void loadAndDisplayQuiz() {
        if (selectedLanguage.isEmpty() || selectedSkillLevel.isEmpty() || selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
            Toast.makeText(this,
                    "Please select language, skill, chapter & lesson",
                    Toast.LENGTH_SHORT
            ).show();
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
                                    List<Map<String,Object>> rawQs =
                                            (List<Map<String,Object>>) quizDoc.get("questions");
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
            String question    = d.getString("question");
            @SuppressWarnings("unchecked")
            List<String> opts  = (List<String>) d.get("options");
            String answer      = d.getString("answer");
            String explanation = d.getString("explanation");

            out.append(qNo++).append(". ").append(question).append("\n");
            char label = 'A';
            for (String o : opts) {
                out.append("   ").append(label++).append(") ").append(o).append("\n");
            }
            out.append("Answer: ").append(answer).append("\n")
                    .append("Explanation: ").append(explanation)
                    .append("\n\n");
        }
        tvQuizRecap.setText(out.toString().trim());
    }

    private void displayFromMaps(List<Map<String,Object>> rawQs) {
        StringBuilder out = new StringBuilder();
        int qNo = 1;
        for (Map<String,Object> q : rawQs) {
            String question    = (String) q.get("question");
            @SuppressWarnings("unchecked")
            List<String> opts  = (List<String>) q.get("options");
            String answer      = (String) q.get("answer");
            String explanation = (String) q.get("explanation");

            out.append(qNo++).append(". ").append(question).append("\n");
            char label = 'A';
            for (String o : opts) {
                out.append("   ").append(label++).append(") ").append(o).append("\n");
            }
            out.append("Answer: ").append(answer).append("\n")
                    .append("Explanation: ").append(explanation)
                    .append("\n\n");
        }
        tvQuizRecap.setText(out.toString().trim());
    }
}
