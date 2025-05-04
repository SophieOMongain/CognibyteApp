package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.Recap.RecapActivity;
import com.example.cognibyte.HomePage.Stats.LessonStatsActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        tvWelcome = findViewById(R.id.tv_welcome);

        loadUserData();

        setupGridItemListeners();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            firestore.collection("Users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String username = document.getString("username");
                            if (username != null) {
                                tvWelcome.setText("Welcome, " + username + "!");
                            } else {
                                tvWelcome.setText("Welcome!");
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(HomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            tvWelcome.setText("Welcome!");
        }
    }

    private void setupGridItemListeners() {
        findViewById(R.id.profile_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.settings_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.stats_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LessonStatsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.language_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ManageLanguageActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.lesson_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChapterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.recap_section).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RecapActivity.class);
            startActivity(intent);
        });
    }
}