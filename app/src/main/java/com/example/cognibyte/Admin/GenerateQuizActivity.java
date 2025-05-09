package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerateQuizActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerSkillLevel, spinnerChapter, spinnerLesson;
    private Button btnGenerateContent, btnViewContent, btnDisplayContent;
    private Button btnEditQuiz, btnSaveQuiz, btnNextQuestion, btnPreviousQuestion;
    private EditText tvSingleQuestion;
    private FirebaseFirestore firestore;
    private DocumentReference currentQuizRef;
    private String selectedLanguage = "", selectedSkillLevel = "", selectedChapter = "", selectedLesson = "";
    private List<DocumentSnapshot> questionsList;
    private int currentQuestionIndex = 0;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_quiz);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        btnGenerateContent = findViewById(R.id.btnGenerateQuiz);
        btnViewContent = findViewById(R.id.btnViewContent);
        btnDisplayContent = findViewById(R.id.btnDisplayContent);
        btnEditQuiz = findViewById(R.id.btnEditQuiz);
        btnSaveQuiz = findViewById(R.id.btnSaveQuiz);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);
        btnPreviousQuestion = findViewById(R.id.btnPreviousQuestion);
        tvSingleQuestion = findViewById(R.id.tvSingleQuestion);
        tvSingleQuestion.setEnabled(false);
        firestore = FirebaseFirestore.getInstance();

        setupSpinners();
        setupListeners();
    }

    private void setupSpinners() {
        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(langAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLanguage = languages[pos];
                loadChapterTitles();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] skills = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, skills);
        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedSkillLevel = skills[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedChapter = (String) p.getItemAtPosition(pos);
                loadLessonTitles(selectedChapter);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLesson = (String) p.getItemAtPosition(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnGenerateContent.setOnClickListener(v -> {
            if (selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
                Toast.makeText(this, "Please select a chapter and lesson", Toast.LENGTH_SHORT).show();
                return;
            }
            btnGenerateContent.setEnabled(false);

            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterTitle", selectedChapter)
                    .whereEqualTo("lessonTitle", selectedLesson)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (qs.isEmpty()) {
                            Toast.makeText(this, "Lesson not found", Toast.LENGTH_SHORT).show();
                            btnGenerateContent.setEnabled(true);
                            return;
                        }
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        String lessonContent = doc.getString("lessonContent");
                        String chapterId = doc.getString("chapterId");
                        String lessonTitle = doc.getString("lessonTitle");
                        Long chapNum = doc.getLong("chapterNumber");
                        Long lesNum = doc.getLong("lessonNumber");
                        int chapterNumber = chapNum != null ? chapNum.intValue() : 0;
                        int lessonNumber = lesNum != null ? lesNum.intValue() : 0;

                        if (lessonContent == null || lessonContent.isEmpty()) {
                            Toast.makeText(this, "Lesson content is empty", Toast.LENGTH_SHORT).show();
                            btnGenerateContent.setEnabled(true);
                            return;
                        }

                        requestQuizGeneration(chapterNumber, lessonNumber, selectedLanguage, selectedSkillLevel, lessonContent, chapterId, lessonTitle);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching lesson: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnGenerateContent.setEnabled(true);
                    });
        });

        btnDisplayContent.setOnClickListener(v -> loadQuiz());
        btnViewContent.setOnClickListener(v ->
                startActivity(new Intent(this, ViewQuizContentActivity.class))
        );

        btnEditQuiz.setOnClickListener(v -> enableEditing());
        btnSaveQuiz.setOnClickListener(v -> saveCurrentQuestion());

        btnNextQuestion.setOnClickListener(v -> {
            if (questionsList != null && currentQuestionIndex < questionsList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion();
            }
        });

        btnPreviousQuestion.setOnClickListener(v -> {
            if (questionsList != null && currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion();
            }
        });
    }

    private void loadQuiz() {
        if (selectedLanguage.isEmpty() || selectedSkillLevel.isEmpty() || selectedChapter.isEmpty() || selectedLesson.isEmpty()) {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("QuizContent")
                .document(selectedLanguage)
                .collection("Quizzes")
                .whereEqualTo("chapterTitle", selectedChapter)
                .whereEqualTo("lessonTitle", selectedLesson)
                .whereEqualTo("skillLevel", selectedSkillLevel)
                .get()
                .addOnSuccessListener(quizSnapshots -> {
                    if (quizSnapshots.isEmpty()) {
                        Toast.makeText(this, "No quiz found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot quizDoc = quizSnapshots.getDocuments().get(0);
                    currentQuizRef = quizDoc.getReference();

                    currentQuizRef.collection("Questions")
                            .orderBy(FieldPath.documentId())
                            .get()
                            .addOnSuccessListener(questionSnaps -> {
                                questionsList = questionSnaps.getDocuments();
                                if (!questionsList.isEmpty()) {
                                    currentQuestionIndex = 0;
                                    displayQuestion();
                                }
                            });
                });
    }

    private void displayQuestion() {
        if (questionsList == null || questionsList.isEmpty() || currentQuestionIndex >= questionsList.size()) return;

        DocumentSnapshot currentQ = questionsList.get(currentQuestionIndex);

        String question = currentQ.getString("question");
        List<String> options = (List<String>) currentQ.get("options");
        String answer = currentQ.getString("answer");
        String explanation = currentQ.getString("explanation");

        StringBuilder fullText = new StringBuilder();
        if (question != null) fullText.append("Question: ").append(question).append("\n\n");

        if (options != null && options.size() >= 4) {
            fullText.append("A) ").append(options.get(0)).append("\n")
                    .append("B) ").append(options.get(1)).append("\n")
                    .append("C) ").append(options.get(2)).append("\n")
                    .append("D) ").append(options.get(3)).append("\n\n");
        }

        if (answer != null) fullText.append("Correct Answer: ").append(answer).append("\n\n");
        if (explanation != null) fullText.append("Explanation: ").append(explanation);

        tvSingleQuestion.setText(fullText.toString());
        tvSingleQuestion.setEnabled(false);
        isEditing = false;
    }

    private void enableEditing() {
        if (questionsList == null || questionsList.isEmpty()) return;
        tvSingleQuestion.setEnabled(true);
        isEditing = true;
    }

    private void saveCurrentQuestion() {
        if (!isEditing || questionsList == null || currentQuestionIndex >= questionsList.size()) return;

        String[] lines = tvSingleQuestion.getText().toString().split("\n");

        Map<String, Object> updatedFields = new HashMap<>();
        try {
            updatedFields.put("question", lines[0].replace("Question: ", "").trim());
            updatedFields.put("optionA", lines[2].replace("A) ", "").trim());
            updatedFields.put("optionB", lines[3].replace("B) ", "").trim());
            updatedFields.put("optionC", lines[4].replace("C) ", "").trim());
            updatedFields.put("optionD", lines[5].replace("D) ", "").trim());
            updatedFields.put("correctAnswer", lines[7].replace("Correct Answer: ", "").trim());
            updatedFields.put("explanation", lines[9].replace("Explanation: ", "").trim());
        } catch (Exception e) {
            Toast.makeText(this, "Error formatting your input!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference currentQRef = questionsList.get(currentQuestionIndex).getReference();
        currentQRef.update(updatedFields)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Question updated successfully!", Toast.LENGTH_SHORT).show();
                    tvSingleQuestion.setEnabled(false);
                    isEditing = false;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save question!", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadChapterTitles() {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(chapters -> {
                    Set<String> titlesSet = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : chapters.getDocuments()) {
                        String t = doc.getString("chapterTitle");
                        if (t != null) titlesSet.add(t);
                    }
                    List<String> uniqueTitles = new ArrayList<>(titlesSet);
                    spinnerChapter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, uniqueTitles));
                });
    }

    private void loadLessonTitles(String chapterTitle) {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterTitle", chapterTitle)
                .get()
                .addOnSuccessListener(lessons -> {
                    List<String> titles = new ArrayList<>();
                    for (DocumentSnapshot doc : lessons.getDocuments()) {
                        String t = doc.getString("lessonTitle");
                        if (t != null) titles.add(t);
                    }
                    spinnerLesson.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles));
                });
    }

    private void requestQuizGeneration(int chapterNumber, int lessonNumber, String language, String skillLevel, String lessonContent, String chapterId, String lessonTitle) {
        OpenAIClient.generateQuiz(chapterNumber, lessonNumber, language, skillLevel, lessonContent, new OpenAIClient.QuizCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> questions) {
                OpenAIClient.saveQuizToFirestore(chapterNumber, lessonNumber, language, selectedChapter, lessonTitle, skillLevel, chapterId, questions);
                runOnUiThread(() -> {
                    Toast.makeText(GenerateQuizActivity.this, "Quiz generated and saved successfully", Toast.LENGTH_LONG).show();
                    btnGenerateContent.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(GenerateQuizActivity.this, "Quiz generation error: " + error, Toast.LENGTH_LONG).show();
                    btnGenerateContent.setEnabled(true);
                });
            }
        });
    }
}
