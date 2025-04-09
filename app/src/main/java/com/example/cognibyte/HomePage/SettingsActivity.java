package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.Account.LoginActivity;
import com.example.cognibyte.Account.MainActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private EditText etUsername, etDateJoined, etLanguage;
    private Button btnViewCourses, btnDeleteAccount, btnSignOut;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.et_username);
        etDateJoined = findViewById(R.id.et_date_joined);
        etLanguage = findViewById(R.id.et_language);
        btnViewCourses = findViewById(R.id.btn_view_courses);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnBack = findViewById(R.id.btn_back);

        loadUserData();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        btnViewCourses.setOnClickListener(v -> {
            Toast.makeText(this, "View Courses clicked!", Toast.LENGTH_SHORT).show();
        });

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            firestore.collection("Users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etUsername.setText(documentSnapshot.getString("username"));
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            Date creationDate = new Date(user.getMetadata().getCreationTimestamp());
            String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(creationDate);
            etDateJoined.setText(formattedDate);

            etLanguage.setText("English");
        } else {
            Toast.makeText(this, "No authenticated user.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteAccount() {
        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("Users").document(uid).delete()
                .addOnSuccessListener(aVoid -> {
                    mAuth.getCurrentUser().delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
