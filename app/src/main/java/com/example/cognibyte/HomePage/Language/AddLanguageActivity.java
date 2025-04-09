package com.example.cognibyte.HomePage.Language;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import models.Language;

public class AddLanguageActivity extends AppCompatActivity {
    private TextView currentLanguageTextView;
    private Spinner languageSpinner;
    private ImageView currentLanguageLogoImageView;
    private Button addLanguageButton;
    private FirebaseFirestore db;
    private String[] allLanguages = {"Java", "Python", "HTML", "JavaScript"};
    private String userUid;
    private String currentLanguage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_language);

        db = FirebaseFirestore.getInstance();
        currentLanguageTextView = findViewById(R.id.currentLanguageTextView);
        currentLanguageLogoImageView = findViewById(R.id.currentLanguageLogoImageView);
        languageSpinner = findViewById(R.id.languageSpinner);
        addLanguageButton = findViewById(R.id.addLanguageButton);

        if (getIntent() != null) {
            currentLanguage = getIntent().getStringExtra("currentLanguage");
            userUid = getIntent().getStringExtra("uid");
        }
        if (currentLanguage == null) {
            currentLanguage = "Java";
        }
        currentLanguageTextView.setText("Current Language: " + currentLanguage);
        setLanguageLogo(currentLanguage);

        List<String> languagesList = new ArrayList<>();
        for (String lang : allLanguages) {
            languagesList.add(lang);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languagesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        addLanguageButton.setOnClickListener(view -> {
            String newLanguage = languageSpinner.getSelectedItem().toString();
            addLanguageToDatabase(newLanguage);
        });
    }

    private void setLanguageLogo(String language) {
        int logoResId = R.drawable.java_icon;
        switch (language.toLowerCase()) {
            case "java":
                logoResId = R.drawable.java_icon;
                break;
            case "python":
                logoResId = R.drawable.python_icon;
                break;
            case "html":
                logoResId = R.drawable.html_icon;
                break;
            case "javascript":
                logoResId = R.drawable.javascript_icon;
                break;
        }
        currentLanguageLogoImageView.setImageResource(logoResId);
    }

    private void addLanguageToDatabase(String newLanguage) {
        if (userUid == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Language language = new Language(userUid, newLanguage, "beginner");
        db.collection("languages")
                .add(language)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddLanguageActivity.this, "Language added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddLanguageActivity.this, "Failed to add language", Toast.LENGTH_SHORT).show();
                });
    }
}
