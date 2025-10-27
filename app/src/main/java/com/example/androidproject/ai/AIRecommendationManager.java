package com.example.androidproject.ai;

import android.content.Context;
import android.util.Log;

import com.example.androidproject.UserExploreActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AIRecommendationManager {
    private static final String TAG = "AIRecommendationManager";
    private final Context context;
    private final FirebaseFirestore db;
    private final OpenAIService openAIService;

    public AIRecommendationManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.openAIService = new OpenAIService();
    }

    public void getPersonalizedRecommendations(String userDescription, RecommendationCallback callback) {
        if (userDescription == null || userDescription.trim().isEmpty()) {
            callback.onError("User description is empty");
            return;
        }

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserExploreActivity.Event> allEvents = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UserExploreActivity.Event event = document.toObject(UserExploreActivity.Event.class);
                        event.setId(document.getId());
                        allEvents.add(event);
                    }

                    generateAIRecommendations(userDescription, allEvents, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching events: ", e);
                    callback.onError("Failed to load events");
                });
    }

    private void generateAIRecommendations(String userDescription, List<UserExploreActivity.Event> events, RecommendationCallback callback) {
        String prompt = createRecommendationPrompt(userDescription, events);

        openAIService.getRecommendations(prompt, new OpenAIService.AICallback() {
            @Override
            public void onSuccess(List<String> recommendedEventNames) {
                if (recommendedEventNames == null || recommendedEventNames.isEmpty()) {
                    callback.onSuccess(events); // fallback to all
                } else {
                    List<UserExploreActivity.Event> recommendedEvents = filterEventsByNames(events, recommendedEventNames);
                    callback.onSuccess(recommendedEvents);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "AI recommendation error: " + error);
                callback.onError(error);
            }
        });
    }

    private String createRecommendationPrompt(String userDescription, List<UserExploreActivity.Event> events) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("The user wrote: \"").append(userDescription).append("\".\n\n");
        prompt.append("Here are the available events:\n");

        for (UserExploreActivity.Event event : events) {
            prompt.append("- ").append(event.getEventName())
                    .append(": ")
                    .append(event.getDescription() != null ? event.getDescription() : "")
                    .append("\n");
        }

        prompt.append("\nSelect 3 to 5 event names that best match the user's interest. ")
                .append("Return only the event names separated by commas, without explanations.");

        return prompt.toString();
    }

    private List<UserExploreActivity.Event> filterEventsByNames(
            List<UserExploreActivity.Event> events,
            List<String> recommendedNames) {

        List<UserExploreActivity.Event> recommendedEvents = new ArrayList<>();
        for (UserExploreActivity.Event event : events) {
            for (String name : recommendedNames) {
                if (event.getEventName() != null &&
                        event.getEventName().toLowerCase().contains(name.toLowerCase().trim())) {
                    recommendedEvents.add(event);
                    break;
                }
            }
        }
        return recommendedEvents;
    }

    public interface RecommendationCallback {
        void onSuccess(List<UserExploreActivity.Event> recommendedEvents);
        void onError(String error);
    }
}
