package com.example.cognibyte.HomePage.Language;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ChangeLanguageActivity extends AppCompatActivity {
    private TextView currentLanguageTextView;
    private ImageView currentLanguageLogoImageView;
    private Spinner languageSpinner;
    private Button changeLanguageButton;
    private FirebaseFirestore db;
    private String[] allLanguages = {"Java", "Python", "HTML", "JavaScript"};
    private String currentLanguage;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_language);

        db = FirebaseFirestore.getInstance();
        userUid = getIntent().getStringExtra("uid");
        if (userUid == null) {
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        currentLanguageTextView = findViewById(R.id.currentLanguageTextView);
        currentLanguageLogoImageView = findViewById(R.id.currentLanguageLogoImageView);
        languageSpinner = findViewById(R.id.languageSpinner);
        changeLanguageButton = findViewById(R.id.changeLanguageButton);

        db.collection("Languages")
                .document(userUid)
                .get()
                .addOnSuccessListener(doc -> {
                    String lang = doc.getString("language");
                    if (lang == null) {
                        List<String> langs = doc.get("languages", List.class);
                        if (langs != null && !langs.isEmpty()) {
                            lang = langs.get(langs.size()-1);
                        } else {
                            lang = "Java";
                        }
                    }
                    currentLanguage = lang;

                    currentLanguageTextView.setText("Current Language: " + currentLanguage);
                    setLanguageLogo(currentLanguage);

                    List<String> options = new ArrayList<>();
                    for (String s : allLanguages) {
                        if (!s.equalsIgnoreCase(currentLanguage)) {
                            options.add(s);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            options
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    languageSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load current language: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });

        changeLanguageButton.setOnClickListener(v -> {
            String newLang = (String) languageSpinner.getSelectedItem();
            if (newLang == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Language Change")
                    .setMessage("Are you sure? Once you change language you can't undo it and you will be starting again from the beginning.")
                    .setPositiveButton("Yes", (d, w) -> saveLanguage(newLang))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void saveLanguage(String newLang) {
        if (userUid == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String,Object> data = new HashMap<>();
        data.put("language", newLang);

        db.collection("Languages")
                .document(userUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Language updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to change language: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLanguageLogo(String language) {
        int res = R.drawable.code_icon2;
        switch (language.toLowerCase()) {
            case "java": res = R.drawable.java_icon; break;
            case "python": res = R.drawable.python_icon; break;
            case "html": res = R.drawable.html_icon; break;
            case "javascript": res = R.drawable.javascript_icon; break;
        }
        currentLanguageLogoImageView.setImageResource(res);
    }
}
