package com.example.cognibyte.HomePage.Stats;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class StatsActivity extends AppCompatActivity {

    private BarChart barChartLessons;
    private EditText etLevel, etLanguage;
    private ImageView btnBack;
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

        firestore.collection("UserProgress")
                .document(uid)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    Map<Integer, Integer> chapterLessonCount = new HashMap<>();

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Boolean isDone = doc.getBoolean("progress");
                        Long chapterNum = doc.getLong("chapterNumber");

                        if (Boolean.TRUE.equals(isDone) && chapterNum != null) {
                            int num = chapterNum.intValue();
                            chapterLessonCount.put(num, chapterLessonCount.getOrDefault(num, 0) + 1);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();

                    for (Map.Entry<Integer, Integer> entry : chapterLessonCount.entrySet()) {
                        entries.add(new BarEntry(entry.getKey(), entry.getValue()));
                    }

                    entries.sort(Comparator.comparing(BarEntry::getX));

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
                    Log.e(TAG, "Error loading data from UserProgress: ", e);
                    barChartLessons.clear();
                    barChartLessons.setNoDataText("Error loading data!");
                });
    }
}