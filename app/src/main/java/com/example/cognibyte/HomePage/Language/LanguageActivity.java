package com.example.cognibyte.HomePage.Language;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LanguageActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Button btnSaveChanges;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        Button btnChangeLanguage = findViewById(R.id.btnChangeLanguage);
        Button btnAddLanguage = findViewById(R.id.btnAddLanguage);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnChangeLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(LanguageActivity.this, ChangeLanguageActivity.class);
            startActivity(intent);
        });

        btnAddLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(LanguageActivity.this, AddLanguageActivity.class);
            startActivity(intent);
        });

        btnSaveChanges.setOnClickListener(v -> saveLanguageChanges());

        btnBack.setOnClickListener(v -> finish());
    }

    private void saveLanguageChanges() {
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Users").document(userId)
                .update("preferredLanguage", "New Selected Language")
                .addOnSuccessListener(aVoid -> Toast.makeText(LanguageActivity.this, "Language updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(LanguageActivity.this, "Failed to update language", Toast.LENGTH_SHORT).show());
    }
}
