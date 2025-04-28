package com.example.cognibyte.HomePage;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvEmail, tvDateOfBirth;
    private EditText etUsername;
    private ImageView btnEditUsername, btnBack;
    private Button btnSaveChanges;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        tvEmail = findViewById(R.id.tv_email);
        etUsername = findViewById(R.id.et_username);
        tvDateOfBirth = findViewById(R.id.tv_date_of_birth);
        btnEditUsername = findViewById(R.id.btn_edit_username);
        btnBack = findViewById(R.id.btn_back);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        etUsername.setEnabled(false);

        btnBack.setOnClickListener(v -> finish());
        btnEditUsername.setOnClickListener(v -> {
            etUsername.setEnabled(true);
            etUsername.requestFocus();
        });
        btnSaveChanges.setOnClickListener(v -> validateAndSave());

        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = currentUser.getUid();
        firestore.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "User record not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    tvEmail.setText(doc.getString("email"));
                    etUsername.setText(doc.getString("username"));
                    tvDateOfBirth.setText(doc.getString("dateOfBirth"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void validateAndSave() {
        String newUsername = etUsername.getText().toString().trim();
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        updateUsernameOnly(newUsername);
    }

    private void updateUsernameOnly(String username) {
        String uid = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);

        firestore.collection("Users").document(uid)
                .update(updates)
                .addOnSuccessListener(__ -> {
                    Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                    etUsername.setEnabled(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
