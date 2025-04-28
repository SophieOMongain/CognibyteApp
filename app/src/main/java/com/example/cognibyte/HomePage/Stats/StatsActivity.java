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
    private EditText etLevel, etLanguage;
    private ImageView btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        selectedLanguage = getIntent().getStringExtra("selectedLanguage");

        barChartLessons = findViewById(R.id.bar_chart_lessons);
        etLevel = findViewById(R.id.et_level);
        etLanguage = findViewById(R.id.et_language);
        btnBack = findViewById(R.id.btn_back);

        if (selectedLanguage != null && !selectedLanguage.isEmpty()) {
            etLanguage.setText(selectedLanguage);
        }

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        barChartLessons.setNoDataText("No data for the chart yet!");
        barChartLessons.setBackgroundColor(Color.WHITE);
        barChartLessons.getDescription().setEnabled(false);

        loadUserData();
        loadLessonProgressData();

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(StatsActivity.this, HomeActivity.class));
            finish();
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            etLevel.setText("Not logged in");
            if (etLanguage.getText().toString().isEmpty()) {
                etLanguage.setText("Unknown");
            }
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("Languages")
                .document(uid)
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    String skill = doc.getString("skillLevel");
                    etLevel.setText(skill != null ? skill : "Unknown");

                    if (etLanguage.getText().toString().isEmpty()) {
                        String lang = doc.getString("language");
                        etLanguage.setText(lang != null ? lang : "Unknown");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading user data", e);
                    etLevel.setText("Error");
                    if (etLanguage.getText().toString().isEmpty()) {
                        etLanguage.setText("Error");
                    }
                });
    }

    private void loadLessonProgressData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("UserProgress")
                .document(uid)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(qs -> {
                    Map<Integer, Integer> counts = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Boolean done = d.getBoolean("progress");
                        Long cnum = d.getLong("chapterNumber");
                        if (Boolean.TRUE.equals(done) && cnum != null) {
                            int chap = cnum.intValue();
                            counts.put(chap, counts.getOrDefault(chap, 0) + 1);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                        entries.add(new BarEntry(e.getKey(), e.getValue()));
                    }
                    entries.sort(Comparator.comparing(BarEntry::getX));

                    if (!entries.isEmpty()) {
                        BarDataSet set = new BarDataSet(entries, "Lessons Completed");
                        set.setColor(ContextCompat.getColor(this, R.color.primary_blue));
                        BarData data = new BarData(set);
                        data.setBarWidth(0.9f);

                        barChartLessons.setData(data);
                        barChartLessons.setFitBars(true);

                        XAxis x = barChartLessons.getXAxis();
                        x.setPosition(XAxis.XAxisPosition.BOTTOM);
                        x.setGranularity(1f);
                        x.setLabelCount(entries.size(), true);

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
