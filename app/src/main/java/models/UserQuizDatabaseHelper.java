package models;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class UserQuizDatabaseHelper {
    private final FirebaseFirestore db;
    private final String userId;

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public UserQuizDatabaseHelper() {
        this.db = FirebaseFirestore.getInstance();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void saveQuizAttemptForQuiz(int chapterNumber, String chapterTitle, int lessonNumber, String lessonTitle, String language, QuizAttempt attempt, SaveCallback callback) {
        if (userId == null) {
            callback.onFailure("User not logged in.");
            return;
        }

        CollectionReference attemptsRef = db
                .collection("UserQuiz")
                .document(userId)
                .collection("Attempts");

        attemptsRef.get()
                .addOnSuccessListener(qs -> {
                    int next = qs.size() + 1;
                    String attemptId = "attempt" + next;
                    DocumentReference attemptDoc = attemptsRef.document(attemptId);

                    Map<String, Object> attemptData = new HashMap<>();
                    attemptData.put("attemptNumber", next);
                    attemptData.put("userId", userId);
                    attemptData.put("language", language);
                    attemptData.put("chapterTitle", chapterTitle);
                    attemptData.put("chapterNumber", chapterNumber);
                    attemptData.put("lessonTitle", lessonTitle);
                    attemptData.put("lessonNumber", lessonNumber);
                    attemptData.put("questionsAsked", attempt.getQuestionsAsked());
                    attemptData.put("wrongQuestions", attempt.getWrongQuestions());
                    attemptData.put("score", attempt.getScore());
                    attemptData.put("timestamp", FieldValue.serverTimestamp());

                    attemptDoc.set(attemptData)
                            .addOnSuccessListener(a -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Couldn't fetch attempts: " + e.getMessage()));
    }
}
