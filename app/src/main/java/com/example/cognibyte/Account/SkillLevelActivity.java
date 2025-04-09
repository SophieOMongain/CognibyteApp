package com.example.cognibyte.Account;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cognibyte.HomePage.ChapterActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SkillLevelActivity extends AppCompatActivity {

    private SeekBar skillSeekBar;
    private TextView skillLevelDisplay;
    private Button saveButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    private final String[] skillLevels = {"Beginner", "Intermediate", "Expert"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_level);  // Ensure this layout matches your exact design

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        skillSeekBar = findViewById(R.id.skill_seekbar);
        skillLevelDisplay = findViewById(R.id.skill_level_display);
        saveButton = findViewById(R.id.btn_save_skill);

        skillSeekBar.setMax(2);
        skillSeekBar.setProgress(0);
        skillLevelDisplay.setText(skillLevels[0]);

        skillSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                skillLevelDisplay.setText(skillLevels[progress]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        saveButton.setOnClickListener(v -> saveSkillLevel());
    }

    private void saveSkillLevel() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedSkillLevel = skillLevels[skillSeekBar.getProgress()];

        Map<String, Object> updates = new HashMap<>();
        updates.put("skillLevel", selectedSkillLevel);

        firestore.collection("Languages").document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Skill level saved: " + selectedSkillLevel, Toast.LENGTH_SHORT).show();
                    navigateToNextStep();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save skill level: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void navigateToNextStep() {
        Intent intent = new Intent(SkillLevelActivity.this, ChapterActivity.class);
        startActivity(intent);
        finish();
    }
}
