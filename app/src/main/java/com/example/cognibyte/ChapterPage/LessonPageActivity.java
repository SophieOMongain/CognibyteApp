package com.example.cognibyte.ChapterPage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Account.LoginActivity;
import com.example.cognibyte.ChapterPage.Quiz.QuizActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LessonPageActivity extends AppCompatActivity {

    private static final String TAG = "LessonPageActivity";

    private ProgressBar progressBar;
    private TextView tvLessonTitle, tvLessonContent;
    private ScrollView scrollContainer;
    private Button btnRetry, btnStartQuiz, btnReturnToLessons, btnHome;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    private int chapterNumber;
    private int lessonNumber;
    private String language, skillLevel;
    private String lessonTitle, lessonContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_page);

        progressBar = findViewById(R.id.progressBar);
        tvLessonTitle = findViewById(R.id.tvLessonTitle);
        tvLessonContent = findViewById(R.id.tvLessonContent);
        scrollContainer = findViewById(R.id.scrollContainer);
        btnRetry = findViewById(R.id.btnRetry);
        btnStartQuiz = findViewById(R.id.btnStartQuiz);
        btnReturnToLessons = findViewById(R.id.btnReturnToLessons);
        btnHome = findViewById(R.id.btnHome);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Intent in = getIntent();
        lessonTitle = in.getStringExtra("lessonTitle");
        chapterNumber = in.getIntExtra("chapterNumber", -1);
        language = in.getStringExtra("language");
        skillLevel = in.getStringExtra("skillLevel");

        if (lessonTitle == null || chapterNumber < 0 || language == null || skillLevel == null) {
            Toast.makeText(this, "Invalid lesson parameters.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvLessonContent.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        btnRetry.setOnClickListener(v -> fetchLesson());
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        btnReturnToLessons.setOnClickListener(v -> finish());
        btnStartQuiz.setOnClickListener(v -> {
            if (lessonContent == null || lessonContent.isEmpty()) {
                Toast.makeText(this, "Cannot start quiz; content missing.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("chapterNumber", chapterNumber);
            intent.putExtra("lessonNumber", lessonNumber);
            intent.putExtra("language", language);
            intent.putExtra("skillLevel", skillLevel);
            intent.putExtra("lessonTitle", lessonTitle);
            startActivity(intent);
        });

        btnStartQuiz.setVisibility(View.GONE);
        fetchLesson();
    }

    private void fetchLesson() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("lessonTitle", lessonTitle)
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    progressBar.setVisibility(View.GONE);
                    if (qs.isEmpty()) {
                        Toast.makeText(this, "Lesson not found. Please contact admin.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    DocumentSnapshot doc = qs.getDocuments().get(0);

                    Long ln = doc.getLong("lessonNumber");
                    lessonNumber = (ln != null ? ln.intValue() :  -1);

                    String fetchedTitle   = doc.getString("lessonTitle");
                    String fetchedContent = doc.getString("lessonContent");
                    if (fetchedContent == null) {
                        Toast.makeText(this, "Lesson content incomplete.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    lessonTitle = (fetchedTitle != null ? fetchedTitle : lessonTitle);
                    lessonContent = fetchedContent;
                    tvLessonTitle.setText(lessonTitle);
                    tvLessonContent.setText(lessonContent);
                    btnStartQuiz .setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching lesson", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
