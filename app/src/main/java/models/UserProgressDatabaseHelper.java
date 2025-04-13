package models;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class UserProgressDatabaseHelper {

    private FirebaseFirestore db;

    public UserProgressDatabaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void markLessonCompleted(String userId, String chapterId, int chapterNumber, int lessonNumber, boolean progress, SaveCallback callback) {
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("chapterId", chapterId);
        progressData.put("chapterNumber", chapterNumber);
        progressData.put("lessonNumber", lessonNumber);
        progressData.put("progress", progress);

        db.collection("UserProgress")
                .document(userId)
                .collection("Chapters")
                .document(chapterId)
                .set(progressData, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
