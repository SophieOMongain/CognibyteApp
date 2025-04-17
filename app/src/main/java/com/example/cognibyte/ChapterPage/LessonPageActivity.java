package com.example.cognibyte.ChapterPage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.Account.LoginActivity;
import com.example.cognibyte.ChapterPage.Quiz.QuizActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class LessonPageActivity extends AppCompatActivity {

    private Button btnRetry, btnHome, btnStartQuiz, btnReturnToLessons;
    private ProgressBar progressBar;
    private TextView tvLessonTitle, tvLessonContent;
    private String lessonContent;
    private String lessonTitle;
    private int chapterNumber;
    private String language, skillLevel;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private static final String TAG = "LessonPageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_page);

        btnRetry = findViewById(R.id.btnRetry);
        btnHome = findViewById(R.id.btnHome);
        btnStartQuiz = findViewById(R.id.btnStartQuiz);
        btnReturnToLessons = findViewById(R.id.btnReturnToLessons);
        progressBar = findViewById(R.id.progressBar);
        tvLessonTitle = findViewById(R.id.tvLessonTitle);
        tvLessonContent = findViewById(R.id.tvLessonContent);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        chapterNumber = getIntent().getIntExtra("chapterNumber", -1);
        language = getIntent().getStringExtra("language");
        lessonTitle = getIntent().getStringExtra("lessonTitle");
        skillLevel = getIntent().getStringExtra("skillLevel");

        if (lessonTitle == null || lessonTitle.trim().isEmpty() || chapterNumber == -1 || language == null || skillLevel == null) {
            Toast.makeText(this, "Invalid lesson or chapter parameters.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fetchLessonFromFirestore();

        btnRetry.setOnClickListener(v -> fetchLessonFromFirestore());
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(LessonPageActivity.this, HomeActivity.class));
            finish();
        });
        btnReturnToLessons.setOnClickListener(v -> markLessonCompleted());
        btnStartQuiz.setOnClickListener(v -> {
            if (lessonContent == null || lessonContent.trim().isEmpty()) {
                Toast.makeText(LessonPageActivity.this, "Cannot start quiz. Lesson content is missing.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(LessonPageActivity.this, QuizActivity.class);
            intent.putExtra("lessonTitle", lessonTitle);
            intent.putExtra("chapterNumber", chapterNumber);
            intent.putExtra("lessonContent", lessonContent);
            intent.putExtra("language", language);
            intent.putExtra("skillLevel", skillLevel);
            startActivity(intent);
        });
    }

    private void fetchLessonFromFirestore() {
        if (language == null || lessonTitle == null || chapterNumber == -1) {
            Toast.makeText(this, "Required lesson parameters missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(android.view.View.VISIBLE);
        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("lessonTitle", lessonTitle)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        lessonTitle = document.getString("lessonTitle");
                        lessonContent = document.getString("lessonContent");
                        if (lessonTitle == null || lessonContent == null) {
                            Toast.makeText(this, "Lesson data is incomplete.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        tvLessonTitle.setText(lessonTitle);
                        tvLessonContent.setText(lessonContent);
                        btnStartQuiz.setVisibility(android.view.View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Lesson not found. Please contact the admin.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Error fetching lesson: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markLessonCompleted() {
        String userId = mAuth.getCurrentUser().getUid();
        String docId = "Chapter" + chapterNumber + "_Lesson" + lessonTitle.replaceAll("\\s+", "");
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("chapterNumber", chapterNumber);
        progressData.put("lessonTitle", lessonTitle);
        progressData.put("progress", true);
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .document(docId)
                .set(progressData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LessonPageActivity.this, "Lesson marked as completed!", Toast.LENGTH_SHORT).show();
                    navigateBackToLessons();
                })
                .addOnFailureListener(e -> Toast.makeText(LessonPageActivity.this, "Failed to update lesson progress: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void navigateBackToLessons() {
        Intent intent = new Intent(LessonPageActivity.this, LessonActivity.class);
        intent.putExtra("chapterNumber", chapterNumber);
        intent.putExtra("language", language);
        startActivity(intent);
        finish();
    }
}
