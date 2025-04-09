package com.example.cognibyte.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Account.MainActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private Button btnGenerateLessons, btnGenerateQuizzes, btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        btnGenerateLessons = findViewById(R.id.btnGenerateLessons);
        btnGenerateQuizzes = findViewById(R.id.btnGenerateQuizzes);
        btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnGenerateLessons.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, GenerateChaptersActivity.class);
            startActivity(intent);
        });

        btnGenerateQuizzes.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, GenerateQuizActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        checkIfAdmin();
    }

    private void checkIfAdmin() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("Users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && !"admin".equals(documentSnapshot.getString("role"))) {
                        Toast.makeText(this, "Access denied. Admins only.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking admin status.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
