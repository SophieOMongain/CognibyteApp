package com.example.cognibyte.HomePage.Stats;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.cognibyte.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private Spinner spinnerLanguageStats, spinnerChapterProgress;
    private ProgressBar progressChapter;
    private TextView tvChapterPct;
    private EditText etLevel;
    private BarChart barChartLessons;
    private ImageView btnBack;

    private final List<String> userLanguages = new ArrayList<>();
    private final List<String> chapterTitles = new ArrayList<>();
    private final Map<String, Integer> chapterTitleToNumber = new LinkedHashMap<>();

    private String selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        spinnerLanguageStats = findViewById(R.id.spinnerLanguageStats);
        spinnerChapterProgress = findViewById(R.id.spinnerChapterProgress);
        progressChapter = findViewById(R.id.progressChapter);
        tvChapterPct = findViewById(R.id.tvChapterPct);
        etLevel = findViewById(R.id.et_level);
        barChartLessons = findViewById(R.id.bar_chart_lessons);
        btnBack = findViewById(R.id.btn_back);

        barChartLessons.setNoDataText("No data available");
        barChartLessons.setBackgroundColor(Color.WHITE);
        barChartLessons.getDescription().setEnabled(false);

        btnBack.setOnClickListener(v -> finish());
        setupLanguageSpinner();
    }

    private void setupLanguageSpinner() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("Languages")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Object langs = doc.get("languages");
                    if (langs instanceof List<?>) {
                        userLanguages.clear();
                        for (Object lang : (List<?>) langs) {
                            if (lang instanceof String) {
                                userLanguages.add((String) lang);
                            }
                        }
                    }

                    if (userLanguages.isEmpty()) {
                        Toast.makeText(this, "No languages found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userLanguages);
                    langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguageStats.setAdapter(langAdapter);

                    selectedLanguage = doc.getString("language");
                    if (selectedLanguage == null || !userLanguages.contains(selectedLanguage)) {
                        selectedLanguage = userLanguages.get(0);
                    }

                    spinnerLanguageStats.setSelection(userLanguages.indexOf(selectedLanguage));
                    loadLessonProgressData();
                    loadUserData();
                    loadChapterTitles();

                    spinnerLanguageStats.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onNothingSelected(AdapterView<?> parent) {}

                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedLanguage = userLanguages.get(position);
                            loadLessonProgressData();
                            loadUserData();
                            loadChapterTitles();
                        }
                    });
                });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("Languages").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String skill = doc.getString("skillLevel");
                    etLevel.setText(skill != null ? skill : "Unknown");
                })
                .addOnFailureListener(e -> etLevel.setText("Error"));
    }

    private void loadLessonProgressData() {
        if (mAuth.getCurrentUser() == null || selectedLanguage == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("UserProgress").document(uid)
                .collection("Languages").document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    Map<Integer, Integer> counts = new HashMap<>();
                    for (int i = 1; i <= 5; i++) counts.put(i, 0);

                    for (DocumentSnapshot doc : qs) {
                        Long cnum = doc.getLong("chapterNumber");
                        Boolean done = doc.getBoolean("progress");
                        if (cnum != null && Boolean.TRUE.equals(done)) {
                            counts.put(cnum.intValue(), counts.getOrDefault(cnum.intValue(), 0) + 1);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    for (int i = 1; i <= 5; i++) {
                        entries.add(new BarEntry(i, counts.getOrDefault(i, 0)));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Lessons Completed");
                    dataSet.setColor(ContextCompat.getColor(this, R.color.primary_blue));
                    BarData barData = new BarData(dataSet);
                    barData.setBarWidth(0.7f);

                    barChartLessons.setData(barData);
                    barChartLessons.setFitBars(true);

                    XAxis xAxis = barChartLessons.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override public String getFormattedValue(float value) {
                            return (value >= 1 && value <= 5) ? "Chapter " + ((int) value) : "";
                        }
                    });

                    barChartLessons.getAxisLeft().setDrawGridLines(false);
                    barChartLessons.getAxisRight().setEnabled(false);
                    barChartLessons.getDescription().setEnabled(false);

                    barChartLessons.invalidate();
                });
    }

    private void loadChapterTitles() {
        chapterTitles.clear();
        chapterTitleToNumber.clear();

        if (mAuth.getCurrentUser() == null || selectedLanguage == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("UserProgress")
                .document(uid)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(progressSnapshots -> {
                    Set<Integer> uniqueChapterNumbers = new HashSet<>();
                    for (DocumentSnapshot doc : progressSnapshots) {
                        Long cnum = doc.getLong("chapterNumber");
                        if (cnum != null) uniqueChapterNumbers.add(cnum.intValue());
                    }

                    if (uniqueChapterNumbers.isEmpty()) {
                        Toast.makeText(this, "No chapters found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection("ChapterContent")
                            .document(selectedLanguage)
                            .collection("Chapters")
                            .get()
                            .addOnSuccessListener(contentSnapshots -> {
                                for (DocumentSnapshot doc : contentSnapshots) {
                                    Long chapterNum = doc.getLong("chapterNumber");
                                    String chapterTitle = doc.getString("chapterTitle");

                                    if (chapterNum != null && chapterTitle != null && uniqueChapterNumbers.contains(chapterNum.intValue())) {
                                        if (!chapterTitleToNumber.containsValue(chapterNum.intValue())) {
                                            chapterTitles.add(chapterTitle);
                                            chapterTitleToNumber.put(chapterTitle, chapterNum.intValue());
                                        }
                                    }
                                }

                                if (!chapterTitles.isEmpty()) {
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chapterTitles);
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinnerChapterProgress.setAdapter(adapter);
                                    spinnerChapterProgress.setEnabled(true);

                                    spinnerChapterProgress.setSelection(0);
                                    Integer firstNum = chapterTitleToNumber.get(chapterTitles.get(0));
                                    if (firstNum != null) loadChapterProgress(firstNum);

                                    spinnerChapterProgress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override public void onNothingSelected(AdapterView<?> parent) {}

                                        @Override
                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                            String selected = chapterTitles.get(position);
                                            Integer number = chapterTitleToNumber.get(selected);
                                            if (number != null) loadChapterProgress(number);
                                        }
                                    });
                                } else {
                                    Toast.makeText(this, "No matching chapters with titles.", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void loadChapterProgress(int chapterNumber) {
        if (mAuth.getCurrentUser() == null || selectedLanguage == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("UserProgress").document(uid)
                .collection("Languages").document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chapterNumber)
                .whereEqualTo("progress", true)
                .get()
                .addOnSuccessListener(qs -> {
                    int completed = qs.size();
                    int totalLessons = 5;
                    int percent = (int) ((completed / (float) totalLessons) * 100);

                    progressChapter.setMax(100);
                    progressChapter.setProgress(percent);
                    tvChapterPct.setText(percent + "%");
                })
                .addOnFailureListener(e -> {
                    progressChapter.setProgress(0);
                    tvChapterPct.setText("0%");
                    Toast.makeText(this, "Failed to load chapter progress.", Toast.LENGTH_SHORT).show();
                });
    }
}
