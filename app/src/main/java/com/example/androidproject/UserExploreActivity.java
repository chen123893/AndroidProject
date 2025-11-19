package com.example.androidproject;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidproject.ai.AIRecommendationManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserExploreActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button btnSearch, btnAIRecommendations;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<Event> eventList;
    private EventAdapter adapter;
    private int currentUserGender = -1; // -1 = not loaded, 0 = female, 1 = male
    private String userDescription = "";
    private AIRecommendationManager aiRecommendationManager;
    private boolean showingAIRecommendations = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_explore);

        Log.d("UserExplore", "Activity created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize AI Recommendation Manager
        aiRecommendationManager = new AIRecommendationManager(this);

        // Initialize views
        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        btnAIRecommendations = findViewById(R.id.btn_ai_recommendations);
        recyclerView = findViewById(R.id.recycler_events);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        // Setup bottom navigation
        setupBottomNavigation();

        // Load current user's gender and description first, then load events
        loadCurrentUserProfile();

        // Search button listener
        btnSearch.setOnClickListener(v -> searchEvents());

        // AI Recommendations button listener
        btnAIRecommendations.setOnClickListener(v -> {
            if (showingAIRecommendations) {
                // If already showing AI recommendations, switch back to all events
                loadAllEvents();
                btnAIRecommendations.setText("✨ Get AI Recommendations");
                btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
                showingAIRecommendations = false;
                Toast.makeText(this, "Showing all events", Toast.LENGTH_SHORT).show();
            } else {
                // Get AI recommendations
                getAIRecommendations();
            }
        });
    }

    private void toggleAIRecommendations() {
        if (showingAIRecommendations) {
            loadAllEvents();
            btnAIRecommendations.setText("Get AI Recommendations");
            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
            showingAIRecommendations = false;
            Toast.makeText(this, "Showing all events", Toast.LENGTH_SHORT).show();
        } else {
            getAIRecommendations();
        }
    }

    private AlertDialog loadingDialog;

    private void showLoading(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        builder.setTitle(message);
        builder.setView(progressBar);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_explore);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) {
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(UserExploreActivity.this, UserTimetableActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(UserExploreActivity.this, UserProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadCurrentUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d("UserExplore", "Loading profile for user: " + userId);

        db.collection("user").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long gender = doc.getLong("gender");
                        currentUserGender = (gender != null) ? gender.intValue() : -1;

                        // Get user description for AI recommendations
                        userDescription = doc.getString("description");
                        if (userDescription == null) {
                            userDescription = "";
                        }

                        Log.d("UserExplore", "User profile loaded. Gender: " + currentUserGender + ", Description: " + userDescription);

                        // Enable AI button if user has description
                        if (!TextUtils.isEmpty(userDescription)) {
                            btnAIRecommendations.setEnabled(true);
                            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
                        } else {
                            btnAIRecommendations.setEnabled(false);
                            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                            btnAIRecommendations.setText("No user description for AI");
                        }

                        // Now load events after profile is retrieved
                        loadAllEvents();
                    } else {
                        Log.e("UserExplore", "User profile not found in Firestore");
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        loadAllEvents(); // Load anyway
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserExplore", "Failed to load user profile: " + e.getMessage());
                    Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                    loadAllEvents(); // Load anyway
                });
    }

    private void loadAllEvents() {
        Log.d("UserExplore", "Loading all events");

        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("UserExplore", "Events loaded: " + querySnapshot.size());
                    eventList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Event event = parseEventFromDocument(doc);

                            if (event != null && shouldShowEvent(event)) {
                                // Temporarily add with default count = 0
                                event.setCurrentAttendees(0);
                                eventList.add(event);

                                // 🔹 Fetch live attendee count from attendance
                                db.collection("attendance")
                                        .whereEqualTo("eventID", event.getEventID()) // use Exxxx ID
                                        .get()
                                        .addOnSuccessListener(snapshot -> {
                                            int liveCount = snapshot.size();
                                            event.setCurrentAttendees(liveCount);
                                            adapter.notifyDataSetChanged(); // update UI
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("UserExplore", "Failed to fetch live count: " + e.getMessage()));
                            }
                        } catch (Exception e) {
                            Log.e("UserExplore", "Error parsing event: " + e.getMessage());
                        }
                    }

                    adapter.notifyDataSetChanged();
                    Log.d("UserExplore", "Events displayed: " + eventList.size());

                    if (eventList.isEmpty()) {
                        Toast.makeText(UserExploreActivity.this, "No events found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserExplore", "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void getAIRecommendations() {
        if (TextUtils.isEmpty(userDescription)) {
            Toast.makeText(this, "No user description found for AI recommendations", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Analyzing your interests...");
        btnAIRecommendations.setEnabled(false);

        aiRecommendationManager.getPersonalizedRecommendations(userDescription, new AIRecommendationManager.RecommendationCallback() {
            @Override
            public void onSuccess(List<Event> recommendedEvents) {
                runOnUiThread(() -> {
                    hideLoading();
                    showingAIRecommendations = true;
                    btnAIRecommendations.setEnabled(true);
                    btnAIRecommendations.setText("Show All Events");
                    btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, R.color.green));

                    eventList.clear();
                    eventList.addAll(recommendedEvents);
                    adapter.notifyDataSetChanged();

                    if (recommendedEvents.isEmpty()) {
                        Toast.makeText(UserExploreActivity.this,
                                "No specific recommendations found. Showing all events.",
                                Toast.LENGTH_LONG).show();
                        loadAllEvents();
                    } else {
                        Toast.makeText(UserExploreActivity.this,
                                "AI found " + recommendedEvents.size() + " personalized recommendations!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    btnAIRecommendations.setEnabled(true);
                    btnAIRecommendations.setText("Get AI Recommendations");
                    showingAIRecommendations = false;
                    Toast.makeText(UserExploreActivity.this,
                            "AI recommendations unavailable. Showing all events.",
                            Toast.LENGTH_SHORT).show();
                    loadAllEvents();
                });
            }
        });
    }

    private Event parseEventFromDocument(QueryDocumentSnapshot doc) {
        Event event = new Event();
        try {
            event.setId(doc.getId());
            event.setEventID(doc.getString("eventID"));
            event.setAdminID(doc.getString("adminID"));
            event.setDescription(doc.getString("description"));
            event.setEndDateTime(doc.getString("endDateTime"));
            event.setEventName(doc.getString("eventName"));
            event.setStartDateTime(doc.getString("startDateTime"));
            event.setVenue(doc.getString("venue"));
            event.setImageName(doc.getString("imageName"));
            // Handle genderSpec
            Long genderSpec = doc.getLong("genderSpec");
            if (genderSpec != null) {
                event.setGenderSpec(genderSpec.intValue());
            } else {
                event.setGenderSpec(2); // Default to "None"
            }

            // Handle numeric fields
            Long currentAttendees = doc.getLong("currentAttendees");
            event.setCurrentAttendees(currentAttendees != null ? currentAttendees.intValue() : 0);

            Long pax = doc.getLong("pax");
            event.setPax(pax != null ? pax.intValue() : 0);

            return event;
        } catch (Exception e) {
            Log.e("UserExplore", "Error in event parsing: " + e.getMessage());
            return null;
        }
    }


    private boolean shouldShowEvent(Event event) {
        // If genderSpec is 2 (None/Any), show to everyone
        if (event.getGenderSpec() == 2) {
            return true;
        }

        // If user gender not loaded, show all events
        if (currentUserGender == -1) {
            return true;
        }

        // Check if event matches user's gender
        // genderSpec: 0 = Male, 1 = Female, 2 = Any/None
        return event.getGenderSpec() == currentUserGender;
    }

    private void searchEvents() {
        String keyword = searchInput.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(keyword)) {
            loadAllEvents();
            return;
        }

        Log.d("UserExplore", "Searching events with keyword: " + keyword);

        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("UserExplore", "Search results: " + querySnapshot.size());
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Event event = parseEventFromDocument(doc);
                            if (event != null) {
                                // Check if event matches search keyword and gender filter
                                boolean matchesSearch = (event.getEventName() != null && event.getEventName().toLowerCase().contains(keyword)) ||
                                        (event.getVenue() != null && event.getVenue().toLowerCase().contains(keyword)) ||
                                        (event.getDescription() != null && event.getDescription().toLowerCase().contains(keyword));

                                if (matchesSearch && shouldShowEvent(event)) {
                                    eventList.add(event);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("UserExplore", "Error parsing event in search: " + e.getMessage());
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("UserExplore", "Search events displayed: " + eventList.size());

                    if (eventList.isEmpty()) {
                        Toast.makeText(UserExploreActivity.this, "No events found for: " + keyword, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserExplore", "Search failed: " + e.getMessage());
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------------- EVENT MODEL -----------------------
    public static class Event {
        private String id;
        private String eventID; // Your custom eventID field
        private String adminID;
        private String description;
        private String endDateTime;
        private String eventName;
        private int genderSpec; // Changed to int: 0=Male, 1=Female, 2=None/Any
        private String startDateTime;
        private String venue;
        private int currentAttendees;
        private int pax;

        private String imageName;
        public String getImageName() { return imageName; }
        public void setImageName(String imageName) { this.imageName = imageName; }

        public Event() {} // Needed for Firestore

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEventID() { return eventID; }
        public void setEventID(String eventID) { this.eventID = eventID; }

        public String getAdminID() { return adminID; }
        public void setAdminID(String adminID) { this.adminID = adminID; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }

        public int getGenderSpec() { return genderSpec; }
        public void setGenderSpec(int genderSpec) { this.genderSpec = genderSpec; }

        public String getStartDateTime() { return startDateTime; }
        public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }

        public int getCurrentAttendees() { return currentAttendees; }
        public void setCurrentAttendees(int currentAttendees) { this.currentAttendees = currentAttendees; }

        public int getPax() { return pax; }
        public void setPax(int pax) { this.pax = pax; }
    }

    // ---------------------- ADAPTER ---------------------------
    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

        private final ArrayList<Event> events;

        EventAdapter(ArrayList<Event> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            try {
                Event event = events.get(position);

                holder.tvEventName.setText(event.getEventName() != null ? event.getEventName() : "Unnamed Event");
                holder.tvVenue.setText("Venue: " + (event.getVenue() != null ? event.getVenue() : "Not specified"));

                String startTime = event.getStartDateTime() != null ? event.getStartDateTime() : "Not set";
                String endTime = event.getEndDateTime() != null ? event.getEndDateTime() : "Not set";
                holder.tvDatetime.setText("Start: " + startTime + "\nEnd: " + endTime);

                holder.tvCapacity.setText(event.getCurrentAttendees() + " / " + event.getPax());

                if (showingAIRecommendations) {
                    holder.tvAIRecBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.tvAIRecBadge.setVisibility(View.GONE);
                }

                if (event.getCurrentAttendees() >= event.getPax()) {
                    holder.btnJoin.setText("Full");
                    holder.btnJoin.setEnabled(false);
                    holder.btnJoin.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                } else {
                    checkIfJoined(event, holder.btnJoin);
                }

                holder.btnJoin.setOnClickListener(v -> joinEvent(event, holder.btnJoin));

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(UserExploreActivity.this, ExploreDetailsActivity.class);
                    intent.putExtra("eventName", event.getEventName());
                    intent.putExtra("venue", event.getVenue());
                    intent.putExtra("startDateTime", event.getStartDateTime());
                    intent.putExtra("endDateTime", event.getEndDateTime());
                    intent.putExtra("description", event.getDescription());
                    intent.putExtra("pax", event.getPax());
                    intent.putExtra("currentAttendees", event.getCurrentAttendees());
                    intent.putExtra("imageName", event.getImageName());
                    startActivity(intent);
                });

            } catch (Exception e) {
                Log.e("EventAdapter", "Error binding view holder: " + e.getMessage());
                e.printStackTrace();
            }
        }


        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvVenue, tvDatetime, tvCapacity, tvAIRecBadge;
            Button btnJoin;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    tvEventName = itemView.findViewById(R.id.tv_event_name);
                    tvVenue = itemView.findViewById(R.id.tv_event_venue);
                    tvDatetime = itemView.findViewById(R.id.tv_event_datetime);
                    tvCapacity = itemView.findViewById(R.id.tv_event_capacity);
                    btnJoin = itemView.findViewById(R.id.btn_join);

                    // Add AI recommendation badge - you'll need to add this TextView to your event_item.xml
                    tvAIRecBadge = itemView.findViewById(R.id.tv_ai_recommendation_badge);

                } catch (Exception e) {
                    Log.e("EventViewHolder", "Error initializing views: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // ------------------ CHECK IF USER JOINED ------------------
        private void checkIfJoined(Event event, Button joinButton) {
            String firebaseUid = mAuth.getCurrentUser().getUid();
            String eventDocId = event.getId(); // Firestore document ID

            // Step 1: Find user's custom userID ("Uxxxxxx")
            db.collection("user")
                    .document(firebaseUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String customUserID = userDoc.getString("userID");
                            if (customUserID == null) {
                                Log.w("EventAdapter", "User missing custom userID field!");
                                customUserID = firebaseUid; // fallback
                            }

                            // Step 2: Check attendance using the custom userID
                            db.collection("attendance")
                                    .whereEqualTo("eventID", event.getEventID())  // if you use custom eventID (Exxxxx)
                                    .whereEqualTo("userID", customUserID)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            joinButton.setText("Joined");
                                            joinButton.setEnabled(false);
                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                                        } else {
                                            joinButton.setText("Join");
                                            joinButton.setEnabled(true);
                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, R.color.colorPrimary));
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e("EventAdapter", "Error checking attendance: " + e.getMessage()));
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("EventAdapter", "Error fetching user custom ID: " + e.getMessage()));
        }


        // ------------------ JOIN EVENT ------------------
        private void joinEvent(Event event, Button joinButton) {
            String firebaseUid = mAuth.getCurrentUser().getUid();

            // Get user’s custom ID (Uxxxx)
            db.collection("user").document(firebaseUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (!userDoc.exists()) {
                            Toast.makeText(UserExploreActivity.this, "User record not found!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String customUserID = userDoc.getString("userID");
                        if (customUserID == null) {
                            Toast.makeText(UserExploreActivity.this, "Missing userID in profile!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String eventDocId = event.getId(); // Firestore doc ID
                        String eventCustomID = event.getEventID(); // e.g., E1760...

                        // Step 2: Save attendance with eventID = Exxxxx and userID = Uxxxxx
                        Map<String, Object> attendance = new HashMap<>();
                        attendance.put("eventID", eventCustomID); // use the Exxxx one for consistency
                        attendance.put("userID", customUserID);   // use Uxxxx

                        db.collection("attendance")
                                .add(attendance)
                                .addOnSuccessListener(docRef -> {
                                    Log.d("EventAdapter", "Successfully joined event");

                                    // Step 3: Update event count
                                    DocumentReference eventRef = db.collection("events").document(eventDocId);
                                    eventRef.update("currentAttendees", event.getCurrentAttendees() + 1)
                                            .addOnSuccessListener(aVoid -> {
                                                event.setCurrentAttendees(event.getCurrentAttendees() + 1);
                                                joinButton.setText("Joined");
                                                joinButton.setEnabled(false);
                                                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                                                notifyDataSetChanged();
                                                Toast.makeText(UserExploreActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(UserExploreActivity.this, "Joined but failed to update count", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(UserExploreActivity.this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Log.e("EventAdapter", "Error fetching user custom ID: " + e.getMessage()));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!showingAIRecommendations) {
            loadAllEvents();
        }
    }
}