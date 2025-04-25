package com.example.cognibyte.HomePage.Stats;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.cognibyte.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
import models.QuizAttempt;

public class QuizStatsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerLanguage, spinnerChapters, spinnerLesson, spinnerAttempt;
    private BarChart barChartQuiz;
    private PieChart pieChartAnswers;
    private TextView tvOverallAccuracy;

    private FirebaseFirestore db;
    private String userId;

    private final List<String> userLanguages = new ArrayList<>();
    private final List<String> chapters = new ArrayList<>();
    private final List<String> lessons = new ArrayList<>();
    private final List<DocumentSnapshot> attemptDocs = new ArrayList<>();
    private final List<String> attempts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_stats);

        btnBack = findViewById(R.id.btnBack);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerChapters = findViewById(R.id.spinnerChapters);
        spinnerLesson = findViewById(R.id.spinnerLesson);
        spinnerAttempt = findViewById(R.id.spinnerAttempt);
        barChartQuiz = findViewById(R.id.barChartQuiz);
        pieChartAnswers = findViewById(R.id.pieChartAnswers);
        tvOverallAccuracy = findViewById(R.id.tvOverallAccuracy);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        btnBack.setOnClickListener(v -> finish());

        loadUserLanguages();
    }

    private void loadUserLanguages() {
        db.collection("Languages").document(userId).get()
                .addOnSuccessListener(doc -> {
                    Object arr = doc.get("languages");
                    if (arr instanceof List<?>) {
                        for (Object o : (List<?>) arr) {
                            if (o instanceof String) userLanguages.add((String) o);
                        }
                    }
                    if (userLanguages.isEmpty()) {
                        Toast.makeText(this, "No languages added yet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userLanguages);
                    langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerLanguage.setAdapter(langAdapter);
                    spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                        @Override public void onNothingSelected(AdapterView<?> p) {}
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            loadChapters(userLanguages.get(pos));
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading languages: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadChapters(String language) {
        chapters.clear();
        db.collection("UserQuiz").document(userId).collection("Attempts")
                .whereEqualTo("language", language)
                .get()
                .addOnSuccessListener(qs -> {
                    Set<Integer> chapterNumbers = new TreeSet<>();
                    for (DocumentSnapshot doc : qs) {
                        Number num = doc.getLong("chapterNumber");
                        if (num != null) chapterNumbers.add(num.intValue());
                    }
                    for (int num : chapterNumbers) {
                        chapters.add("Chapter " + num);
                    }
                    ArrayAdapter<String> chapAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chapters);
                    chapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerChapters.setAdapter(chapAdapter);
                    spinnerChapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                        @Override public void onNothingSelected(AdapterView<?> p) {}
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            int chapNum = Integer.parseInt(chapters.get(pos).replace("Chapter ", ""));
                            loadLessons(language, chapNum);
                            loadBarChart(language, chapNum);
                        }
                    });
                });
    }

    private void loadLessons(String language, int chapNum) {
        lessons.clear();
        db.collection("UserQuiz").document(userId).collection("Attempts")
                .whereEqualTo("language", language)
                .whereEqualTo("chapterNumber", chapNum)
                .get()
                .addOnSuccessListener(qs -> {
                    Set<String> lessonTitles = new TreeSet<>();
                    for (DocumentSnapshot doc : qs) {
                        String title = doc.getString("lessonTitle");
                        if (title != null) lessonTitles.add(title);
                    }
                    lessons.addAll(lessonTitles);

                    ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lessons);
                    lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerLesson.setAdapter(lessonAdapter);
                    spinnerLesson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                        @Override public void onNothingSelected(AdapterView<?> p) {}
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            String lesson = lessons.get(pos);
                            loadAttempts(language, chapNum, lesson);
                        }
                    });
                });
    }

    private void loadAttempts(String language, int chapNum, String lesson) {
        attempts.clear();
        attemptDocs.clear();
        db.collection("UserQuiz").document(userId).collection("Attempts")
                .whereEqualTo("language", language)
                .whereEqualTo("chapterNumber", chapNum)
                .whereEqualTo("lessonTitle", lesson)
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot doc : qs) {
                        attemptDocs.add(doc);
                        attempts.add("Attempt " + doc.getLong("attemptNumber"));
                    }
                    ArrayAdapter<String> attAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, attempts);
                    attAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinnerAttempt.setAdapter(attAdapter);
                    spinnerAttempt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                        @Override public void onNothingSelected(AdapterView<?> p) {}
                        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            QuizAttempt at = attemptDocs.get(pos).toObject(QuizAttempt.class);
                            renderPieForAttempt(at);
                        }
                    });
                });
    }

    private void loadBarChart(String language, int chapNum) {
        db.collection("UserQuiz").document(userId).collection("Attempts")
                .whereEqualTo("language", language)
                .whereEqualTo("chapterNumber", chapNum)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Integer> counts = new HashMap<>();
                    for (DocumentSnapshot doc : qs) {
                        String lesson = doc.getString("lessonTitle");
                        counts.put(lesson, counts.getOrDefault(lesson, 0) + 1);
                    }
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>(counts.keySet());
                    Collections.sort(labels);
                    for (int i = 0; i < labels.size(); i++) {
                        entries.add(new BarEntry(i, counts.get(labels.get(i))));
                    }
                    BarDataSet set = new BarDataSet(entries, "Attempts");
                    set.setColor(ContextCompat.getColor(this, R.color.primary_blue));
                    BarData data = new BarData(set);
                    data.setBarWidth(0.9f);
                    barChartQuiz.setData(data);
                    barChartQuiz.getDescription().setEnabled(false);
                    barChartQuiz.setFitBars(true);
                    XAxis x = barChartQuiz.getXAxis();
                    x.setGranularity(1f);
                    x.setPosition(XAxis.XAxisPosition.BOTTOM);
                    x.setValueFormatter(new IndexAxisValueFormatter(labels));
                    barChartQuiz.invalidate();
                });
    }

    private void renderPieForAttempt(QuizAttempt at) {
        if (at == null || at.getQuestionsAsked() == null) {
            pieChartAnswers.clear();
            return;
        }
        int totalQ = at.getQuestionsAsked().size();
        int wrong = at.getWrongQuestions() == null ? 0 : at.getWrongQuestions().size();
        int correct = totalQ - wrong;

        tvOverallAccuracy.setText(totalQ > 0 ? String.format("%d%%", (correct * 100 / totalQ)) : "—%");

        List<PieEntry> ents = new ArrayList<>();
        ents.add(new PieEntry(correct, "Correct"));
        ents.add(new PieEntry(wrong, "Wrong"));
        PieDataSet ds = new PieDataSet(ents, "");

        ds.setColors(
                ContextCompat.getColor(this, R.color.primary_blue),
                ContextCompat.getColor(this, R.color.secondary_blue)
        );
        PieData pd = new PieData(ds);
        pd.setValueTextSize(14f);

        pieChartAnswers.setData(pd);
        pieChartAnswers.getDescription().setEnabled(false);
        pieChartAnswers.setUsePercentValues(true);
        pieChartAnswers.setCenterText("Details");
        pieChartAnswers.setCenterTextSize(16f);
        pieChartAnswers.invalidate();
        pieChartAnswers.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

            @Override public void onNothingSelected() {}
            @Override public void onValueSelected(Entry e, Highlight h) {
                boolean tappedWrong = ((PieEntry) e).getLabel().equals("Wrong");
                List<String> list = new ArrayList<>();
                if (tappedWrong && at.getWrongQuestions() != null) {
                    for (var q : at.getWrongQuestions()) list.add(q.getQuestion());
                } else {
                    for (var q : at.getQuestionsAsked()) {
                        if (at.getWrongQuestions() == null || !at.getWrongQuestions().contains(q))
                            list.add(q.getQuestion());
                    }
                }
                if (list.isEmpty()) {
                    Toast.makeText(QuizStatsActivity.this,
                            tappedWrong ? "No wrong questions" : "No correct questions",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(QuizStatsActivity.this)
                        .setTitle(tappedWrong ? "Wrong" : "Correct")
                        .setItems(list.toArray(new String[0]), null)
                        .show();
            }
        });
    }

    private void clearCharts() {
        barChartQuiz.clear();
        pieChartAnswers.clear();
        tvOverallAccuracy.setText("—%");
    }
}
