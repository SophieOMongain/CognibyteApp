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
import com.example.cognibyte.HomePage.Recap.RecapActivity;
import com.example.cognibyte.HomePage.Stats.LessonStatsActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class ChapterActivity extends AppCompatActivity {

    private Spinner spinnerLanguage;
    private ImageView imgLanguageLogo;
    private Button btnChapter1, btnChapter2, btnChapter3, btnChapter4, btnChapter5;
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
        btnChapter5 = findViewById(R.id.btnChapter5);
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
        btnChapter5.setOnClickListener(v -> startChapter(5));
        btnHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        btnProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        btnStats.setOnClickListener(v -> navigateTo(LessonStatsActivity.class));
        btnCodeQuiz.setOnClickListener(v -> navigateTo(RecapActivity.class));
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
                        Toast.makeText(this, "Please select a language first.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, com.example.cognibyte.Account.LanguageSelectionActivity.class));
                        finish();
                        return;
                    }

                    selectedLanguage = normalizeLanguage(lang);
                    skillLevel = skill;
                    userLanguages = langsList.isEmpty() ? Collections.singletonList(lang) : langsList;

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userLanguages);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguage.setAdapter(adapter);

                    int idx = userLanguages.indexOf(selectedLanguage);
                    if (idx >= 0) spinnerLanguage.setSelection(idx);

                    imgLanguageLogo.setImageResource(getLanguageIcon(selectedLanguage));
                    updateChapterButtons();
                    loadChapterTitles();
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
                    selectedLanguage = normalizeLanguage(newLang);
                    imgLanguageLogo.setImageResource(getLanguageIcon(selectedLanguage));
                    Toast.makeText(this, "Switched to " + newLang, Toast.LENGTH_SHORT).show();
                    updateChapterButtons();
                    loadChapterTitles();
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

    private String normalizeLanguage(String lang) {
        if (lang.equalsIgnoreCase("javascript")) return "Javascript";
        return lang;
    }

    private void updateChapterButtons() {
        setEnabled(btnChapter1, true);

        BiConsumer<Button, Integer> unlockIfPrevDone = (btn, prevChap) -> {
            firestore.collection("UserProgress")
                    .document(userId)
                    .collection("Languages")
                    .document(selectedLanguage)
                    .collection("Chapters")
                    .whereEqualTo("chapterNumber", prevChap)
                    .whereEqualTo("lessonNumber", 5)
                    .whereEqualTo("progress", true)
                    .get()
                    .addOnSuccessListener(qs -> {
                        boolean ok = !qs.isEmpty();
                        setEnabled(btn, ok);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking progress for chapter " + prevChap, e);
                        setEnabled(btn, false);
                    });
        };

        unlockIfPrevDone.accept(btnChapter2, 1);
        unlockIfPrevDone.accept(btnChapter3, 2);
        unlockIfPrevDone.accept(btnChapter4, 3);
        unlockIfPrevDone.accept(btnChapter5, 4);
    }

    private void loadChapterTitles() {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Long lessonNum = doc.getLong("lessonNumber");
                        if (lessonNum != null && lessonNum == 1) {
                            docs.add(doc);
                        }
                    }
                    Collections.sort(docs, (d1, d2) -> {
                        Long chapNum1 = d1.getLong("chapterNumber");
                        Long chapNum2 = d2.getLong("chapterNumber");
                        return Long.compare(chapNum1 != null ? chapNum1 : 0, chapNum2 != null ? chapNum2 : 0);
                    });

                    List<String> chapterTitles = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        String title = doc.getString("chapterTitle");
                        if (title != null) {
                            chapterTitles.add(title);
                        }
                    }

                    if (chapterTitles.size() > 0) btnChapter1.setText(chapterTitles.get(0));
                    if (chapterTitles.size() > 1) btnChapter2.setText(chapterTitles.get(1));
                    if (chapterTitles.size() > 2) btnChapter3.setText(chapterTitles.get(2));
                    if (chapterTitles.size() > 3) btnChapter4.setText(chapterTitles.get(3));
                    if (chapterTitles.size() > 4) btnChapter5.setText(chapterTitles.get(4));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load chapter titles", e);
                    Toast.makeText(this, "Error loading chapter titles.", Toast.LENGTH_SHORT).show();
                });
    }

    private void startChapter(int num) {
        if (num == 1) {
            launchLessonActivity(1);
            return;
        }
        firestore.collection("UserProgress")
                .document(userId)
                .collection("Languages")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", num - 1)
                .whereEqualTo("lessonNumber", 5)
                .whereEqualTo("progress", true)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        Toast.makeText(this, "Complete Chapter " + (num - 1) + " first!", Toast.LENGTH_SHORT).show();
                    } else {
                        launchLessonActivity(num);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking progress for chapter " + (num - 1), e);
                    Toast.makeText(this, "Could not check progress.", Toast.LENGTH_SHORT).show();
                });
    }

    private void launchLessonActivity(int chap) {
        firestore.collection("ChapterContent")
                .document(selectedLanguage)
                .collection("Chapters")
                .whereEqualTo("chapterNumber", chap)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String chapterTitle = queryDocumentSnapshots.getDocuments().get(0).getString("chapterTitle");
                        Intent i = new Intent(this, com.example.cognibyte.ChapterPage.LessonActivity.class);
                        i.putExtra("chapterNumber", chap);
                        i.putExtra("chapterTitle", chapterTitle);
                        i.putExtra("language", selectedLanguage);
                        i.putExtra("skillLevel", skillLevel);
                        startActivity(i);
                    } else {
                        Toast.makeText(this, "Chapter not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch chapter title", e);
                    Toast.makeText(this, "Error loading chapter.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    private void setEnabled(Button btn, boolean ok) {
        btn.setEnabled(ok);
        btn.setAlpha(ok ? 1f : 0.5f);
    }
}
