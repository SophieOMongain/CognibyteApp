package com.example.cognibyte.ChapterPage.Quiz;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Account.LoginActivity;
import com.example.cognibyte.ChapterPage.LessonActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;
import models.Question;
import models.QuizAttempt;
import models.UserQuizDatabaseHelper;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";
    private ProgressBar progressBar;
    private TextView tvQuestion, tvCorrectAnswer, tvExplanation, tvWrongQuestions, tvRecommendation;
    private RadioGroup rgOptions;
    private Button btnSubmit, btnNext, btnBack, btnReturnToLessons, btnCompleteLesson, btnRetryQuiz;
    private List<Question> quizQuestions = new ArrayList<>();
    private List<Question> wrongQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String language, skillLevel, lessonTitle;
    private int chapterNumber, lessonNumber;
    private FirebaseFirestore firestore;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        progressBar = findViewById(R.id.progressBar);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        tvExplanation = findViewById(R.id.tvExplanation);
        tvWrongQuestions = findViewById(R.id.tvWrongQuestions);
        tvRecommendation = findViewById(R.id.tvRecommendation);
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
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        language = getIntent().getStringExtra("language");
        skillLevel = getIntent().getStringExtra("skillLevel");
        chapterNumber = getIntent().getIntExtra("chapterNumber", -1);
        lessonNumber = getIntent().getIntExtra("lessonNumber", -1);
        lessonTitle = getIntent().getStringExtra("lessonTitle");

        if (language == null || skillLevel == null || chapterNumber < 0 || lessonNumber < 0 || lessonTitle == null) {
            Log.e(TAG, "Missing quiz params");
            Toast.makeText(this, "Error: Missing quiz data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvExplanation.setMovementMethod(new ScrollingMovementMethod());
        btnBack.setOnClickListener(v -> finish());
        btnReturnToLessons.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnNext.setOnClickListener(v -> {
            currentQuestionIndex++;
            showNextQuestion();
        });
        btnRetryQuiz.setOnClickListener(v -> resetQuiz());
        btnCompleteLesson.setOnClickListener(v -> completeLesson());

        btnNext.setVisibility(View.GONE);
        btnCompleteLesson.setVisibility(View.GONE);
        btnRetryQuiz.setVisibility(View.GONE);
        btnReturnToLessons.setVisibility(View.GONE);

        loadQuizFromSubcollection();
    }

    private void loadQuizFromSubcollection() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("QuizContent")
                .document(language)
                .collection("Quizzes")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("lessonNumber", lessonNumber)
                .whereEqualTo("skillLevel", skillLevel)
                .get()
                .addOnSuccessListener(qs -> {
                    progressBar.setVisibility(View.GONE);
                    if (qs.isEmpty()) {
                        Toast.makeText(this, "Quiz not found!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String quizId = qs.getDocuments().get(0).getId();
                    firestore.collection("QuizContent")
                            .document(language)
                            .collection("Quizzes")
                            .document(quizId)
                            .collection("Questions")
                            .get()
                            .addOnSuccessListener(qs2 -> {
                                if (qs2.isEmpty()) {
                                    Toast.makeText(this, "No questions available.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                quizQuestions.clear();
                                for (DocumentSnapshot d : qs2.getDocuments()) {
                                    Question q = d.toObject(Question.class);
                                    if (q != null) quizQuestions.add(q);
                                }
                                if (quizQuestions.size() < 5) {
                                    Toast.makeText(this, "Not enough questions!", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Collections.shuffle(quizQuestions);
                                quizQuestions = quizQuestions.subList(0, 5);
                                showNextQuestion();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading questions", e);
                                Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error finding quiz", e);
                    Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showNextQuestion() {
        if (currentQuestionIndex >= quizQuestions.size()) {
            showFinalScore();
            return;
        }
        Question q = quizQuestions.get(currentQuestionIndex);
        tvQuestion.setText(q.getQuestion());

        rgOptions.removeAllViews();
        char label = 'A';
        for (String o : q.getOptions()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(label + ". " + o);
            rgOptions.addView(rb);
            label++;
        }

        tvCorrectAnswer.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
    }

    private void checkAnswer() {
        int sel = rgOptions.getCheckedRadioButtonId();
        if (sel < 0) {
            Toast.makeText(this, "Please select an answer!", Toast.LENGTH_SHORT).show();
            return;
        }
        String chosen = ((RadioButton) findViewById(sel)).getText().toString();
        if (chosen.contains(". ")) {
            chosen = chosen.substring(chosen.indexOf(". ") + 2).trim();
        }
        Question cur = quizQuestions.get(currentQuestionIndex);
        if (chosen.equalsIgnoreCase(cur.getAnswer())) {
            score++;
        } else {
            wrongQuestions.add(cur);
        }

        tvCorrectAnswer.setText("Answer: " + cur.getAnswer());
        tvCorrectAnswer.setVisibility(View.VISIBLE);
        tvExplanation.setText(cur.getExplanation());
        tvExplanation.setVisibility(View.VISIBLE);
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void showFinalScore() {
        tvQuestion.setText("Quiz complete! Score: " + score + "/" + quizQuestions.size());
        rgOptions.setVisibility(View.GONE);
        tvCorrectAnswer.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.GONE);

        if (!wrongQuestions.isEmpty()) {
            StringBuilder sb = new StringBuilder("You got wrong:\n");
            for (Question w : wrongQuestions) {
                sb.append("- ").append(w.getQuestion())
                        .append("\n  Ans: ").append(w.getAnswer())
                        .append("\n\n");
            }
            tvWrongQuestions.setText(sb.toString());
            tvWrongQuestions.setVisibility(View.VISIBLE);
        }

        if (score <= 2) {
            tvRecommendation.setText("Good effort! To improve your score, review the lesson recap and try again. You're getting closer every time!");
        } else if (score == 3 || score == 4) {
            tvRecommendation.setText("Amazing work! You're definitely ready to move forward — keep up the momentum!");
        } else if (score == 5) {
            tvRecommendation.setText("You are unstoppable! You're on your way to becoming a coding master!");
        }

        findViewById(R.id.cardRecommendation).setVisibility(View.VISIBLE);
        tvRecommendation.setVisibility(View.VISIBLE);
        findViewById(R.id.bottomButtonGroup).setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        btnRetryQuiz.setVisibility(View.VISIBLE);
        btnCompleteLesson.setVisibility(View.VISIBLE);
        btnReturnToLessons.setVisibility(View.VISIBLE);

        QuizAttempt attempt = new QuizAttempt(quizQuestions, wrongQuestions, score);
        new UserQuizDatabaseHelper().saveQuizAttemptForQuiz(
                chapterNumber,
                "Chapter " + chapterNumber,
                lessonNumber,
                lessonTitle,
                language,
                attempt,
                new UserQuizDatabaseHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Saved quiz attempt");
                    }

                    @Override
                    public void onFailure(String err) {
                        Log.e(TAG, "Save failed: " + err);
                    }
                }
        );
    }

    private void resetQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        wrongQuestions.clear();
        tvWrongQuestions.setVisibility(View.GONE);
        rgOptions.setVisibility(View.VISIBLE);
        btnRetryQuiz.setVisibility(View.GONE);
        btnCompleteLesson.setVisibility(View.GONE);
        btnReturnToLessons.setVisibility(View.GONE);
        if (tvRecommendation != null) tvRecommendation.setVisibility(View.GONE);
        showNextQuestion();
    }

    private void completeLesson() {
        Map<String, Object> data = new HashMap<>();
        data.put("chapterNumber", chapterNumber);
        data.put("lessonNumber", lessonNumber);
        data.put("lessonTitle", lessonTitle);
        data.put("progress", true);

        String docId = "Chapter" + chapterNumber + "_Lesson" + lessonNumber;

        firestore.collection("UserProgress")
                .document(user.getUid())
                .collection("Languages")
                .document(language)
                .collection("Chapters")
                .document(docId)
                .set(data)
                .addOnSuccessListener(v -> {
                    int nextChapter = (lessonNumber == 5)
                            ? chapterNumber + 1
                            : chapterNumber;
                    Intent intent = new Intent(QuizActivity.this, LessonActivity.class);
                    intent.putExtra("chapterNumber", nextChapter);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(QuizActivity.this,
                                "Couldn’t complete lesson: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
