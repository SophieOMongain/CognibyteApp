package com.example.cognibyte.Account;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cognibyte.Admin.AdminActivity;
import com.example.cognibyte.HomePage.HomeActivity;
import com.example.cognibyte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Button createAccountButton;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private VideoView robotVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createAccountButton = findViewById(R.id.create_account_button);
        loginButton = findViewById(R.id.login_button);
        robotVideo = findViewById(R.id.robot_video);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        createAccountButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateAccountActivity.class));
        });
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        String path = "android.resource://" + getPackageName() + "/" + R.raw.animated_robot1;
        robotVideo.setVideoURI(Uri.parse(path));
        robotVideo.setBackgroundColor(Color.TRANSPARENT);

        robotVideo.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            robotVideo.start();
        });

        robotVideo.setOnErrorListener((mp, what, extra) -> {
            ImageView fallback = new ImageView(this);
            fallback.setImageResource(R.drawable.robot_logo);
            ((android.view.ViewGroup) robotVideo.getParent())
                    .removeView(robotVideo);
            ((android.view.ViewGroup) findViewById(R.id.login_box))
                    .addView(fallback, 0);
            return true;

        });
        checkUserRoleAndRedirect();
    }

    private void checkUserRoleAndRedirect() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        firestore.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                            documentSnapshot.getString("role") != null) {
                        String role = documentSnapshot.getString("role");
                        Intent dest =
                                "admin".equals(role)
                                        ? new Intent(MainActivity.this, AdminActivity.class)
                                        : new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(dest);
                        finish();
                    } else {
                        Toast.makeText(this,
                                "User role not found. Contact support.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error fetching user role: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
