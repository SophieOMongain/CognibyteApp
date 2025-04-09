package com.example.cognibyte.Client;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class JDoodleClient {
    private static final String API_URL = "https://api.jdoodle.com/v1/execute";
    private static final String CLIENT_ID = "2579f7905eb1e4c868b61fb02a4178e6";
    private static final String CLIENT_SECRET = "be375b17abec56cfcc43394d2602d1086bcba63dec4bc89edb3495013f68967e";

    public interface JDoodleCallback {
        void onSuccess(String output);
        void onError(String error);
    }

    public static void executeCode(String script, String language, JDoodleCallback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("clientId", CLIENT_ID);
            jsonObject.put("clientSecret", CLIENT_SECRET);
            jsonObject.put("script", script);
            jsonObject.put("language", language);
            jsonObject.put("versionIndex", "0");

            RequestBody body = RequestBody.create(MediaType.get("application/json"), jsonObject.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError("API Error: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Request Failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("JSON Error: " + e.getMessage());
        }
    }
}
