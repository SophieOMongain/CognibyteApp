package com.example.cognibyte.Admin;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.EditLessonTextAdapter;
import com.example.cognibyte.Adapter.LessonTextAdapter;
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewQuizContentActivity extends AppCompatActivity {

    private DocumentReference currentQuizRef;
    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerSkillLevel, spinnerChapter, spinnerLesson;
    private Button btnSelect, btnEdit, btnSave;
    private RecyclerView rvQuestions;
    private FirebaseFirestore firestore;

    private String selectedLanguage  = "";
    private String selectedSkillLevel = "";
    private String selectedChapter   = "";
    private String selectedLesson    = "";

    private LessonTextAdapter readAdapter;
    private EditLessonTextAdapter editAdapter;
    private boolean isEditMode = false;
    private final List<String> currentDisplay = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_quiz_content);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        btnSelect = findViewById(R.id.btnSelect);
        btnEdit = findViewById(R.id.btnEditQuiz);
        btnSave = findViewById(R.id.btnSaveQuiz);
        rvQuestions = findViewById(R.id.rvQuestions);
        firestore = FirebaseFirestore.getInstance();
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        readAdapter = new LessonTextAdapter(currentDisplay);
        rvQuestions.setAdapter(readAdapter);
        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, languages
        );
        spinnerLanguage.setAdapter(langAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLanguage = languages[pos];
                loadChapterTitles();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] skills = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, skills
        );

        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedSkillLevel = skills[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedChapter = (String)p.getItemAtPosition(pos);
                loadLessonTitles(selectedChapter);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLesson = (String)p.getItemAtPosition(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnBack.setOnClickListener(v -> finish());

        btnSelect.setOnClickListener(v -> {
            if (selectedLanguage.isEmpty() || selectedSkillLevel.isEmpty() || selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
                Toast.makeText(this,
                        "Please select language, skill level, chapter & lesson",
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
                    .get()
                    .addOnSuccessListener(quizQs -> {
                        if (quizQs.isEmpty()) {
                            Toast.makeText(this,
                                    "No quiz found for these selections",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        DocumentSnapshot quizDoc = quizQs.getDocuments().get(0);
                        currentQuizRef = quizDoc.getReference();

                        currentQuizRef.collection("Questions")
                                .orderBy(FieldPath.documentId())
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    if (qs2.isEmpty()) {
                                        Toast.makeText(this,
                                                "Quiz exists but has no questions",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        return;
                                    }

                                    currentDisplay.clear();
                                    for (DocumentSnapshot qSnap : qs2.getDocuments()) {
                                        String question = qSnap.getString("question");
                                        if (question != null) {
                                            currentDisplay.add(question);
                                        }
                                    }
                                    isEditMode = false;
                                    readAdapter = new LessonTextAdapter(currentDisplay);
                                    rvQuestions.setAdapter(readAdapter);
                                    btnEdit.setVisibility(View.VISIBLE);
                                    btnSave.setVisibility(View.VISIBLE);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error loading questions: " + e.getMessage(),
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error querying quiz: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
        });

        btnEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                editAdapter = new EditLessonTextAdapter(new ArrayList<>(currentDisplay));
                rvQuestions.setAdapter(editAdapter);
                isEditMode = true;
                btnEdit.setText("Cancel");
            } else {
                rvQuestions.setAdapter(readAdapter);
                isEditMode = false;
                btnEdit.setText("Edit");
            }
        });

        btnSave.setOnClickListener(v -> {
            if (!isEditMode || currentQuizRef == null) {
                Toast.makeText(this,
                        "Nothing to save",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            List<String> updated = editAdapter.getItems();
            Map<String,Object> updates = new HashMap<>();
            updates.put("questions", updated);
            updates.put("lastEdit", com.google.firebase.firestore.FieldValue.serverTimestamp());

            currentQuizRef.update(updates)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this,
                                "Quiz updated successfully",
                                Toast.LENGTH_SHORT
                        ).show();
                        currentDisplay.clear();
                        currentDisplay.addAll(updated);
                        readAdapter = new LessonTextAdapter(currentDisplay);
                        rvQuestions.setAdapter(readAdapter);
                        isEditMode = false;
                        btnEdit.setText("Edit");
                    })
                    .addOnFailureListener(e -> Toast.makeText(
                            this,
                            "Failed to save quiz: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show());
        });
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
                            this, android.R.layout.simple_spinner_dropdown_item, chapters
                    ));
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Error loading chapters: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
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
                            this, android.R.layout.simple_spinner_dropdown_item, lessons
                    ));
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Error loading lessons: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
