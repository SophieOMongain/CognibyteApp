package com.example.cognibyte.HomePage.Language;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cognibyte.HomePage.ChapterActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.SetOptions;
import java.util.*;

public class ChangeLanguageActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rvLanguages;
    private Spinner spinnerNewLanguage;
    private Button btnAddLanguage;
    private TextView tvCurrentLanguage;
    private ImageView ivCurrentLogo;

    private FirebaseFirestore db;
    private String userUid;
    private List<String> userLanguages = new ArrayList<>();
    private String currentLanguage;
    private final String[] allLanguages = {"Java", "Python", "HTML", "JavaScript"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_language);

        db = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnBack = findViewById(R.id.btnBack);
        rvLanguages = findViewById(R.id.rvLanguages);
        spinnerNewLanguage = findViewById(R.id.languageSpinner);
        btnAddLanguage = findViewById(R.id.changeLanguageButton);
        tvCurrentLanguage = findViewById(R.id.currentLanguageTextView);
        ivCurrentLogo = findViewById(R.id.currentLanguageLogoImageView);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, ChapterActivity.class));
            finish();
        });

        rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        rvLanguages.setAdapter(new LanguageAdapter());

        loadUserLanguages();
    }

    private void loadUserLanguages() {
        db.collection("Languages")
                .document(userUid)
                .get()
                .addOnSuccessListener(doc -> {
                    userLanguages.clear();
                    Object langsObj = doc.get("languages");
                    if (langsObj instanceof List<?>) {
                        for (Object o : (List<?>) langsObj) {
                            if (o instanceof String) {
                                userLanguages.add((String) o);
                            }
                        }
                    }
                    String lang = doc.getString("language");
                    currentLanguage = (lang != null)
                            ? lang
                            : (userLanguages.isEmpty() ? allLanguages[0] : userLanguages.get(0));

                    tvCurrentLanguage.setText("Current Language: " + currentLanguage);
                    setLanguageLogo(currentLanguage);

                    List<String> available = new ArrayList<>();
                    for (String s : allLanguages) {
                        if (!userLanguages.contains(s)) available.add(s);
                    }
                    ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, available);
                    spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerNewLanguage.setAdapter(spAdapter);

                    btnAddLanguage.setOnClickListener(v -> {
                        String toAdd = (String) spinnerNewLanguage.getSelectedItem();
                        if (toAdd != null) confirmAddLanguage(toAdd);
                    });

                    rvLanguages.getAdapter().notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load languages: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void confirmAddLanguage(String lang) {
        new AlertDialog.Builder(this)
                .setTitle("Add Language")
                .setMessage("Add " + lang + " to your languages?")
                .setPositiveButton("Yes", (d, w) -> {
                    userLanguages.add(lang);
                    saveLanguages();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setLanguageLogo(String language) {
        int res = R.drawable.code_icon2;

        switch (language.toLowerCase()) {
            case "java": res = R.drawable.java_icon; break;
            case "python": res = R.drawable.python_icon; break;
            case "html": res = R.drawable.html_icon; break;
            case "javascript": res = R.drawable.javascript_icon; break;
        }
        ivCurrentLogo.setImageResource(res);
    }

    private void saveLanguages() {
        Map<String, Object> data = new HashMap<>();
        data.put("languages", userLanguages);
        data.put("language", currentLanguage);

        db.collection("Languages")
                .document(userUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> loadUserLanguages())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error saving languages: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangVH> {
        @NonNull
        @Override
        public LangVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_language, parent, false);
            return new LangVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LangVH h, int pos) {
            String lang = userLanguages.get(pos);

            h.tvName.setText(lang);
            h.ivLogo.setImageResource(getLogoRes(lang));
            h.tvName.setAlpha(lang.equals(currentLanguage) ? 1f : 0.5f);
            h.itemView.setOnClickListener(v -> {
                currentLanguage = lang;
                saveLanguages();
            });

            h.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ChangeLanguageActivity.this)
                        .setTitle("Remove Language")
                        .setMessage("Remove " + lang + "?")
                        .setPositiveButton("Yes", (d, w) -> {
                            userLanguages.remove(lang);
                            if (lang.equals(currentLanguage)) {
                                currentLanguage = userLanguages.isEmpty()
                                        ? allLanguages[0]
                                        : userLanguages.get(0);
                            }
                            saveLanguages();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return userLanguages.size();
        }

        private int getLogoRes(String language) {
            switch (language.toLowerCase()) {
                case "java": return R.drawable.java_icon;
                case "python": return R.drawable.python_icon;
                case "html": return R.drawable.html_icon;
                case "javascript": return R.drawable.javascript_icon;
                default: return R.drawable.code_icon2;
            }
        }

        class LangVH extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView ivLogo, btnDelete;

            LangVH(View v) {
                super(v);
                ivLogo = v.findViewById(R.id.ivLangLogo);
                tvName = v.findViewById(R.id.tvLangName);
                btnDelete = v.findViewById(R.id.ivDeleteLang);
            }
        }
    }
}
