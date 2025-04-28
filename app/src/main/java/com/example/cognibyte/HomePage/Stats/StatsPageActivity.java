package com.example.cognibyte.HomePage.Stats;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.HomePage.ProfileActivity;
import com.example.cognibyte.HomePage.Recap.RecapActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class StatsPageActivity extends AppCompatActivity {

    private Spinner spinnerLanguage;
    private ProgressBar progressOverall;
    private TextView tvOverallPct;
    private Button btnViewStats, btnViewQuizStats;
    private ImageView btnBack;
    private ImageView btnHome, btnCodeQuiz, btnChapter, btnProfile;
    private final List<String> userLanguages = new ArrayList<>();
    private String selectedLanguage;
    private static final int TOTAL_CHAPTERS = 5;
    private FirebaseFirestore firestore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_page);

        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        progressOverall = findViewById(R.id.progressOverall);
        tvOverallPct = findViewById(R.id.tvOverallPct);
        btnViewStats = findViewById(R.id.btnViewStats);
        btnViewQuizStats = findViewById(R.id.btnViewQuizStats);
        btnHome = findViewById(R.id.btnHome);
        btnCodeQuiz = findViewById(R.id.btncodeQuiz);
        btnChapter = findViewById(R.id.btnChapter);
        btnProfile = findViewById(R.id.btnProfile);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = u.getUid();
        btnViewStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        btnViewQuizStats.setOnClickListener(v -> startActivity(new Intent(this, QuizStatsActivity.class)));
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        btnCodeQuiz.setOnClickListener(v -> startActivity(new Intent(this, RecapActivity.class)));
        btnChapter.setOnClickListener(v -> startActivity(new Intent(this, com.example.cognibyte.HomePage.ChapterActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        setupLanguageSpinner();
    }

    private void setupLanguageSpinner() {
        firestore.collection("Languages")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    Object arr = doc.get("languages");
                    if (arr instanceof List<?>) {
                        userLanguages.clear();
                        for (Object o : (List<?>) arr) {
                            if (o instanceof String) {
                                userLanguages.add((String) o);
                            }
                        }
                    }

                    if (userLanguages.isEmpty()) {
                        Toast.makeText(this, "Please add a language first.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userLanguages);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguage.setAdapter(adapter);

                    String stored = doc.getString("language");
                    if (stored != null && userLanguages.contains(stored)) {
                        selectedLanguage = stored;
                    } else {
                        selectedLanguage = userLanguages.get(0);
                    }
                    spinnerLanguage.setSelection(userLanguages.indexOf(selectedLanguage));

                    spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String lang = userLanguages.get(position);
                            if (!lang.equals(selectedLanguage)) {
                                selectedLanguage = lang;
                                loadOverallProgress();
                            }
                        }
                    });

                    loadOverallProgress();
                })
                .addOnFailureListener(e -> {
                    Log.e("StatsPage", "load languages", e);
                    Toast.makeText(this, "Could not load your languages.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOverallProgress() {
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("lessonNumber", 5)
                .whereEqualTo("progress", true)
                .get()
                .addOnSuccessListener(qs -> {
                    int done = qs.size();
                    int pct = Math.round(done * 100f / TOTAL_CHAPTERS);
                    progressOverall.setMax(100);
                    progressOverall.setProgress(pct);
                    tvOverallPct.setText(pct + "%");
                })
                .addOnFailureListener(e -> Log.e("StatsPage", "loadOverallProgress", e));
    }
}
