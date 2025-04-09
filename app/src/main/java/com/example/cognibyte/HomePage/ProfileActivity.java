package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etDateOfBirth;
    private Button btnSaveChanges;
    private ImageButton btnEditName, btnEditEmail, btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnEditName = findViewById(R.id.btn_edit_name);
        btnEditEmail = findViewById(R.id.btn_edit_email);
        btnBack = findViewById(R.id.btn_back);

        loadUserData();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        btnEditName.setOnClickListener(v -> {
            etName.setEnabled(true);
            etName.requestFocus();
        });

        btnEditEmail.setOnClickListener(v -> {
            etEmail.setEnabled(true);
            etEmail.requestFocus();
        });

        btnSaveChanges.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            firestore.collection("Users").document(uid).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            etName.setText(document.getString("name"));
                            etEmail.setText(document.getString("email"));
                            etDateOfBirth.setText(document.getString("dateOfBirth"));

                            etName.setEnabled(false);
                            etEmail.setEnabled(false);
                        } else {
                            Toast.makeText(ProfileActivity.this, "No user data found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No authenticated user.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("name", name);
            userMap.put("email", email);

            firestore.collection("Users").document(uid).update(userMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        etName.setEnabled(false);
                        etEmail.setEnabled(false);
                    })
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No authenticated user.", Toast.LENGTH_SHORT).show();
        }
    }
}
