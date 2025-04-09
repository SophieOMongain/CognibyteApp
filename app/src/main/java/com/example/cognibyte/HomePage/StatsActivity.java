package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cognibyte.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private BarChart barChartLessons;
    private EditText etLevel, etLanguage;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private static final String TAG = "StatsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        barChartLessons = findViewById(R.id.bar_chart_lessons);
        etLevel = findViewById(R.id.et_level);
        etLanguage = findViewById(R.id.et_language);
        btnBack = findViewById(R.id.btn_back);

        barChartLessons.setNoDataText("No data for the chart yet!");
        barChartLessons.setBackgroundColor(Color.WHITE);
        barChartLessons.getDescription().setEnabled(false);

        loadUserData();
        loadLessonProgressData();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(StatsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            etLevel.setText("Not logged in");
            etLanguage.setText("Unknown");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading user data for UID: " + uid);

        firestore.collection("Languages").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String skillLevel = documentSnapshot.getString("skillLevel");
                        String language = documentSnapshot.getString("language");
                        Log.d(TAG, "User skill level: " + skillLevel + ", language: " + language);
                        etLevel.setText(skillLevel != null ? skillLevel : "Unknown");
                        etLanguage.setText(language != null ? language : "Unknown");
                    } else {
                        etLevel.setText("Unknown");
                        etLanguage.setText("Unknown");
                        Log.w(TAG, "No Languages document found for UID " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    etLevel.setText("Error");
                    etLanguage.setText("Error");
                    Log.e(TAG, "Failed to load language/skill data: ", e);
                });
    }

    private void loadLessonProgressData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading lesson progress for UID: " + uid);

        firestore.collection("LessonProgress").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "LessonProgress doc exists? " + documentSnapshot.exists());
                    Log.d(TAG, "LessonProgress doc data: " + documentSnapshot.getData());

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(StatsActivity.this, "No lesson progress data found.", Toast.LENGTH_SHORT).show();
                        barChartLessons.clear();
                        barChartLessons.setNoDataText("No lessons completed yet!");
                        return;
                    }

                    Map<String, Object> progressData = documentSnapshot.getData();
                    if (progressData == null || progressData.isEmpty()) {
                        Toast.makeText(StatsActivity.this, "No lesson progress data found.", Toast.LENGTH_SHORT).show();
                        barChartLessons.clear();
                        barChartLessons.setNoDataText("No lessons completed yet!");
                        return;
                    }

                    Map<Integer, Integer> chapterLessonCount = new HashMap<>();
                    for (Map.Entry<String, Object> entry : progressData.entrySet()) {
                        String key = entry.getKey();
                        if (key.matches("Chapter\\d+_Lesson\\d+")) {
                            String[] parts = key.split("_");
                            if (parts.length == 2) {
                                String chapterPart = parts[0];
                                String chapterNumStr = chapterPart.replace("Chapter", "");
                                try {
                                    int chapterNum = Integer.parseInt(chapterNumStr);
                                    Boolean finished = (Boolean) entry.getValue();
                                    if (finished != null && finished) {
                                        int count = chapterLessonCount.getOrDefault(chapterNum, 0);
                                        chapterLessonCount.put(chapterNum, count + 1);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing chapter number from key: " + key, e);
                                }
                            }
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    for (Map.Entry<Integer, Integer> e : chapterLessonCount.entrySet()) {
                        entries.add(new BarEntry(e.getKey(), e.getValue()));
                    }

                    Collections.sort(entries, (a, b) -> Float.compare(a.getX(), b.getX()));

                    if (!entries.isEmpty()) {
                        BarDataSet dataSet = new BarDataSet(entries, "Lessons Completed");
                        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_blue));

                        BarData barData = new BarData(dataSet);
                        barData.setBarWidth(0.9f);

                        barChartLessons.setData(barData);
                        barChartLessons.setFitBars(true);

                        XAxis xAxis = barChartLessons.getXAxis();
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setGranularity(1f);
                        xAxis.setLabelCount(entries.size(), true);

                        barChartLessons.invalidate();
                        Log.d(TAG, "Bar chart updated with " + entries.size() + " entries.");
                    } else {
                        Toast.makeText(StatsActivity.this, "No lessons completed yet!", Toast.LENGTH_SHORT).show();
                        barChartLessons.clear();
                        barChartLessons.setNoDataText("No lessons completed yet!");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatsActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading data from LessonProgress: ", e);
                    barChartLessons.clear();
                    barChartLessons.setNoDataText("Error loading data!");
                });
    }
}
