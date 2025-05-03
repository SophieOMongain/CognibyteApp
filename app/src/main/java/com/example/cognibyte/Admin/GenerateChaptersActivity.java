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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.EditLessonTextAdapter;
import com.example.cognibyte.Adapter.LessonTextAdapter;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateChaptersActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage;
    private EditText etChapterTitle, etChapterNumber, etLessonTitle, etLessonNumber, etDescription;
    private Button btnGenerateContent, btnViewContent;
    private Button btnDisplayContent, btnEditContent, btnSaveContent;
    private RecyclerView rvDisplayedContent;
    private String selectedLanguage = "";
    private FirebaseFirestore firestore;
    private List<String> lessonContentList = new ArrayList<>();
    private LessonTextAdapter lessonReadAdapter;
    private EditLessonTextAdapter lessonEditAdapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_chapters);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etChapterTitle = findViewById(R.id.etChapterTitle);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonTitle = findViewById(R.id.etLessonTitle);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        etDescription = findViewById(R.id.etDescription);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);
        btnDisplayContent = findViewById(R.id.btnDisplayContent);
        btnEditContent = findViewById(R.id.btnEditContent);
        btnSaveContent = findViewById(R.id.btnSaveContent);
        rvDisplayedContent = findViewById(R.id.rvDisplayedContent);
        firestore = FirebaseFirestore.getInstance();
        rvDisplayedContent.setLayoutManager(new LinearLayoutManager(this));
        btnEditContent.setVisibility(View.GONE);
        btnSaveContent.setVisibility(View.GONE);

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedLanguage = languages[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnBack.setOnClickListener(v -> finish());

        btnGenerateContent.setOnClickListener(v -> {
            String chapterTitle = etChapterTitle.getText().toString().trim();
            String lessonTitle = etLessonTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String chapterNumber = etChapterNumber.getText().toString().trim();
            String lessonNumber = etLessonNumber.getText().toString().trim();

            if (chapterTitle.isEmpty() || lessonTitle.isEmpty() || description.isEmpty()
                    || chapterNumber.isEmpty() || lessonNumber.isEmpty()) {
                Toast.makeText(this, "Fill in chapter #, lesson #, titles & description", Toast.LENGTH_SHORT).show();
                return;
            }

            int chapterNum, lesNum;
            try {
                chapterNum = Integer.parseInt(chapterNumber);
                lesNum = Integer.parseInt(lessonNumber);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Chapter/Lesson numbers must be integers", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chapterNum < 1 || chapterNum > 5 || lesNum < 1 || lesNum > 5) {
                Toast.makeText(this, "Numbers must be between 1 and 5", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerateContent.setEnabled(false);

            OpenAIClient.generateLesson(chapterNum, lesNum, chapterTitle, lessonTitle, description, selectedLanguage,
                    new OpenAIClient.CompletionCallback() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> {
                                Toast.makeText(GenerateChaptersActivity.this, "Content generated and saved!", Toast.LENGTH_LONG).show();
                                btnGenerateContent.setEnabled(true);
                            });
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(GenerateChaptersActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                btnGenerateContent.setEnabled(true);
                            });
                        }
                    });
        });

        btnViewContent.setOnClickListener(v ->
                startActivity(new Intent(this, ViewChapterContentActivity.class))
        );

        btnDisplayContent.setOnClickListener(v -> {
            String chapterTitle = etChapterTitle.getText().toString().trim();
            String lessonTitle = etLessonTitle.getText().toString().trim();

            if (chapterTitle.isEmpty() || lessonTitle.isEmpty() || selectedLanguage.isEmpty()) {
                Toast.makeText(this, "Chapter title, lesson title and language must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterTitle", chapterTitle)
                    .whereEqualTo("lessonTitle", lessonTitle)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String lessonContent = doc.getString("lessonContent");
                            lessonContentList = lessonContent != null ? Arrays.asList(lessonContent.split("\n")) : new ArrayList<>();
                            lessonReadAdapter = new LessonTextAdapter(lessonContentList);
                            rvDisplayedContent.setAdapter(lessonReadAdapter);
                            btnEditContent.setVisibility(View.VISIBLE);
                            btnSaveContent.setVisibility(View.VISIBLE);
                            isEditMode = false;
                        } else {
                            Toast.makeText(this, "No lesson content found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load content: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnEditContent.setOnClickListener(v -> {
            if (!isEditMode) {
                lessonEditAdapter = new EditLessonTextAdapter(new ArrayList<>(lessonContentList));
                rvDisplayedContent.setAdapter(lessonEditAdapter);
                isEditMode = true;
            }
        });

        btnSaveContent.setOnClickListener(v -> {
            if (isEditMode) {
                List<String> updatedContent = lessonEditAdapter.getItems();
                String updatedString = String.join("\n", updatedContent);
                String chapterTitle = etChapterTitle.getText().toString().trim();
                String lessonTitle = etLessonTitle.getText().toString().trim();

                firestore.collection("ChapterContent")
                        .document(selectedLanguage)
                        .collection("Chapters")
                        .whereEqualTo("chapterTitle", chapterTitle)
                        .whereEqualTo("lessonTitle", lessonTitle)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                doc.getReference().update(
                                        "lessonContent", updatedString,
                                        "lastEdit", FieldValue.serverTimestamp()
                                ).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Content saved", Toast.LENGTH_SHORT).show();
                                    lessonContentList = updatedContent;
                                    rvDisplayedContent.setAdapter(new LessonTextAdapter(lessonContentList));
                                    isEditMode = false;
                                }).addOnFailureListener(e ->
                                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
            }
        });
    }
}
