package com.example.cognibyte.ChapterPage.Quiz;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.ChapterPage.LessonActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import models.Question;
import models.Quiz;
import models.QuizAttempt;
import models.UserQuizDatabaseHelper;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestion, tvCorrectAnswer, tvExplanation, tvWrongQuestions;
    private RadioGroup rgOptions;
    private Button btnSubmit, btnNext, btnBack, btnReturnToLessons, btnCompleteLesson, btnRetryQuiz;
    private int currentQuestionIndex = 0, score = 0;
    private List<Question> quizQuestions;
    private List<Question> wrongQuestions = new ArrayList<>();
    private int lessonNumber, chapterNumber;
    private String language, skillLevel;
    private FirebaseFirestore firestore;
    private FirebaseUser user;
    private String correctAnswer;
    private static final String TAG = "QuizActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        tvExplanation = findViewById(R.id.tvExplanation);
        tvWrongQuestions = findViewById(R.id.tvWrongQuestions);
        rgOptions = findViewById(R.id.rgOptions);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);
        btnReturnToLessons = findViewById(R.id.btnReturnToLessons);
        btnCompleteLesson = findViewById(R.id.btnCompleteLesson);
        btnRetryQuiz = findViewById(R.id.btnRetryQuiz);

        firestore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        language = getIntent().getStringExtra("language");
        skillLevel = getIntent().getStringExtra("skillLevel");
        lessonNumber = getIntent().getIntExtra("lessonNumber", -1);
        chapterNumber = getIntent().getIntExtra("chapterNumber", -1);
        if (lessonNumber == -1 || chapterNumber == -1 || language == null || skillLevel == null) {
            Log.e(TAG, "Missing required quiz data: language=" + language + ", skillLevel=" + skillLevel +
                    ", lessonNumber=" + lessonNumber + ", chapterNumber=" + chapterNumber);
            Toast.makeText(this, "Error: Missing quiz data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Starting quiz for " + language + " - Lesson " + lessonNumber +
                " Chapter " + chapterNumber + " (" + skillLevel + ")");

        btnBack.setOnClickListener(v -> finish());

        btnCompleteLesson.setOnClickListener(v -> completeLesson());

        btnReturnToLessons.setOnClickListener(v -> {
            Toast.makeText(QuizActivity.this, "Returned to lesson page", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(QuizActivity.this, LessonActivity.class);
            intent.putExtra("chapterNumber", chapterNumber);
            intent.putExtra("language", language);
            intent.putExtra("skillLevel", skillLevel);
            startActivity(intent);
            finish();
        });

        btnRetryQuiz.setOnClickListener(v -> {
            currentQuestionIndex = 0;
            score = 0;
            wrongQuestions.clear();
            tvWrongQuestions.setText("");
            rgOptions.setVisibility(View.VISIBLE);
            tvCorrectAnswer.setVisibility(View.GONE);
            tvExplanation.setVisibility(View.GONE);
            btnSubmit.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
            btnCompleteLesson.setVisibility(View.GONE);
            btnRetryQuiz.setVisibility(View.GONE);
            btnReturnToLessons.setVisibility(View.GONE);
            showNextQuestion();
        });

        loadQuiz();

        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnNext.setOnClickListener(v -> {
            currentQuestionIndex++;
            showNextQuestion();
        });
    }

    private void loadQuiz() {
        if (language == null || lessonNumber == -1 || skillLevel == null) {
            Log.e(TAG, "Cannot load quiz: Missing required data.");
            Toast.makeText(this, "Error: Missing required quiz details.", Toast.LENGTH_SHORT).show();
            return;
        }
        firestore.collection("QuizContent")
                .document(language)
                .collection("Quizzes")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("lessonNumber", lessonNumber)
                .whereEqualTo("skillLevel", skillLevel)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Quiz quiz = document.toObject(Quiz.class);
                        if (quiz != null && quiz.getQuestions() != null && quiz.getQuestions().size() >= 5) {
                            Collections.shuffle(quiz.getQuestions());
                            quizQuestions = quiz.getQuestions().subList(0, 5);
                            showNextQuestion();
                        } else {
                            Log.e(TAG, "Not enough questions available in Firestore.");
                            Toast.makeText(this, "Not enough questions available!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Quiz document not found in Firestore.");
                        Toast.makeText(this, "Quiz not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading quiz from Firestore: " + e.getMessage());
                    Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= quizQuestions.size()) {
            showFinalScore();
            return;
        }
        Question currentQuestion = quizQuestions.get(currentQuestionIndex);
        tvQuestion.setText(currentQuestion.getQuestion());
        List<String> options = currentQuestion.getOptions();
        rgOptions.removeAllViews();
        correctAnswer = currentQuestion.getAnswer();
        Collections.shuffle(options);
        for (String option : options) {
            RadioButton rb = new RadioButton(this);
            rb.setText(option);
            rb.setId(View.generateViewId());
            rgOptions.addView(rb);
        }
        tvCorrectAnswer.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
    }

    private void checkAnswer() {
        int selectedId = rgOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer!", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selected = findViewById(selectedId);
        String selectedAnswer = selected.getText().toString().trim().toLowerCase();
        String correctAnswerNormalized = correctAnswer.trim().toLowerCase();
        Log.d(TAG, "User selected: " + selectedAnswer);
        Log.d(TAG, "Correct answer: " + correctAnswerNormalized);
        if (selectedAnswer.equals(correctAnswerNormalized)) {
            score++;
        } else {
            wrongQuestions.add(quizQuestions.get(currentQuestionIndex));
        }
        tvCorrectAnswer.setText("Correct Answer: " + correctAnswer);
        tvCorrectAnswer.setVisibility(View.VISIBLE);
        tvExplanation.setText(quizQuestions.get(currentQuestionIndex).getExplanation());
        tvExplanation.setVisibility(View.VISIBLE);
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void showFinalScore() {
        String resultMessage = "Quiz Complete! Score: " + score + "/" + quizQuestions.size();
        tvQuestion.setText(resultMessage);
        rgOptions.setVisibility(View.GONE);
        tvCorrectAnswer.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.GONE);
        if (!wrongQuestions.isEmpty()) {
            StringBuilder wrongDisplay = new StringBuilder("You got these questions wrong:\n\n");
            for (Question q : wrongQuestions) {
                wrongDisplay.append("Q: ").append(q.getQuestion()).append("\n")
                        .append("Correct Answer: ").append(q.getAnswer()).append("\n\n");
            }
            tvWrongQuestions.setText(wrongDisplay.toString());
            tvWrongQuestions.setVisibility(View.VISIBLE);
        } else {
            tvWrongQuestions.setText("Congratulations! You answered all questions correctly.");
            tvWrongQuestions.setVisibility(View.VISIBLE);
        }
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        btnRetryQuiz.setVisibility(View.VISIBLE);
        btnCompleteLesson.setText("Complete Lesson");
        btnCompleteLesson.setVisibility(View.VISIBLE);
        btnReturnToLessons.setVisibility(View.VISIBLE);
        QuizAttempt attempt = new QuizAttempt(quizQuestions, wrongQuestions, score);
        String quizIdentifier = "quiz" + lessonNumber;
        UserQuizDatabaseHelper dbHelper = new UserQuizDatabaseHelper();
        dbHelper.saveQuizAttemptForQuiz(chapterNumber, quizIdentifier, attempt, new UserQuizDatabaseHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Quiz attempt saved successfully.");
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to save quiz attempt: " + error);
            }
        });
    }

    private void completeLesson() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(QuizActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        firestore.collection("ChapterContent")
                .document(language)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("lessonNumber", lessonNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot chapterDoc = querySnapshot.getDocuments().get(0);
                        String chapterId = chapterDoc.getId();
                        Map<String, Object> progressData = new HashMap<>();
                        progressData.put("chapterNumber", chapterNumber);
                        progressData.put("lessonNumber", lessonNumber);
                        progressData.put("progress", true);

                        firestore.collection("UserProgress")
                                .document(userId)
                                .collection("Chapters")
                                .document(chapterId)
                                .set(progressData)
                                .addOnSuccessListener(aVoid -> {
                                    Intent intent = new Intent(QuizActivity.this, LessonActivity.class);
                                    intent.putExtra("chapterNumber", chapterNumber);
                                    intent.putExtra("language", language);
                                    intent.putExtra("skillLevel", skillLevel);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(QuizActivity.this, "Failed to update lesson progress: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(QuizActivity.this, "Chapter data not found. Please contact the admin.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(QuizActivity.this, "Error fetching chapter data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
