package com.example.cognibyte.Admin;

import android.os.Bundle;
import android.text.InputType;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.Arrays;
import java.util.List;

public class ViewQuizContentActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerSkillLevel;
    private EditText etChapterNumber, etLessonNumber, etQuizQuestion;
    private Button btnSelect, btnEdit, btnSave;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "Select Language";
    private String selectedSkillLevel = "Select Skill Level";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_quiz_content);
        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerSkillLevel = findViewById(R.id.spinnerSkillLevel);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        etQuizQuestion = findViewById(R.id.etQuizQuestion);
        btnSelect = findViewById(R.id.btnSelect);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        firestore = FirebaseFirestore.getInstance();
        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedLanguage = languages[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        String[] skillLevels = {"Beginner", "Intermediate", "Expert"};
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, skillLevels);
        spinnerSkillLevel.setAdapter(skillAdapter);
        spinnerSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedSkillLevel = skillLevels[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        btnBack.setOnClickListener(v -> finish());
        btnSelect.setOnClickListener(v -> {
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            if(chapterStr.isEmpty() || lessonStr.isEmpty()){
                Toast.makeText(ViewQuizContentActivity.this, "Please enter chapter and lesson numbers", Toast.LENGTH_SHORT).show();
                return;
            }
            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
            } catch (NumberFormatException e) {
                Toast.makeText(ViewQuizContentActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return;
            }
            if(chapterNumber < 1 || chapterNumber > 10 || lessonNumber < 0 || lessonNumber > 9){
                Toast.makeText(ViewQuizContentActivity.this, "Chapter must be 1-10 and Lesson must be 0-9", Toast.LENGTH_SHORT).show();
                return;
            }
            firestore.collection("QuizContent")
                    .document(selectedLanguage)
                    .collection("Quizzes")
                    .whereEqualTo("chapterNumber", chapterNumber)
                    .whereEqualTo("lessonNumber", lessonNumber)
                    .whereEqualTo("skillLevel", selectedSkillLevel)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if(!querySnapshot.isEmpty()){
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            List<String> questions = (List<String>) document.get("question");
                            if(questions != null && !questions.isEmpty()){
                                String allQuestions = String.join("\n\n", questions);
                                etQuizQuestion.setText(allQuestions);
                                etQuizQuestion.setEnabled(false);
                            } else {
                                Toast.makeText(ViewQuizContentActivity.this, "Questions not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ViewQuizContentActivity.this, "No quiz content found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ViewQuizContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        btnEdit.setOnClickListener(v -> {
            etQuizQuestion.setEnabled(true);
            etQuizQuestion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        });
        btnSave.setOnClickListener(v -> {
            String newQuestionsText = etQuizQuestion.getText().toString().trim();
            if(newQuestionsText.isEmpty()){
                Toast.makeText(ViewQuizContentActivity.this, "Questions cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
            } catch (NumberFormatException e) {
                Toast.makeText(ViewQuizContentActivity.this, "Invalid chapter or lesson number", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> updatedQuestions = Arrays.asList(newQuestionsText.split("\n\n"));
            firestore.collection("QuizContent")
                    .document(selectedLanguage)
                    .collection("Quizzes")
                    .whereEqualTo("chapterNumber", chapterNumber)
                    .whereEqualTo("lessonNumber", lessonNumber)
                    .whereEqualTo("skillLevel", selectedSkillLevel)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if(!querySnapshot.isEmpty()){
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            document.getReference().update("question", updatedQuestions, "lastEdit", FieldValue.serverTimestamp())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ViewQuizContentActivity.this, "Quiz content updated", Toast.LENGTH_SHORT).show();
                                        etQuizQuestion.setEnabled(false);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ViewQuizContentActivity.this, "Error updating quiz content", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(ViewQuizContentActivity.this, "No quiz content found to update", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ViewQuizContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
