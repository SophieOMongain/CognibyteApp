package com.example.cognibyte.HomePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.ChapterPage.CodeQuiz.WeeklyQuizActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChapterActivity extends AppCompatActivity {

    private Spinner spinnerLanguage;
    private ImageView imgLanguageLogo;
    private Button btnChapter1, btnChapter2, btnChapter3, btnChapter4;
    private ImageView btnHome, btnProfile, btnStats, btnCodeQuiz;
    private FirebaseFirestore firestore;
    private String userId;
    private String selectedLanguage;
    private String skillLevel;
    private List<String> userLanguages = Collections.emptyList();
    private static final String TAG = "ChapterActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        imgLanguageLogo = findViewById(R.id.imgLanguageLogo);
        btnChapter1 = findViewById(R.id.btnChapter1);
        btnChapter2 = findViewById(R.id.btnChapter2);
        btnChapter3 = findViewById(R.id.btnChapter3);
        btnChapter4 = findViewById(R.id.btnChapter4);
        btnHome = findViewById(R.id.btnHome);
        btnProfile = findViewById(R.id.btnProfile);
        btnStats = findViewById(R.id.btnStats);
        btnCodeQuiz = findViewById(R.id.btncodeQuiz);

        loadUserSelectedLanguageAndSkillLevel();

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String lang = userLanguages.get(position);
                if (!lang.equalsIgnoreCase(selectedLanguage)) {
                    switchLanguage(lang);
                }
            }
        });

        btnChapter1.setOnClickListener(v -> startChapter(1));
        btnChapter2.setOnClickListener(v -> startChapter(2));
        btnChapter3.setOnClickListener(v -> startChapter(3));
        btnChapter4.setOnClickListener(v -> startChapter(4));
        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        btnStats.setOnClickListener(v -> navigateTo(StatsActivity.class));
        btnCodeQuiz.setOnClickListener(v -> navigateTo(WeeklyQuizActivity.class));
    }

    private void loadUserSelectedLanguageAndSkillLevel() {
        firestore.collection("Languages")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String lang = doc.getString("language");
                    String skill = doc.getString("skillLevel");

                    List<String> langsList = new ArrayList<>();
                    Object langsObj = doc.get("languages");
                    if (langsObj instanceof List) {
                        for (Object o : (List<?>) langsObj) {
                            if (o instanceof String) langsList.add((String) o);
                        }
                    }
                    if (lang == null && !langsList.isEmpty()) {
                        lang = langsList.get(0);
                    }

                    if (lang == null || skill == null) {
                        Toast.makeText(this,
                                "Please select a language first.",
                                Toast.LENGTH_LONG
                        ).show();
                        startActivity(new Intent(
                                this,
                                com.example.cognibyte.Account.LanguageSelectionActivity.class
                        ));
                        finish();
                        return;
                    }

                    selectedLanguage = lang;
                    skillLevel = skill;
                    userLanguages = langsList.isEmpty()
                            ? Collections.singletonList(lang)
                            : langsList;

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            userLanguages
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguage.setAdapter(adapter);

                    int idx = userLanguages.indexOf(selectedLanguage);
                    if (idx >= 0) spinnerLanguage.setSelection(idx);

                    imgLanguageLogo.setImageResource(getLanguageIcon(selectedLanguage));
                    updateChapterButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading language data", e);
                    Toast.makeText(this, "Error loading language.", Toast.LENGTH_SHORT).show();
                });
    }

    private void switchLanguage(String newLang) {
        firestore.collection("Languages")
                .document(userId)
                .update("language", newLang)
                .addOnSuccessListener(a -> {
                    selectedLanguage = newLang;
                    imgLanguageLogo.setImageResource(getLanguageIcon(newLang));
                    Toast.makeText(this, "Switched to " + newLang, Toast.LENGTH_SHORT).show();
                    updateChapterButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to switch language", e);
                    Toast.makeText(this, "Could not switch language.", Toast.LENGTH_SHORT).show();
                });
    }

    private int getLanguageIcon(String language) {
        switch (language.toLowerCase()) {
            case "javascript": return R.drawable.javascript_icon;
            case "python": return R.drawable.python_icon;
            case "java": return R.drawable.java_icon;
            case "html": return R.drawable.html_icon;
            default: return R.drawable.home;
        }
    }

    private void updateChapterButtons() {
        firestore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    boolean c1 = Boolean.TRUE.equals(doc.getBoolean("Chapter1"));
                    boolean c2 = Boolean.TRUE.equals(doc.getBoolean("Chapter2"));
                    boolean c3 = Boolean.TRUE.equals(doc.getBoolean("Chapter3"));

                    btnChapter1.setEnabled(true);
                    setEnabled(btnChapter2, c1);
                    setEnabled(btnChapter3, c2);
                    setEnabled(btnChapter4, c3);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading progress", e));
    }

    private void setEnabled(Button btn, boolean ok) {
        btn.setEnabled(ok);
        btn.setAlpha(ok ? 1f : 0.5f);
    }

    private void startChapter(int num) {
        if (num > 1) {
            firestore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        boolean prev = Boolean.TRUE.equals(doc.getBoolean("Chapter" + (num - 1)));
                        if (!prev) {
                            Toast.makeText(this,
                                    "Complete Chapter " + (num - 1) + " first!",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        Intent i = new Intent(
                                this,
                                com.example.cognibyte.ChapterPage.LessonActivity.class
                        );
                        i.putExtra("chapterNumber", num);
                        i.putExtra("language", selectedLanguage);
                        i.putExtra("skillLevel", skillLevel);
                        startActivity(i);
                    });
        } else {
            Intent i = new Intent(
                    this,
                    com.example.cognibyte.ChapterPage.LessonActivity.class
            );
            i.putExtra("chapterNumber", num);
            i.putExtra("language", selectedLanguage);
            i.putExtra("skillLevel", skillLevel);
            startActivity(i);
        }
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}
