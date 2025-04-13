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
    private Spinner spinnerLanguage;
    private EditText etChapterNumber, etLessonNumber, etLastEdited;
    private Button btnSelect, btnEdit, btnSave;
    private RecyclerView rvLessonContent, rvLessonRecap;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "Select Language";
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
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonNumber = findViewById(R.id.etLessonNumber);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedLanguage = languages[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        btnBack.setOnClickListener(v -> finish());
        btnSelect.setOnClickListener(v -> {
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            if(chapterStr.isEmpty() || lessonStr.isEmpty()){
                Toast.makeText(ViewChapterContentActivity.this, "Please enter both chapter and lesson numbers", Toast.LENGTH_SHORT).show();
                return;
            }
            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
            } catch (NumberFormatException e) {
                Toast.makeText(ViewChapterContentActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return;
            }
            firestore.collection("ChapterContent")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", chapterNumber)
                    .whereEqualTo("lessonNumber", lessonNumber)
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
                int chapterNumber, lessonNumber;
                try {
                    chapterNumber = Integer.parseInt(etChapterNumber.getText().toString().trim());
                    lessonNumber = Integer.parseInt(etLessonNumber.getText().toString().trim());
                } catch (NumberFormatException e) {
                    Toast.makeText(ViewChapterContentActivity.this, "Invalid chapter or lesson number", Toast.LENGTH_SHORT).show();
                    return;
                }
                firestore.collection("ChapterContent")
                        .document(selectedLanguage)
                        .collection("Chapters")
                        .whereEqualTo("chapterNumber", chapterNumber)
                        .whereEqualTo("lessonNumber", lessonNumber)
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
}
