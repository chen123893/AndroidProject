package com.example.androidproject.ai;

import android.util.Log;

import com.example.androidproject.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback;

public class OpenAIService {
    private static final String TAG = "OpenAIService";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // ✅ Load from BuildConfig (already verified)
    private static final String API_KEY = BuildConfig.OPENAI_API_KEY;

    private final OkHttpClient client;

    public OpenAIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            Log.e(TAG, "❌ Missing or empty OpenAI API key. Please check BuildConfig and Gradle setup.");
        } else {
            Log.d(TAG, "✅ OpenAI API key loaded (len=" + API_KEY.length() + ")");
        }
    }

    public void getRecommendations(String prompt, AICallback callback) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            callback.onError("Missing API key");
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a helpful assistant that recommends events based on user interests. " +
                            "Return only event names separated by commas."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt));

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 150);
            requestBody.put("temperature", 0.7);

            Log.d(TAG, "📡 Sending OpenAI request with prompt:\n" + prompt);

            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            requestBody.toString(),
                            MediaType.parse("application/json")
                    ))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "🚫 Network error: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "🌐 Response code: " + response.code());
                    Log.d(TAG, "📦 Raw response: " + responseBody);

                    if (!response.isSuccessful()) {
                        callback.onError("API error: " + response.code());
                        return;
                    }

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.optJSONArray("choices");

                        if (choices == null || choices.length() == 0) {
                            callback.onError("Empty AI response");
                            return;
                        }

                        JSONObject messageObj = choices.getJSONObject(0).optJSONObject("message");
                        if (messageObj == null) {
                            callback.onError("Invalid AI response structure");
                            return;
                        }

                        String content = messageObj.optString("content", "").trim();
                        Log.d(TAG, "💬 AI response content: " + content);

                        List<String> eventNames = parseEventNames(content);
                        callback.onSuccess(eventNames);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Parse error: " + e.getMessage());
                        callback.onError("Response parse error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Request creation error: " + e.getMessage());
            callback.onError("Request creation error: " + e.getMessage());
        }
    }

    private List<String> parseEventNames(String content) {
        List<String> eventNames = new ArrayList<>();
        try {
            if (content.contains(",")) {
                for (String name : content.split(",")) {
                    String cleaned = name.trim().replace("\"", "").replace("'", "").replace(".", "");
                    if (!cleaned.isEmpty()) eventNames.add(cleaned);
                }
            } else {
                // Single event name fallback
                if (!content.isEmpty()) eventNames.add(content);
            }
        } catch (Exception e) {
            Log.e(TAG, "⚠️ parseEventNames error: " + e.getMessage());
        }
        return eventNames;
    }

    public interface AICallback {
        void onSuccess(List<String> recommendedEventNames);
        void onError(String error);
    }
}
