package com.example.cognibyte.HomePage.Language;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddLanguageActivity extends AppCompatActivity {

    private TextView currentLanguageTextView;
    private ImageView currentLanguageLogoImageView;
    private Spinner languageSpinner;
    private Button addLanguageButton;
    private FirebaseFirestore db;
    private String[] allLanguages = {"Java", "Python", "HTML", "JavaScript"};
    private String userUid;
    private String currentLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_language);

        db  = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentLanguageTextView = findViewById(R.id.currentLanguageTextView);
        currentLanguageLogoImageView = findViewById(R.id.currentLanguageLogoImageView);
        languageSpinner = findViewById(R.id.languageSpinner);
        addLanguageButton = findViewById(R.id.addLanguageButton);

        db.collection("Languages")
                .document(userUid)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> chosen = new ArrayList<>();
                    Object arrObj = doc.get("languages");
                    if (arrObj instanceof List) {
                        for (Object o : (List<?>)arrObj) {
                            if (o instanceof String) chosen.add((String)o);
                        }
                    }

                    String top = doc.getString("language");
                    if (top != null) {
                        currentLanguage = top;
                    } else if (!chosen.isEmpty()) {
                        currentLanguage = chosen.get(chosen.size()-1);
                    } else {
                        currentLanguage = "Java";
                        chosen.add("Java");
                    }

                    currentLanguageTextView.setText("Current Language: " + currentLanguage);
                    setLanguageLogo(currentLanguage);

                    List<String> toAdd = new ArrayList<>();
                    for (String lang : allLanguages) {
                        if (!chosen.contains(lang)) {
                            toAdd.add(lang);
                        }
                    }
                    if (toAdd.isEmpty()) {
                        toAdd.add("No more to add");
                        addLanguageButton.setEnabled(false);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            toAdd
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    languageSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading language.", Toast.LENGTH_SHORT).show()
                );

        addLanguageButton.setOnClickListener(v -> {
            String newLang = (String)languageSpinner.getSelectedItem();
            if (newLang == null || newLang.equals("No more to add")) return;

            Map<String,Object> payload = new HashMap<>();
            payload.put("uid", userUid);
            payload.put("language", newLang);
            payload.put("languages", FieldValue.arrayUnion(newLang));

            db.collection("Languages")
                    .document(userUid)
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener(r -> {
                        Toast.makeText(this, "Language added!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to add: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
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
