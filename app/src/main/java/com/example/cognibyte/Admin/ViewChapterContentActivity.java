package com.example.cognibyte.Admin;

import android.os.Bundle;
import android.text.InputType;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewChapterContentActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage;
    private EditText etChapterNumber, etLessonNumber, etLessonContent;
    private Button btnSelect, btnEdit, btnSave;
    private TextView tvLastEdit;
    private FirebaseFirestore firestore;
    private String selectedLanguage = "Select Language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_chapter_content);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etChapterNumber = findViewById(R.id.etChapterNumber);
        etLessonNumber = findViewById(R.id.etLessonNumber);
        btnSelect = findViewById(R.id.btnSelect);
        etLessonContent = findViewById(R.id.etLessonContent);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        tvLastEdit = findViewById(R.id.LastEdited);
        firestore = FirebaseFirestore.getInstance();

        String[] languages = {"Java", "Javascript", "HTML", "Python"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
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
            if(chapterNumber < 1 || chapterNumber > 10 || lessonNumber < 1 || lessonNumber > 10){
                Toast.makeText(ViewChapterContentActivity.this, "Numbers must be between 1 and 10", Toast.LENGTH_SHORT).show();
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
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            String lessonContent = document.getString("lessonContent");
                            etLessonContent.setText(lessonContent);
                            etLessonContent.setEnabled(false);
                            if(document.contains("lastEdit") && document.getTimestamp("lastEdit") != null){
                                Date date = document.getTimestamp("lastEdit").toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                                tvLastEdit.setText("Last Edited: " + sdf.format(date));
                            } else {
                                tvLastEdit.setText("Last Edited: N/A");
                            }
                        } else {
                            Toast.makeText(ViewChapterContentActivity.this, "No content found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnEdit.setOnClickListener(v -> {
            etLessonContent.setEnabled(true);
            etLessonContent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        });

        btnSave.setOnClickListener(v -> {
            String newContent = etLessonContent.getText().toString().trim();
            if(newContent.isEmpty()){
                Toast.makeText(ViewChapterContentActivity.this, "Content cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String chapterStr = etChapterNumber.getText().toString().trim();
            String lessonStr = etLessonNumber.getText().toString().trim();
            int chapterNumber, lessonNumber;
            try {
                chapterNumber = Integer.parseInt(chapterStr);
                lessonNumber = Integer.parseInt(lessonStr);
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
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            document.getReference().update("lessonContent", newContent, "lastEdit", com.google.firebase.firestore.FieldValue.serverTimestamp())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ViewChapterContentActivity.this, "Content updated", Toast.LENGTH_SHORT).show();
                                        etLessonContent.setEnabled(false);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error updating content", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(ViewChapterContentActivity.this, "No content found to update", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ViewChapterContentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
