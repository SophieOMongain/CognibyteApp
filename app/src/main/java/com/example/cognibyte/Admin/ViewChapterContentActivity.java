package com.example.cognibyte.Admin;

import android.os.Bundle;
import android.text.InputType;
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
import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewChapterContentActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerChapter, spinnerLesson;
    private EditText etLastEdited;
    private Button btnSelect, btnEdit, btnSave;
    private RecyclerView rvLessonContent, rvLessonRecap;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "Select Language";
    private String selectedChapter = "";
    private String selectedLesson = "";
    private String currentLessonContent = "";
    private String currentLessonRecap = "";
    private List<String> contentList = new ArrayList<>();
    private List<String> recapList = new ArrayList<>();
    private boolean isEditMode = false;
    private LessonTextAdapter readAdapter;
    private EditLessonTextAdapter editContentAdapter;
    private EditLessonTextAdapter editRecapAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_chapter_content);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerChapter = findViewById(R.id.spinnerChapter);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        etLastEdited = findViewById(R.id.LastEdited);
        btnSelect = findViewById(R.id.btnSelect);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        rvLessonContent = findViewById(R.id.rvLessonContent);
        rvLessonRecap = findViewById(R.id.rvLessonRecap);
        firestore = FirebaseFirestore.getInstance();

        rvLessonContent.setLayoutManager(new LinearLayoutManager(this));
        rvLessonRecap.setLayoutManager(new LinearLayoutManager(this));
        rvLessonContent.setAdapter(new LessonTextAdapter(new ArrayList<>()));
        rvLessonRecap.setAdapter(new LessonTextAdapter(new ArrayList<>()));

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(langAdapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = languages[position];
                loadChapterTitles();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedChapter = (String) parent.getItemAtPosition(pos);
                loadLessonTitles(selectedChapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedLesson = (String) parent.getItemAtPosition(pos);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnBack.setOnClickListener(v -> finish());

        btnSelect.setOnClickListener(v -> {
            if(selectedChapter.isEmpty() || selectedLesson.isEmpty()){
                Toast.makeText(ViewChapterContentActivity.this, "Please select both chapter and lesson titles", Toast.LENGTH_SHORT).show();
                return;
            }
            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterTitle", selectedChapter)
                    .whereEqualTo("lessonTitle", selectedLesson)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if(!querySnapshot.isEmpty()){
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            currentLessonContent = doc.getString("lessonContent") != null ? doc.getString("lessonContent") : "";
                            currentLessonRecap = doc.getString("lessonRecap") != null ? doc.getString("lessonRecap") : "";
                            contentList = currentLessonContent.isEmpty() ? new ArrayList<>() : Arrays.asList(currentLessonContent.split("\n"));
                            recapList = currentLessonRecap.isEmpty() ? new ArrayList<>() : Arrays.asList(currentLessonRecap.split("\\. "));
                            readAdapter = new LessonTextAdapter(contentList);
                            rvLessonContent.setAdapter(readAdapter);
                            rvLessonRecap.setAdapter(new LessonTextAdapter(recapList));
                            if(doc.contains("lastEdit") && doc.getTimestamp("lastEdit") != null){
                                Date date = doc.getTimestamp("lastEdit").toDate();
                                String formattedDate = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(date);
                                etLastEdited.setText("Last Edited: " + formattedDate);
                            } else {
                                etLastEdited.setText("Last Edited: N/A");
                            }
                            isEditMode = false;
                        } else {
                            Toast.makeText(ViewChapterContentActivity.this, "No content found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnEdit.setOnClickListener(v -> {
            if(!isEditMode){
                editContentAdapter = new EditLessonTextAdapter(new ArrayList<>(contentList));
                editRecapAdapter = new EditLessonTextAdapter(new ArrayList<>(recapList));
                rvLessonContent.setAdapter(editContentAdapter);
                rvLessonRecap.setAdapter(editRecapAdapter);
                isEditMode = true;
            }
        });

        btnSave.setOnClickListener(v -> {
            if(isEditMode){
                List<String> updatedContentList = editContentAdapter.getItems();
                List<String> updatedRecapList = editRecapAdapter.getItems();
                String newContent = String.join("\n", updatedContentList);
                String newRecap = String.join(". ", updatedRecapList);
                firestore.collection("ChapterContent")
                        .document(selectedLanguage)
                        .collection("Chapters")
                        .whereEqualTo("chapterTitle", selectedChapter)
                        .whereEqualTo("lessonTitle", selectedLesson)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if(!querySnapshot.isEmpty()){
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                doc.getReference().update(
                                        "lessonContent", newContent,
                                        "lessonRecap", newRecap,
                                        "lastEdit", FieldValue.serverTimestamp()
                                ).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ViewChapterContentActivity.this, "Content updated successfully", Toast.LENGTH_SHORT).show();
                                    currentLessonContent = newContent;
                                    currentLessonRecap = newRecap;
                                    contentList = newContent.isEmpty() ? new ArrayList<>() : Arrays.asList(newContent.split("\n"));
                                    recapList = newRecap.isEmpty() ? new ArrayList<>() : Arrays.asList(newRecap.split("\\. "));
                                    readAdapter = new LessonTextAdapter(contentList);
                                    rvLessonContent.setAdapter(readAdapter);
                                    rvLessonRecap.setAdapter(new LessonTextAdapter(recapList));
                                    isEditMode = false;
                                }).addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Failed to update content", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(ViewChapterContentActivity.this, "No content found to update", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadChapterTitles() {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> chapterTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String chapterTitle = doc.getString("chapterTitle");
                        if (chapterTitle != null && !chapterTitles.contains(chapterTitle)) {
                            chapterTitles.add(chapterTitle);
                        }
                    }
                    if (!chapterTitles.isEmpty()) {
                        ArrayAdapter<String> chapterAdapter = new ArrayAdapter<>(ViewChapterContentActivity.this, android.R.layout.simple_spinner_dropdown_item, chapterTitles);
                        spinnerChapter.setAdapter(chapterAdapter);
                    } else {
                        Toast.makeText(ViewChapterContentActivity.this, "No chapters found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error loading chapters: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadLessonTitles(String chapterTitle) {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterTitle", chapterTitle)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> lessonTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String lessonTitle = doc.getString("lessonTitle");
                        if (lessonTitle != null && !lessonTitles.contains(lessonTitle)) {
                            lessonTitles.add(lessonTitle);
                        }
                    }
                    if (!lessonTitles.isEmpty()) {
                        ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(ViewChapterContentActivity.this, android.R.layout.simple_spinner_dropdown_item, lessonTitles);
                        spinnerLesson.setAdapter(lessonAdapter);
                    } else {
                        spinnerLesson.setAdapter(null);
                        Toast.makeText(ViewChapterContentActivity.this, "No lessons found for the selected chapter", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error loading lessons: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
