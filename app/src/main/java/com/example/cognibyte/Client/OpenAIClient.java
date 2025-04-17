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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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

    public static void generateLesson(int chapterNumber, int lessonNumber, String chapterTitle, String lessonTitle, String description, String language, CompletionCallback callback) {
        String prompt = "You are creating Lesson " + lessonNumber + " of Chapter " + chapterNumber
                + " (\"" + chapterTitle + "\") titled \"" + lessonTitle + "\" for " + language + " learners.\n\nDescription: " + description
                + "\n\nPlease generate a concise lesson title and detailed lesson content (300–500 words) " + "that builds on prior knowledge with clear explanations and examples. "
                + "Respond *only* with a JSON object with keys:\n" + "  • \"title\": your generated lesson title\n" + "  • \"content\": the full lesson text";

        sendRequest(prompt, 700, new CompletionCallback() {
            @Override
            public void onSuccess(String lessonResponse) {
                try {
                    JSONObject lessonJson = new JSONObject(lessonResponse);
                    String generatedTitle = lessonJson.getString("title");
                    String content = lessonJson.getString("content");

                    String recapPrompt = "Summarize Lesson " + lessonNumber + " of Chapter " + chapterNumber
                            + " in 3 simple sentences for beginner " + language + " learners:\n\n"
                            + content;

                    sendRequest(recapPrompt, 150, new CompletionCallback() {
                        @Override
                        public void onSuccess(String recap) {
                            storeLessonInFirestore(chapterNumber, lessonNumber, chapterTitle, lessonTitle, description, language, generatedTitle, content, recap);
                            callback.onSuccess(
                                    "Lesson and recap saved.\n\n"
                                            + "Title: " + generatedTitle + "\n\n"
                                            + "Recap:\n" + recap
                            );
                        }
                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to generate recap: " + error);
                        }
                    });

                } catch (Exception e) {
                    callback.onError("Error parsing lesson JSON: " + e.getMessage()
                            + "\nRaw response: " + lessonResponse);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to generate lesson: " + error);
            }
        });
    }

    public static void generateQuiz(int chapterNumber, int lessonNumber, String language, String skillLevel, String lessonContent, QuizCallback callback) {
        String prompt = "Generate a JSON-formatted multiple-choice quiz with EXACTLY 10 questions for "
                + "Lesson " + lessonNumber + " of Chapter " + chapterNumber + " in " + language + " at the " + skillLevel + " level. "
                + "Each question must have 4 answer choices, with only one correct answer, " + "and include a brief explanation. "
                + "Format the response as a JSON array of objects with keys 'question', 'options', 'answer', and 'explanation'.\n\n" + lessonContent;

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

    public static void saveQuizToFirestore(int chapterNumber, int lessonNumber, String language, String chapterTitle, String lessonTitle, String skillLevel, String chapterId, List<Map<String,Object>> questions) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference quizDoc = firestore
                .collection("QuizContent")
                .document(language)
                .collection("Quizzes")
                .document();
        String quizId = quizDoc.getId();

        Map<String,Object> quizData = new HashMap<>();
        quizData.put("quizId", quizId);
        quizData.put("chapterId", chapterId);
        quizData.put("chapterNumber", chapterNumber);
        quizData.put("lessonNumber", lessonNumber);
        quizData.put("chapterTitle", chapterTitle);
        quizData.put("lessonTitle", lessonTitle);
        quizData.put("skillLevel", skillLevel);
        quizData.put("lastEdit", FieldValue.serverTimestamp());

        List<Map<String,Object>> sanitized = new ArrayList<>();
        for (Map<String,Object> q : questions) {
            Map<String,Object> m = new HashMap<>();
            m.put("question", q.get("question"));
            m.put("options", q.get("options"));
            m.put("answer", q.get("answer"));
            m.put("explanation", q.get("explanation"));
            sanitized.add(m);
        }
        quizData.put("questions", sanitized);

        quizDoc.set(quizData, SetOptions.merge())
                .addOnSuccessListener(__ -> {
                    Log.d(TAG, "Quiz saved: " + quizId);
                    CollectionReference qCol = quizDoc.collection("Questions");
                    for (int i = 0; i < sanitized.size(); i++) {
                        final int num = i + 1;
                        qCol.document(String.valueOf(num))
                                .set(sanitized.get(i), SetOptions.merge())
                                .addOnSuccessListener(___ ->
                                        Log.d(TAG, "Saved question #" + num))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to save question #"+num+": "+e.getMessage()));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save quiz: " + e.getMessage()));
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

    private static void storeLessonInFirestore(int chapterNumber, int lessonNumber, String chapterTitle, String lessonTitle, String description, String language, String generatedTitle, String content, String recap) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference parentDoc = firestore
                .collection("ChapterContent")
                .document(language);
        CollectionReference chaptersCol = parentDoc.collection("Chapters");

        parentDoc.set(new HashMap<>(), SetOptions.merge())
                .addOnSuccessListener(unused ->
                        chaptersCol
                                .whereEqualTo("chapterNumber", chapterNumber)
                                .whereEqualTo("lessonNumber",  lessonNumber)
                                .get()
                                .addOnSuccessListener(found -> {
                                    Map<String,Object> data = new HashMap<>();
                                    data.put("chapterNumber",  chapterNumber);
                                    data.put("lessonNumber",   lessonNumber);
                                    data.put("chapterTitle",   chapterTitle);
                                    data.put("lessonTitle",    lessonTitle);
                                    data.put("description",    description);
                                    data.put("generatedTitle", generatedTitle);
                                    data.put("lessonContent",  content);
                                    data.put("lessonRecap",    recap);
                                    data.put("lastEdit",       FieldValue.serverTimestamp());

                                    if (!found.isEmpty()) {
                                        DocumentReference existing = found.getDocuments().get(0).getReference();
                                        data.put("chapterId", existing.getId());
                                        existing.set(data, SetOptions.merge());
                                    } else {
                                        DocumentReference newDoc = chaptersCol.document();
                                        data.put("chapterId", newDoc.getId());
                                        newDoc.set(data, SetOptions.merge());
                                    }
                                })
                );
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
