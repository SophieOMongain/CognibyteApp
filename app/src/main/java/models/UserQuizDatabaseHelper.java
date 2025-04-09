package models;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;


public class UserQuizDatabaseHelper {
    private FirebaseFirestore db;
    private String userId;

    public UserQuizDatabaseHelper() {
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void saveQuizAttemptForQuiz(int chapterNumber, String quizName, QuizAttempt attempt, SaveCallback callback) {
        if (userId == null) {
            callback.onFailure("User not logged in.");
            return;
        }

        db.collection("UserQuiz")
                .document(userId)
                .collection("Chapters")
                .document("Chapter" + chapterNumber)
                .collection("Quizzes")
                .document(quizName)
                .collection("Attempts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int nextAttemptNumber = querySnapshot.size() + 1;
                    String attemptId = "attempt" + nextAttemptNumber;

                    db.collection("UserQuiz")
                            .document(userId)
                            .collection("Chapters")
                            .document("Chapter" + chapterNumber)
                            .collection("Quizzes")
                            .document(quizName)
                            .collection("Attempts")
                            .document(attemptId)
                            .set(attempt)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
