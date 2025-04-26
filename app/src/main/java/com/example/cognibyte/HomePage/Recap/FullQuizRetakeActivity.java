package com.example.cognibyte.HomePage.Recap;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.Adapter.FullQuizAdapter;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;
import models.QuestionItem;
import models.RetakeAttempt;

public class FullQuizRetakeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnSubmitQuiz;
    private ImageView btnBackQuiz;
    private TextView timerTextView;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String selectedLanguage, selectedSkill, selectedChapter, selectedLesson;
    private List<QuestionItem> questionsList = new ArrayList<>();
    private FullQuizAdapter adapter;

    private CountDownTimer countDownTimer;
    private static final long TOTAL_TIME = 10 * 60 * 1000; // 10 minutes in milliseconds
    private long startTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_quiz_retake);

        recyclerView = findViewById(R.id.recyclerViewFullQuiz);
        btnSubmitQuiz = findViewById(R.id.btnSubmitQuiz);
        btnBackQuiz = findViewById(R.id.btnBackQuiz);
        timerTextView = findViewById(R.id.timerTextView);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        selectedLanguage = getIntent().getStringExtra("language");
        selectedSkill = getIntent().getStringExtra("skill");
        selectedChapter = getIntent().getStringExtra("chapter");
        selectedLesson = getIntent().getStringExtra("lesson");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FullQuizAdapter(questionsList);
        recyclerView.setAdapter(adapter);

        loadFullQuiz();

        btnSubmitQuiz.setOnClickListener(v -> {
            cancelTimer();
            calculateScore();
        });
        btnBackQuiz.setOnClickListener(v -> showExitConfirmation());
    }

    private void loadFullQuiz() {
        firestore.collection("QuizContent")
                .document(selectedLanguage)
                .collection("Quizzes")
                .whereEqualTo("chapterTitle", selectedChapter)
                .whereEqualTo("lessonTitle", selectedLesson)
                .whereEqualTo("skillLevel", selectedSkill)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot quizDoc = qs.getDocuments().get(0);
                        DocumentReference quizRef = quizDoc.getReference();

                        quizRef.collection("Questions")
                                .orderBy(FieldPath.documentId())
                                .get()
                                .addOnSuccessListener(q2 -> {
                                    if (!q2.isEmpty()) {
                                        questionsList.clear();
                                        for (DocumentSnapshot d : q2.getDocuments()) {
                                            String question = d.getString("question");
                                            @SuppressWarnings("unchecked")
                                            List<String> options = (List<String>) d.get("options");
                                            String answer = d.getString("answer");

                                            if (question != null && options != null && answer != null) {
                                                questionsList.add(new QuestionItem(question, options, answer));
                                            }
                                        }
                                        Collections.shuffle(questionsList);
                                        adapter.notifyDataSetChanged();
                                        startTimer();
                                    } else {
                                        loadFromQuestionsArray(quizDoc);
                                    }
                                })
                                .addOnFailureListener(e -> loadFromQuestionsArray(quizDoc));
                    } else {
                        Toast.makeText(this, "Quiz not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadFromQuestionsArray(DocumentSnapshot quizDoc) {
        List<Map<String, Object>> rawQs = (List<Map<String, Object>>) quizDoc.get("questions");
        if (rawQs != null && !rawQs.isEmpty()) {
            questionsList.clear();
            for (Map<String, Object> q : rawQs) {
                String question = (String) q.get("question");
                @SuppressWarnings("unchecked")
                List<String> options = (List<String>) q.get("options");
                String answer = (String) q.get("answer");

                if (question != null && options != null && answer != null) {
                    questionsList.add(new QuestionItem(question, options, answer));
                }
            }
            Collections.shuffle(questionsList);
            adapter.notifyDataSetChanged();
            startTimer();
        } else {
            Toast.makeText(this, "Quiz has no questions!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        startTimeMillis = System.currentTimeMillis();
        countDownTimer = new CountDownTimer(TOTAL_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timerTextView.setText(time);
            }

            @Override
            public void onFinish() {
                Toast.makeText(FullQuizRetakeActivity.this, "Time's up! Submitting quiz.", Toast.LENGTH_SHORT).show();
                calculateScore();
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void calculateScore() {
        int score = 0;
        for (QuestionItem item : questionsList) {
            if (item.getSelectedAnswer() != null && item.getSelectedAnswer().equals(item.getCorrectAnswer())) {
                score++;
            }
        }
        showResultDialog(score, questionsList.size());
    }

    private void showResultDialog(int score, int total) {
        long endTimeMillis = System.currentTimeMillis();
        long timeTakenMillis = endTimeMillis - startTimeMillis;
        String formattedTime = formatTime(timeTakenMillis);

        saveRetakeAttempt(score, total, formattedTime);

        new AlertDialog.Builder(this)
                .setTitle("Quiz Completed!")
                .setMessage("Your Score: " + score + "/" + total + "\nTime Taken: " + formattedTime)
                .setCancelable(false)
                .setPositiveButton("Try Again", (dialog, which) -> resetQuiz())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    private String formatTime(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void saveRetakeAttempt(int score, int totalQuestions, String timeTaken) {
        String userId = auth.getCurrentUser().getUid();

        RetakeAttempt attempt = new RetakeAttempt(
                score,
                totalQuestions,
                timeTaken,
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()),
                selectedLanguage,
                selectedChapter,
                selectedLesson
        );

        firestore.collection("UserFullQuizPractice")
                .document(userId)
                .collection("RetakeAttempts")
                .add(attempt)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Attempt saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save attempt.", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetQuiz() {
        for (QuestionItem item : questionsList) {
            item.setSelectedAnswer(null);
        }
        Collections.shuffle(questionsList);
        adapter.notifyDataSetChanged();
        startTimer();
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Quiz?")
                .setMessage("Are you sure you want to leave the quiz? All your progress will be lost.")
                .setPositiveButton("Yes, Exit", (dialog, which) -> finish())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
        showExitConfirmation();
    }
}
