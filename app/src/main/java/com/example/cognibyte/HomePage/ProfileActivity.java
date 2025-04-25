package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etEmail, etUsername, etDateOfBirth;
    private Button btnSaveChanges;
    private ImageView btnEditEmail, btnEditUsername, btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnEditEmail = findViewById(R.id.btn_edit_email);
        btnEditUsername = findViewById(R.id.btn_edit_username);
        btnBack = findViewById(R.id.btn_back);

        makeImageViewClickable(btnEditEmail);
        makeImageViewClickable(btnEditUsername);
        makeImageViewClickable(btnBack);
        loadUserData();

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
            finish();
        });

        btnEditEmail.setOnClickListener(v -> {
            etEmail.setEnabled(true);
            etEmail.requestFocus();
        });

        btnEditUsername.setOnClickListener(v -> {
            etUsername.setEnabled(true);
            etUsername.requestFocus();
        });

        btnSaveChanges.setOnClickListener(v -> validateAndSave());
    }

    private void makeImageViewClickable(ImageView imageView) {
        imageView.setClickable(true);
        imageView.setFocusable(true);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            firestore.collection("Users").document(uid).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            etEmail.setText(document.getString("email"));
                            etUsername.setText(document.getString("username"));
                            etDateOfBirth.setText(document.getString("dateOfBirth"));

                            etEmail.setEnabled(false);
                            etUsername.setEnabled(false);
                        } else {
                            Toast.makeText(this, "No user data found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void validateAndSave() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Email and Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        firestore.collection("Users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(emailSnapshot -> {
                    boolean emailTaken = emailSnapshot.getDocuments().stream()
                            .anyMatch(doc -> !doc.getId().equals(uid));

                    if (emailTaken) {
                        Toast.makeText(this, "Email is already in use.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection("Users")
                            .whereEqualTo("username", username)
                            .get()
                            .addOnSuccessListener(usernameSnapshot -> {
                                boolean usernameTaken = usernameSnapshot.getDocuments().stream()
                                        .anyMatch(doc -> !doc.getId().equals(uid));

                                if (usernameTaken) {
                                    Toast.makeText(this, "Username is already taken.", Toast.LENGTH_SHORT).show();
                                } else {
                                    updateUserData(uid, email, username);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Username check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Email check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateUserData(String uid, String email, String username) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        updates.put("username", username);

        firestore.collection("Users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    etEmail.setEnabled(false);
                    etUsername.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
