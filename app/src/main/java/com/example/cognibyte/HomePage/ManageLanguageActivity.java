package com.example.cognibyte.HomePage;

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
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.SetOptions;
import java.util.*;

public class ManageLanguageActivity extends AppCompatActivity {

    private RecyclerView rvLanguages;
    private Spinner spinnerNewLanguage;
    private Button btnAddLanguage;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private String userUid;
    private List<String> userLanguages = new ArrayList<>();
    private String currentLanguage;

    private static final String[] ALL_LANGUAGES = {
            "Java", "Python", "HTML", "JavaScript"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_language);

        db = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rvLanguages = findViewById(R.id.rvLanguages);
        spinnerNewLanguage = findViewById(R.id.spinnerNewLanguage);
        btnAddLanguage = findViewById(R.id.btnAddLanguage);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, ChapterActivity.class));
            finish();
        });

        rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        rvLanguages.setAdapter(new LangAdapter());
        loadFromFirestore();
    }

    private void loadFromFirestore() {
        db.collection("Languages")
                .document(userUid)
                .get()
                .addOnSuccessListener(doc -> {
                    userLanguages.clear();
                    Object arr = doc.get("languages");
                    if (arr instanceof List<?>) {
                        for (Object o : (List<?>) arr) {
                            if (o instanceof String) {
                                userLanguages.add((String) o);
                            }
                        }
                    }

                    String prim = doc.getString("language");
                    currentLanguage = prim != null
                            ? prim
                            : (userLanguages.isEmpty() ? ALL_LANGUAGES[0] : userLanguages.get(0));

                    List<String> toAdd = new ArrayList<>();
                    for (String s : ALL_LANGUAGES) {
                        if (!userLanguages.contains(s)) toAdd.add(s);
                    }
                    if (toAdd.isEmpty()) {
                        toAdd.add("No more to add");
                        btnAddLanguage.setEnabled(false);
                    } else {
                        btnAddLanguage.setEnabled(true);
                    }

                    ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, toAdd
                    );
                    spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerNewLanguage.setAdapter(spAdapter);

                    btnAddLanguage.setOnClickListener(v -> {
                        String newLang = (String) spinnerNewLanguage.getSelectedItem();
                        if (newLang == null || newLang.startsWith("No more")) return;
                        userLanguages.add(newLang);
                        currentLanguage = newLang;
                        saveToFirestore();
                    });

                    rvLanguages.getAdapter().notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed loading languages: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void saveToFirestore() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("languages", userLanguages);
        payload.put("language", currentLanguage);

        db.collection("Languages")
                .document(userUid)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener(v -> loadFromFirestore())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error saving: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private class LangAdapter extends RecyclerView.Adapter<LangAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manage_language, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String lang = userLanguages.get(pos);

            h.tvName.setText(lang);
            h.rbPrimary.setChecked(lang.equals(currentLanguage));
            h.ivLogo.setImageResource(getLogoRes(lang));

            h.rbPrimary.setOnClickListener(v -> {
                currentLanguage = lang;
                saveToFirestore();
            });

            h.itemView.setOnClickListener(v -> {
                currentLanguage = lang;
                saveToFirestore();
            });

            h.ivDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ManageLanguageActivity.this)
                        .setTitle("Remove Language")
                        .setMessage("Remove " + lang + "?")
                        .setPositiveButton("Yes", (d, w) -> {
                            userLanguages.remove(lang);
                            if (lang.equals(currentLanguage)) {
                                currentLanguage = userLanguages.isEmpty()
                                        ? ALL_LANGUAGES[0]
                                        : userLanguages.get(0);
                            }
                            saveToFirestore();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return userLanguages.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivLogo, ivDelete;
            TextView tvName;
            RadioButton rbPrimary;

            VH(View v) {
                super(v);
                ivLogo = v.findViewById(R.id.ivLangLogo);
                tvName = v.findViewById(R.id.tvLangName);
                rbPrimary = v.findViewById(R.id.rbPrimary);
                ivDelete = v.findViewById(R.id.ivDeleteLang);
            }
        }

        private int getLogoRes(String language) {
            switch (language.toLowerCase()) {
                case "java":
                    return R.drawable.java_icon;
                case "python":
                    return R.drawable.python_icon;
                case "html":
                    return R.drawable.html_icon;
                case "javascript":
                    return R.drawable.javascript_icon;
                default:
                    return R.drawable.code_icon2;
            }
        }
    }
}
