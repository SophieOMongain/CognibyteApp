package com.example.cognibyte.HomePage.Stats;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.cognibyte.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";

    private BarChart barChartLessons;
    private EditText etLevel;
    private Spinner spinnerLanguageStats;
    private ImageView btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String selectedLanguage;
    private List<String> userLanguages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        barChartLessons = findViewById(R.id.bar_chart_lessons);
        etLevel = findViewById(R.id.et_level);
        spinnerLanguageStats = findViewById(R.id.spinnerLanguageStats);
        btnBack = findViewById(R.id.btn_back);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        barChartLessons.setNoDataText("No data for the chart yet!");
        barChartLessons.setBackgroundColor(Color.WHITE);
        barChartLessons.getDescription().setEnabled(false);

        setupLanguageSpinner();
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupLanguageSpinner() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("Languages")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Object langsObj = doc.get("languages");
                    if (langsObj instanceof List<?>) {
                        userLanguages.clear();
                        for (Object o : (List<?>) langsObj) {
                            if (o instanceof String) {
                                userLanguages.add((String) o);
                            }
                        }
                    }

                    if (userLanguages.isEmpty()) {
                        Toast.makeText(this, "No languages found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userLanguages);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguageStats.setAdapter(adapter);

                    String storedLang = doc.getString("language");
                    if (storedLang != null && userLanguages.contains(storedLang)) {
                        selectedLanguage = storedLang;
                    } else {
                        selectedLanguage = userLanguages.get(0);
                    }

                    spinnerLanguageStats.setSelection(userLanguages.indexOf(selectedLanguage));
                    spinnerLanguageStats.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}

                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedLanguage = userLanguages.get(position);
                            loadLessonProgressData();
                            loadUserData();
                        }
                    });

                    loadLessonProgressData();
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading languages", e);
                    Toast.makeText(this, "Error loading languages.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            etLevel.setText("Not logged in");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("Languages")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String skill = doc.getString("skillLevel");
                    etLevel.setText(skill != null ? skill : "Unknown");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading user data", e);
                    etLevel.setText("Error");
                });
    }

    private void loadLessonProgressData() {
        if (mAuth.getCurrentUser() == null || selectedLanguage == null) {
            Toast.makeText(this, "Cannot load data. Missing user or language.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("UserProgress")
                .document(uid)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    Map<Integer, Integer> counts = new HashMap<>();
                    for (int i = 1; i <= 5; i++) {
                        counts.put(i, 0);
                    }

                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Boolean done = d.getBoolean("progress");
                        Long cnum = d.getLong("chapterNumber");
                        Long lessonNum = d.getLong("lessonNumber");
                        if (Boolean.TRUE.equals(done) && cnum != null && lessonNum != null) {
                            int chap = cnum.intValue();
                            counts.put(chap, counts.getOrDefault(chap, 0) + 1);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    for (int i = 1; i <= 5; i++) {
                        entries.add(new BarEntry(i, counts.getOrDefault(i, 0)));
                    }

                    entries.sort(Comparator.comparing(BarEntry::getX));

                    if (!entries.isEmpty()) {
                        BarDataSet set = new BarDataSet(entries, "Lessons Completed");
                        set.setColor(ContextCompat.getColor(this, R.color.primary_blue));
                        BarData data = new BarData(set);
                        data.setBarWidth(0.7f);

                        barChartLessons.setData(data);
                        barChartLessons.setFitBars(true);

                        barChartLessons.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        barChartLessons.getXAxis().setGranularity(1f);
                        barChartLessons.getAxisLeft().setDrawGridLines(false);
                        barChartLessons.getAxisRight().setEnabled(false);
                        barChartLessons.getDescription().setEnabled(false);

                        String[] chapterLabels = new String[]{"", "Chapter 1", "Chapter 2", "Chapter 3", "Chapter 4", "Chapter 5"};

                        barChartLessons.getXAxis().setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                if (value >= 1 && value <= 5) {
                                    return chapterLabels[(int) value];
                                } else {
                                    return "";
                                }
                            }
                        });

                        barChartLessons.invalidate();
                    } else {
                        Toast.makeText(this, "No lessons completed yet!", Toast.LENGTH_SHORT).show();
                        barChartLessons.clear();
                        barChartLessons.setNoDataText("No lessons completed yet!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading progress data", e);
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    barChartLessons.clear();
                    barChartLessons.setNoDataText("Error loading data!");
                });
    }
}
