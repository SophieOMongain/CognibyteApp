package com.example.cognibyte.Account;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Collections;
import models.Language;

public class LanguageSelectionActivity extends AppCompatActivity {

    private LinearLayout layoutJava, layoutJavaScript, layoutPython, layoutHtml;
    private EditText searchBar;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        layoutJava = findViewById(R.id.layout_java);
        layoutJavaScript = findViewById(R.id.layout_javascript);
        layoutPython = findViewById(R.id.layout_python);
        layoutHtml = findViewById(R.id.layout_html);
        searchBar = findViewById(R.id.search_bar);

        layoutJava.setOnClickListener(v -> selectLanguage("Java"));
        layoutJavaScript.setOnClickListener(v -> selectLanguage("Javascript"));
        layoutPython.setOnClickListener(v -> selectLanguage("Python"));
        layoutHtml.setOnClickListener(v -> selectLanguage("HTML"));

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLanguages(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void selectLanguage(String language) {
        String userId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        prefs.edit()
                .putString("selected_language", language)
                .apply();

        Language selectedLanguage = new Language(
                userId,
                Collections.singletonList(language),
                ""
        );

        firestore.collection("Languages")
                .document(userId)
                .set(selectedLanguage)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Language saved successfully!", Toast.LENGTH_SHORT).show();
                    navigateToSkillLevel();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save language: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToSkillLevel() {
        startActivity(new Intent(this, SkillLevelActivity.class));
        finish();
    }

    private void filterLanguages(String query) {
        if (query.isEmpty()) {
            layoutJava.setVisibility(View.VISIBLE);
            layoutJavaScript.setVisibility(View.VISIBLE);
            layoutPython.setVisibility(View.VISIBLE);
            layoutHtml.setVisibility(View.VISIBLE);
        } else {
            layoutJava.setVisibility(query.contains("java") ? View.VISIBLE : View.GONE);
            layoutJavaScript.setVisibility(query.contains("javascript") ? View.VISIBLE : View.GONE);
            layoutPython.setVisibility(query.contains("python") ? View.VISIBLE : View.GONE);
            layoutHtml.setVisibility(query.contains("html") ? View.VISIBLE : View.GONE);
        }
    }
}