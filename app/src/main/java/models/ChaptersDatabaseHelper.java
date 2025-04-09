package models;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class ChaptersDatabaseHelper {
    private FirebaseFirestore db;

    public ChaptersDatabaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveChapter(String language, Chapters chapter, SaveCallback callback) {
        DocumentReference parentDocRef = db.collection("ChapterContent").document(language);
        parentDocRef.set(new java.util.HashMap<>(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    DocumentReference docRef = parentDocRef.collection("Chapters").document();
                    chapter.setChapterId(docRef.getId());
                    docRef.set(chapter)
                            .addOnSuccessListener(unused2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to create parent document: " + e.getMessage()));
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
