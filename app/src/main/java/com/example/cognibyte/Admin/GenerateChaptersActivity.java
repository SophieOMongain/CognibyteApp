package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.EditLessonTextAdapter;
import com.example.cognibyte.Adapter.LessonTextAdapter;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateChaptersActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage;
    private EditText etChapterTitle, etChapterNumber;
    private EditText etLessonTitle, etLessonNumber, etDescription;
    private Button btnGenerateContent, btnViewContent, btnEditGenerated, btnSaveGenerated, btnDisplayContent;
    private RecyclerView rvGeneratedContent;
    private String selectedLanguage = "";
    private List<String> generatedLines = new ArrayList<>();
    private boolean isEditMode = false;
    private EditLessonTextAdapter editAdapter;
    private FirebaseFirestore firestore;
    private String chapterTitle = "", lessonTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_chapters);

        firestore = FirebaseFirestore.getInstance();
        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etChapterTitle = findViewById(R.id.etChapterTitle);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonTitle = findViewById(R.id.etLessonTitle);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        etDescription = findViewById(R.id.etDescription);
        btnGenerateContent = findViewById(R.id.btnGenerateContent);
        btnViewContent = findViewById(R.id.btnViewContent);
        btnEditGenerated = findViewById(R.id.btnEditGenerated);
        btnSaveGenerated = findViewById(R.id.btnSaveGenerated);
        btnDisplayContent = findViewById(R.id.btnDisplayContent);
        rvGeneratedContent = findViewById(R.id.rvGeneratedContent);
        rvGeneratedContent.setLayoutManager(new LinearLayoutManager(this));

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
            chapterTitle = etChapterTitle.getText().toString().trim();
            lessonTitle = etLessonTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String chapterNumber = etChapterNumber.getText().toString().trim();
            String lessonNumber = etLessonNumber.getText().toString().trim();

            if (chapterTitle.isEmpty() || lessonTitle.isEmpty() || description.isEmpty()
                    || chapterNumber.isEmpty() || lessonNumber.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int chapterNum, lessonNum;
            try {
                chapterNum = Integer.parseInt(chapterNumber);
                lessonNum = Integer.parseInt(lessonNumber);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Chapter/Lesson numbers must be integers", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chapterNum < 1 || chapterNum > 5 || lessonNum < 1 || lessonNum > 5) {
                Toast.makeText(this, "Numbers must be between 1 and 5", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerateContent.setEnabled(false);

            OpenAIClient.generateLesson(chapterNum, lessonNum, chapterTitle, lessonTitle, description, selectedLanguage,
                    new OpenAIClient.CompletionCallback() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> {
                                String lessonContent = result.trim();
                                generatedLines = Arrays.asList(lessonContent.split("\n"));
                                rvGeneratedContent.setAdapter(new LessonTextAdapter(generatedLines));
                                isEditMode = false;

                                btnEditGenerated.setVisibility(View.VISIBLE);
                                btnSaveGenerated.setVisibility(View.VISIBLE);

                                Toast.makeText(GenerateChaptersActivity.this, "Content generated!", Toast.LENGTH_SHORT).show();
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

        btnEditGenerated.setOnClickListener(v -> {
            if (!isEditMode && generatedLines != null) {
                editAdapter = new EditLessonTextAdapter(new ArrayList<>(generatedLines));
                rvGeneratedContent.setAdapter(editAdapter);
                isEditMode = true;
            }
        });

        btnSaveGenerated.setOnClickListener(v -> {
            if (!isEditMode || editAdapter == null) return;

            List<String> updatedLines = editAdapter.getItems();
            String lessonContent = String.join("\n", updatedLines);

            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .add(new LessonContent(
                            chapterTitle,
                            lessonTitle,
                            lessonContent,
                            "",
                            FieldValue.serverTimestamp()
                    ))
                    .addOnSuccessListener(documentReference ->
                            Toast.makeText(this, "Saved to Firestore", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnDisplayContent.setOnClickListener(v -> {
            if (chapterTitle.isEmpty() || lessonTitle.isEmpty()) {
                Toast.makeText(this, "Enter chapter and lesson titles first", Toast.LENGTH_SHORT).show();
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
                            String lessonContent = querySnapshot.getDocuments().get(0).getString("lessonContent");
                            List<String> contentList = lessonContent == null || lessonContent.isEmpty()
                                    ? new ArrayList<>() : Arrays.asList(lessonContent.split("\n"));
                            rvGeneratedContent.setAdapter(new LessonTextAdapter(contentList));
                            isEditMode = false;
                        } else {
                            Toast.makeText(this, "No content found for this chapter/lesson", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnViewContent.setOnClickListener(v -> startActivity(
                new Intent(this, ViewChapterContentActivity.class))
        );
    }

    public static class LessonContent {
        public String chapterTitle, lessonTitle, lessonContent, lessonRecap;
        public Object lastEdit;

        public LessonContent() {}

        public LessonContent(String chapterTitle, String lessonTitle, String lessonContent, String lessonRecap, Object lastEdit) {
            this.chapterTitle = chapterTitle;
            this.lessonTitle = lessonTitle;
            this.lessonContent = lessonContent;
            this.lessonRecap = lessonRecap;
            this.lastEdit = lastEdit;
        }
    }
}
