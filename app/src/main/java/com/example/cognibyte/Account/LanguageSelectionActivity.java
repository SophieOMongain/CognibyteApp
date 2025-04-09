package com.example.cognibyte.Account;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import models.Language;

public class LanguageSelectionActivity extends AppCompatActivity {

    private Button btnJava, btnJavaScript, btnPython, btnHtml;
    private EditText searchBar;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnJava = findViewById(R.id.btn_java);
        btnJavaScript = findViewById(R.id.btn_javascript);
        btnPython = findViewById(R.id.btn_python);
        btnHtml = findViewById(R.id.btn_html);
        searchBar = findViewById(R.id.search_bar);

        btnJava.setOnClickListener(v -> selectLanguage("Java"));
        btnJavaScript.setOnClickListener(v -> selectLanguage("JavaScript"));
        btnPython.setOnClickListener(v -> selectLanguage("Python"));
        btnHtml.setOnClickListener(v -> selectLanguage("HTML"));
    }

    private void selectLanguage(String language) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_language", language);
        editor.apply();

        Language selectedLanguage = new Language(userId, language, "");

        firestore.collection("Languages").document(userId)
                .set(selectedLanguage)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Language saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateToSkillLevel();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save language: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToSkillLevel() {
        Intent intent = new Intent(LanguageSelectionActivity.this, SkillLevelActivity.class);
        startActivity(intent);
        finish();
    }
}
