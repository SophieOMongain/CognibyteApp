package com.example.cognibyte.HomePage.Language;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ChangeLanguageActivity extends AppCompatActivity {
    private TextView currentLanguageTextView;
    private ImageView currentLanguageLogoImageView;
    private Spinner languageSpinner;
    private Button changeLanguageButton;
    private FirebaseFirestore db;
    private String[] allLanguages = {"Java", "Python", "HTML", "JavaScript"};
    private String currentLanguage;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_language);
        db = FirebaseFirestore.getInstance();
        currentLanguageTextView = findViewById(R.id.currentLanguageTextView);
        currentLanguageLogoImageView = findViewById(R.id.currentLanguageLogoImageView);
        languageSpinner = findViewById(R.id.languageSpinner);
        changeLanguageButton = findViewById(R.id.changeLanguageButton);
        if (getIntent() != null) {
            currentLanguage = getIntent().getStringExtra("currentLanguage");
            userUid = getIntent().getStringExtra("uid");
        }
        if (currentLanguage == null) {
            currentLanguage = "Java";
        }
        currentLanguageTextView.setText("Current Language: " + currentLanguage);
        setLanguageLogo(currentLanguage);
        List<String> otherLanguages = new ArrayList<>();
        for (String lang : allLanguages) {
            if (!lang.equalsIgnoreCase(currentLanguage)) {
                otherLanguages.add(lang);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, otherLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        changeLanguageButton.setOnClickListener(view -> {
            String newLanguage = languageSpinner.getSelectedItem().toString();
            new AlertDialog.Builder(ChangeLanguageActivity.this)
                    .setTitle("Confirm Language Change")
                    .setMessage("Are you sure? Once you change language you can't undo it and you will be starting again from the beginning.")
                    .setPositiveButton("Yes", (dialog, which) -> updateLanguageInDatabase(newLanguage))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
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

    private void updateLanguageInDatabase(String newLanguage) {
        if (userUid == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("languages")
                .document(userUid)
                .update("language", newLanguage)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ChangeLanguageActivity.this, "Language updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChangeLanguageActivity.this, "Failed to update language", Toast.LENGTH_SHORT).show();
                });
    }
}