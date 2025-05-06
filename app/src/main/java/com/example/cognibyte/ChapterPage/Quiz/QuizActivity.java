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
            String cleaned = o.replaceAll("^[A-D][).]\\s*", "");
            rb.setText(label + ". " + cleaned);
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

        String chosenText = ((RadioButton) findViewById(sel)).getText().toString();
        String cleanedChosen = chosenText.replaceAll("^[A-Da-d][).]\\s*", "").trim();

        Question cur = quizQuestions.get(currentQuestionIndex);
        String correctAnswer = cur.getAnswer().trim();

        if (correctAnswer.matches("^[A-Da-d]$") && cur.getOptions() != null) {
            int index = correctAnswer.toUpperCase().charAt(0) - 'A';
            if (index >= 0 && index < cur.getOptions().size()) {
                correctAnswer = cur.getOptions().get(index);
            }
        }

        String cleanedCorrect = correctAnswer.replaceAll("^[A-Da-d][).]\\s*", "").trim();

        Log.d("CHECK_ANSWER", "Chosen: " + cleanedChosen);
        Log.d("CHECK_ANSWER", "Correct: " + cleanedCorrect);

        if (cleanedChosen.equalsIgnoreCase(cleanedCorrect)) {
            score++;
            Log.d("CHECK_ANSWER", "Correct! Score: " + score);
        } else {
            wrongQuestions.add(cur);
            Log.d("CHECK_ANSWER", "Incorrect.");
        }

        tvCorrectAnswer.setText("Answer: " + cleanedCorrect);
        tvCorrectAnswer.setVisibility(View.VISIBLE);
        tvExplanation.setText(cur.getExplanation());
        tvExplanation.setVisibility(View.VISIBLE);
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
    }


    private void showFinalScore() {
        Log.d(TAG, "Showing final score: " + score + "/" + quizQuestions.size());

        tvQuestion.setText("Quiz complete! Score: " + score + "/" + quizQuestions.size());
        rgOptions.setVisibility(View.GONE);
        tvCorrectAnswer.setVisibility(View.GONE);
        tvExplanation.setVisibility(View.GONE);

        if (!wrongQuestions.isEmpty()) {
            StringBuilder sb = new StringBuilder("Questions you got wrong:\n");
            for (Question w : wrongQuestions) {
                String answerLabel = w.getAnswer().trim();
                String answerText;
                try {
                    int index = "ABCD".indexOf(answerLabel.toUpperCase());
                    if (index >= 0 && index < w.getOptions().size()) {
                        answerText = w.getOptions().get(index);
                    } else {
                        answerText = answerLabel;
                    }
                } catch (Exception e) {
                    answerText = answerLabel;
                }
                sb.append("- ").append(w.getQuestion())
                        .append("\n  Correct answer: ").append(answerText)
                        .append("\n\n");
            }
            tvWrongQuestions.setText(sb.toString());
            tvWrongQuestions.setVisibility(View.VISIBLE);
        } else {
            tvWrongQuestions.setVisibility(View.GONE);
        }

        if (score <= 2) {
            tvRecommendation.setText("Great effort! To help improve your score, review the lesson recap and try again. You're getting closer every time!");
        } else if (score <= 4) {
            tvRecommendation.setText("Amazing work! You're definitely ready to move forward — keep up the momentum!");
        } else {
            tvRecommendation.setText("You are unstoppable! You're on your way to becoming a coding master!");
        }

        findViewById(R.id.cardRecommendation).setVisibility(View.VISIBLE);
        tvRecommendation.setVisibility(View.VISIBLE);
        findViewById(R.id.bottomButtonGroup).setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        btnRetryQuiz.setVisibility(View.VISIBLE);
        btnCompleteLesson.setVisibility(View.VISIBLE);
        btnReturnToLessons.setVisibility(View.VISIBLE);

        QuizAttempt attempt = new QuizAttempt(new ArrayList<>(quizQuestions), new ArrayList<>(wrongQuestions), score);

        Log.d(TAG, "Saving attempt with score: " + score);
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
                        Log.d(TAG, "Quiz attempt saved successfully.");
                    }

                    @Override
                    public void onFailure(String err) {
                        Log.e(TAG, "Failed to save quiz attempt: " + err);
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
        tvRecommendation.setVisibility(View.GONE);
        findViewById(R.id.cardRecommendation).setVisibility(View.GONE);
        findViewById(R.id.bottomButtonGroup).setVisibility(View.GONE);
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
                    firestore.collection("ChapterContent")
                            .document(language)
                            .collection("Chapters")
                            .whereEqualTo("chapterNumber", nextChapter)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (!snapshot.isEmpty()) {
                                    String chapterTitle = snapshot.getDocuments().get(0).getString("chapterTitle");

                                    Intent intent = new Intent(QuizActivity.this, LessonActivity.class);
                                    intent.putExtra("chapterNumber", nextChapter);
                                    intent.putExtra("chapterTitle", chapterTitle);
                                    intent.putExtra("language", language);
                                    intent.putExtra("skillLevel", skillLevel);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Next chapter not found.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error loading next chapter: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e ->
                        Toast.makeText(QuizActivity.this,
                                "Couldn’t complete lesson: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
