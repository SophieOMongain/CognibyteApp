package com.example.cognibyte.ChapterPage.CodeQuiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Client.OpenAIClient;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.HomePage.ProfileActivity;
import com.example.cognibyte.HomePage.SettingsActivity;
import com.example.cognibyte.ChapterPage.Quiz.QuizActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class WeeklyQuizActivity extends AppCompatActivity {
    private Button btnStartWeeklyQuiz, btnViewWeeklyRecap;
    private ImageView btnHome, btnProfile, btnSettings, btnCodeQuiz;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_quiz);

        btnStartWeeklyQuiz = findViewById(R.id.btnStartWeeklyQuiz);
        btnViewWeeklyRecap = findViewById(R.id.btnViewWeeklyRecap);

        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnCodeQuiz = findViewById(R.id.btnCodeQuiz);

        btnStartWeeklyQuiz.setOnClickListener(v -> {
            generateWeeklyQuiz();
        });
        btnViewWeeklyRecap.setOnClickListener(v -> startActivity(new Intent(this, WeeklyRecapActivity.class)));

        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        btnSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));
        btnCodeQuiz.setOnClickListener(v -> navigateTo(CodeQuizActivity.class));
    }

    private void navigateTo(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void generateWeeklyQuiz() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to start the quiz.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        String language = prefs.getString("selected_language", "Java");

        firestore.collection("LessonProgress").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    StringBuilder lessonsContent = new StringBuilder();

                    for (int i = 1; i <= 4; i++) {
                        Boolean completed = documentSnapshot.getBoolean("Lesson" + i);
                        if (completed != null && completed) {
                            lessonsContent.append("Lesson ").append(i).append("\n");
                            lessonsContent.append("Add lesson content here for Lesson ").append(i).append("\n\n");
                        }
                    }

                    if (lessonsContent.length() == 0) {
                        Toast.makeText(this, "No lessons completed this week!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    OpenAIClient.generateCodingQuiz(language, lessonsContent.toString(), new OpenAIClient.CompletionCallback() {
                        @Override
                        public void onSuccess(String quizJson) {
                            Intent intent = new Intent(WeeklyQuizActivity.this, QuizActivity.class);
                            intent.putExtra("quizJson", quizJson);
                            startActivity(intent);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(WeeklyQuizActivity.this, "Failed to generate quiz: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching lesson progress: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
