package com.example.cognibyte.Client;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

public class OpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final String TAG = "OpenAIClient";
    private static final String PREFS_NAME = "LessonCompletionPrefs";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public interface CompletionCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public interface QuizCallback {
        void onSuccess(List<Map<String, Object>> questions);
        void onError(String error);
    }

    public static void generateLesson(int chapterNumber, int lessonNumber, String language, CompletionCallback callback) {
        String prompt = "Generate Lesson " + lessonNumber + " of Chapter " + chapterNumber + " for " + language + " student. " +
                "Ensure the lesson builds upon previous knowledge with clear explanations and examples. " +
                "Generate a concise lesson title and detailed lesson content (around 300-500 words). " +
                "Respond with a JSON object having keys 'title' and 'content'.";
        sendRequest(prompt, 700, new CompletionCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    Log.d("OpenAIClient", "Raw response: " + result);
                    JSONObject jsonResult = new JSONObject(result);
                    String title = jsonResult.getString("title");
                    String content = jsonResult.getString("content");
                    storeLessonInFirestore(chapterNumber, lessonNumber, language, title, "Summary of " + title, content);
                    callback.onSuccess("Title: " + title + "\n\nContent:\n" + content);
                } catch (Exception e) {
                    callback.onError("Error parsing lesson JSON: " + e.getMessage() + "\nRaw result: " + result);
                }
            }
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static void generateQuiz(int chapterNumber, int lessonNumber, String language, String lessonContent, QuizCallback callback) {
        String prompt = "Generate a JSON-formatted multiple-choice quiz with EXACTLY 10 questions for Lesson "
                + lessonNumber + " of Chapter " + chapterNumber + " in " + language + ". Each question must have 4 answer choices, with only one correct answer. " +
                "In addition, provide a short explanation for why the correct answer is correct. " +
                "Format the response as a JSON array with fields: 'question', 'options', 'answer', and 'explanation'. " +
                "Respond ONLY with the JSON array.";
        sendRequest(prompt + "\n" + lessonContent, 1200, new CompletionCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    String cleanedJson = extractJsonArray(result);
                    Log.d(TAG, "Cleaned JSON Response: " + cleanedJson);
                    if (cleanedJson.length() < 50) {
                        throw new Exception("JSON response is too short, possibly truncated.");
                    }
                    JSONArray quizArray = new JSONArray(cleanedJson);
                    List<Map<String, Object>> quizQuestions = new ArrayList<>();
                    if (quizArray.length() < 10) {
                        Log.e(TAG, "Warning: Only " + quizArray.length() + " questions generated for " + language + " Lesson " + lessonNumber);
                    }
                    for (int i = 0; i < quizArray.length(); i++) {
                        JSONObject questionJson = quizArray.getJSONObject(i);
                        Map<String, Object> question = new HashMap<>();
                        question.put("question", questionJson.getString("question"));
                        question.put("options", convertJsonArrayToList(questionJson.getJSONArray("options")));
                        question.put("answer", questionJson.getString("answer"));
                        if (questionJson.has("explanation") && !questionJson.getString("explanation").trim().isEmpty()) {
                            question.put("explanation", questionJson.getString("explanation").trim());
                        } else {
                            question.put("explanation", "No explanation provided.");
                        }
                        quizQuestions.add(question);
                    }
                    while (quizQuestions.size() < 10) {
                        Map<String, Object> fillerQuestion = new HashMap<>();
                        fillerQuestion.put("question", "Placeholder Question");
                        fillerQuestion.put("options", List.of("A", "B", "C", "D"));
                        fillerQuestion.put("answer", "A");
                        fillerQuestion.put("explanation", "Placeholder explanation.");
                        quizQuestions.add(fillerQuestion);
                    }
                    Log.d(TAG, "Final Quiz Size: " + quizQuestions.size() + " questions for " + language + " Lesson " + lessonNumber);
                    callback.onSuccess(quizQuestions);
                } catch (Exception e) {
                    Log.e(TAG, "Parsing error for " + language + " Lesson " + lessonNumber + ": " + e.getMessage());
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Quiz generation failed for " + language + " Lesson " + lessonNumber + ": " + error);
                callback.onError(error);
            }
        });
    }

    public static void saveQuizToFirestore(int chapterNumber, int lessonNumber, String language, String lessonTitle, String skillLevel, String chapterId, List<Map<String, Object>> questions) {
        Log.d(TAG, "Saving quiz to Firestore under QuizContent/" + language + "/Quizzes/");
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("quizId", "");
        quizData.put("chapterId", chapterId);
        quizData.put("chapterNumber", chapterNumber);
        quizData.put("lessonNumber", lessonNumber);
        quizData.put("lessonTitle", lessonTitle);
        quizData.put("skillLevel", skillLevel);
        quizData.put("questions", questions);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference parentDoc = firestore.collection("QuizContent").document(language);
        parentDoc.set(new HashMap<>(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    DocumentReference quizDoc = parentDoc.collection("Quizzes").document();
                    String quizId = quizDoc.getId();
                    quizData.put("quizId", quizId);
                    Log.d(TAG, "Storing quiz at path: /QuizContent/" + language + "/Quizzes/" + quizId);
                    quizDoc.set(quizData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Quiz stored successfully with ID: " + quizId))
                            .addOnFailureListener(e -> Log.e(TAG, "Error storing quiz: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create parent document in QuizContent: " + e.getMessage()));
    }

    public static void generateAnswerExplanation(String question, String correctAnswer, CompletionCallback callback) {
        String prompt = "Explain why the following answer is correct in a simple and educational way:\n" +
                "Question: " + question + "\n" +
                "Correct Answer: " + correctAnswer + "\n" +
                "Provide a short but clear explanation.";
        sendRequest(prompt, 150, callback);
    }

    private static void sendRequest(String prompt, int maxTokens, CompletionCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");
            jsonObject.put("temperature", 0.3);
            jsonObject.put("max_tokens", maxTokens);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful AI tutor."));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            jsonObject.put("messages", messages);
        } catch (Exception e) {
            handler.post(() -> callback.onError("JSON error: " + e.getMessage()));
            return;
        }
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        String content = choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content").trim();
                        handler.post(() -> callback.onSuccess(content));
                    } catch (Exception e) {
                        handler.post(() -> callback.onError("Parsing error: " + e.getMessage()));
                    }
                } else {
                    handler.post(() -> callback.onError("API error: " + response.code() + " - " + response.message()));
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.onError("Request failed: " + e.getMessage()));
            }
        });
    }

    private static void storeLessonInFirestore(int chapterNumber, int lessonNumber, String language, String title, String summary, String content) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> chapterData = new HashMap<>();
        chapterData.put("chapterNumber", chapterNumber);
        chapterData.put("lessonNumber", lessonNumber);
        chapterData.put("lessonTitle", title);
        chapterData.put("lessonContent", content);
        chapterData.put("summary", summary);
        chapterData.put("lastEdit", com.google.firebase.firestore.FieldValue.serverTimestamp());

        DocumentReference parentDoc = firestore.collection("ChapterContent").document(language);
        parentDoc.set(new HashMap<>(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    parentDoc.collection("Chapters")
                            .whereEqualTo("chapterNumber", chapterNumber)
                            .whereEqualTo("lessonNumber", lessonNumber)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                    String docId = document.getId();
                                    chapterData.put("chapterId", docId);
                                    document.getReference().set(chapterData, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Lesson updated successfully with ID: " + docId))
                                            .addOnFailureListener(e -> Log.e(TAG, "Error updating lesson: " + e.getMessage()));
                                } else {
                                    DocumentReference docRef = parentDoc.collection("Chapters").document();
                                    String docId = docRef.getId();
                                    chapterData.put("chapterId", docId);
                                    Log.d(TAG, "Storing new lesson at path: /ChapterContent/" + language + "/Chapters/" + docId);
                                    docRef.set(chapterData, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Lesson stored successfully with ID: " + docId))
                                            .addOnFailureListener(e -> Log.e(TAG, "Error storing lesson: " + e.getMessage()));
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error checking for existing lesson: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create parent document: " + e.getMessage()));
    }

    private static String extractJsonArray(String response) {
        try {
            response = response.trim();
            response = response.replaceAll("```json", "");
            response = response.replaceAll("```", "");
            response = response.trim();
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                throw new Exception("Invalid JSON array format.");
            }
            String jsonArray = response.substring(startIndex, endIndex + 1);
            new JSONArray(jsonArray);
            return jsonArray;
        } catch (Exception e) {
            return "[]";
        }
    }

    public static void generateCodingQuiz(String language, String lessonNumber, CompletionCallback callback) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference quizRef = firestore.collection("CodingQuiz")
                .document(language)
                .collection(lessonNumber)
                .document("quiz");
        quizRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String question = documentSnapshot.getString("question");
                String sampleSolution = documentSnapshot.getString("sampleSolution");
                String explanation = documentSnapshot.getString("explanation");
                String result = "Question: " + question + "\n\nSample Solution:\n" + sampleSolution + "\n\nExplanation:\n" + explanation;
                callback.onSuccess(result);
            } else {
                String prompt = "Generate a beginner-level coding question in " + language +
                        ". Provide a question, a sample correct solution, and a brief explanation. " +
                        "Format your response in JSON with keys: question, sampleSolution, explanation.";
                sendRequest(prompt, 500, new CompletionCallback() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            JSONObject jsonResponse = new JSONObject(result);
                            String question = jsonResponse.getString("question");
                            String sampleSolution = jsonResponse.getString("sampleSolution");
                            String explanation = jsonResponse.getString("explanation");
                            Map<String, Object> quizData = new HashMap<>();
                            quizData.put("question", question);
                            quizData.put("sampleSolution", sampleSolution);
                            quizData.put("explanation", explanation);
                            quizRef.set(quizData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> callback.onSuccess("Quiz saved and retrieved:\n" + result))
                                    .addOnFailureListener(e -> callback.onError("Failed to save quiz: " + e.getMessage()));
                        } catch (Exception e) {
                            callback.onError("Parsing error: " + e.getMessage());
                        }
                    }
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }
        }).addOnFailureListener(e -> callback.onError("Error fetching quiz: " + e.getMessage()));
    }

    public static void markLessonCompleted(int lessonNumber) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot update lesson progress.");
            return;
        }
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> progress = new HashMap<>();
        progress.put("lesson_" + lessonNumber, true);
        firestore.collection("LessonProgress")
                .document(userId)
                .set(progress, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Lesson " + lessonNumber + " marked as completed"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update lesson progress: " + e.getMessage()));
    }

    public static boolean isLessonCompleted(Context context, int lessonNumber) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("lesson_" + lessonNumber, false);
    }

    public static boolean canStartLesson2(Context context) {
        return isLessonCompleted(context, 1);
    }

    private static List<String> convertJsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.optString(i));
        }
        return list;
    }
}
