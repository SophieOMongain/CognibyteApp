package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Account.LoginActivity;
import com.example.cognibyte.Account.MainActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private EditText etUsername, etDateJoined, etLanguage;
    private Button btnViewCourses, btnDeleteAccount, btnSignOut;
    private ImageView btnBack;
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
        btnBack = findViewById(R.id.btnBack);

        loadUserData();

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, HomeActivity.class));
            finish();
        });

        btnViewCourses.setOnClickListener(v ->
                Toast.makeText(this, "View Courses clicked!", Toast.LENGTH_SHORT).show()
        );

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
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
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

            Date creationDate = new Date(user.getMetadata().getCreationTimestamp());
            String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(creationDate);
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
                .setPositiveButton("Yes", (dialog, which) -> showPasswordDialog())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showPasswordDialog() {
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setView(passwordInput)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateAndDelete(password);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void reauthenticateAndDelete(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No authenticated user", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> deleteUserDataAndAccount(user))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void deleteUserDataAndAccount(FirebaseUser user) {
        String uid = user.getUid();

        firestore.collection("UserQuiz").document(uid).collection("Attempts").get()
                .addOnSuccessListener(attempts -> {
                    for (DocumentSnapshot doc : attempts.getDocuments()) {
                        doc.getReference().delete();
                    }
                    firestore.collection("UserQuiz").document(uid).delete();
                });

        firestore.collection("UserProgress").document(uid).collection("Chapters").get()
                .addOnSuccessListener(chapters -> {
                    for (DocumentSnapshot doc : chapters.getDocuments()) {
                        doc.getReference().delete();
                    }
                    firestore.collection("UserProgress").document(uid).delete();
                });

        firestore.collection("Users").document(uid).delete();
        firestore.collection("Languages").document(uid).delete();
        firestore.collection("QuizResults").document(uid).delete();
        firestore.collection("RetakeAttempts").document(uid).delete();
        firestore.collection("UserFullQuizPractice").document(uid).delete();

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete user: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
