package com.example.androidproject;

import android.app.AlertDialog;
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

/**Main activity for users to explore and browse events*/
public class UserExploreActivity extends AppCompatActivity {

    // UI components
    private EditText searchInput;
    private Button btnSearch, btnAIRecommendations;
    private RecyclerView recyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data management
    private ArrayList<Event> eventList;
    private EventAdapter adapter;
    private int currentUserGender = -1; // -1 = not loaded, 0 = female, 1 = male
    private String userDescription = "";

    // AI functionality
    private AIRecommendationManager aiRecommendationManager;
    private boolean showingAIRecommendations = false;

    // Loading dialog
    private AlertDialog loadingDialog;

    /**Initialize activity and set up main components*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_explore);

        // Initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        aiRecommendationManager = new AIRecommendationManager(this);

        // Set up UI components
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();

        // Load user data and events
        loadCurrentUserProfile();

        // Set up button click listeners
        btnSearch.setOnClickListener(v -> searchEvents());
        btnAIRecommendations.setOnClickListener(v -> toggleAIRecommendations());
    }

    /**Initialize all UI views by finding them from layout*/
    private void initializeViews() {
        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        btnAIRecommendations = findViewById(R.id.btn_ai_recommendations);
        recyclerView = findViewById(R.id.recycler_events);
    }

    /**Set up RecyclerView with layout manager and adapter*/
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_explore);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) {
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(this, UserTimetableActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return false;
        });
    }

    /**Toggle between AI recommendations and all events*/
    private void toggleAIRecommendations() {
        if (showingAIRecommendations) {
            // Switch back to all events
            loadAllEvents();
            btnAIRecommendations.setText("✨ Get AI Recommendations");
            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
            showingAIRecommendations = false;
        } else {
            // Get AI recommendations
            getAIRecommendations();
        }
    }


    private void showLoading(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    private void loadCurrentUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("user").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Extract user gender
                        Long gender = doc.getLong("gender");
                        currentUserGender = (gender != null) ? gender.intValue() : -1;

                        // Extract user description for AI recommendations
                        userDescription = doc.getString("description");
                        if (userDescription == null) userDescription = "";

                        // Update AI button state based on description availability
                        updateAIButtonState();
                        loadAllEvents();
                    } else {
                        loadAllEvents();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                    loadAllEvents(); // Load events even if profile fails
                });
    }

    /**Enable or disable AI recommendations button based on user description availability*/
    private void updateAIButtonState() {
        if (!TextUtils.isEmpty(userDescription)) {
            // User has description - enable AI button
            btnAIRecommendations.setEnabled(true);
            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
        } else {
            // No description - disable AI button
            btnAIRecommendations.setEnabled(false);
            btnAIRecommendations.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            btnAIRecommendations.setText("No user description for AI");
        }
    }

    /**Load all events from Firestore and display them*/
    private void loadAllEvents() {
        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = parseEventFromDocument(doc);
                        if (event != null && shouldShowEvent(event)) {
                            // Initialize with 0 attendees, then fetch actual count
                            event.setCurrentAttendees(0);
                            eventList.add(event);
                            fetchLiveAttendeeCount(event);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    /**Fetch real-time attendee count for a specific event from attendance collection*/
    private void fetchLiveAttendeeCount(Event event) {
        db.collection("attendance")
                .whereEqualTo("eventID", event.getEventID())
                .get()
                .addOnSuccessListener(snapshot -> {
                    event.setCurrentAttendees(snapshot.size());
                    adapter.notifyDataSetChanged();
                });
    }

    /**Get AI-powered personalized event recommendations*/
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

                    // Update event list with AI recommendations
                    eventList.clear();
                    eventList.addAll(recommendedEvents);
                    adapter.notifyDataSetChanged();

                    if (recommendedEvents.isEmpty()) {
                        Toast.makeText(UserExploreActivity.this, "No specific recommendations found", Toast.LENGTH_LONG).show();
                        loadAllEvents(); // Fallback to all events
                    } else {
                        Toast.makeText(UserExploreActivity.this, "AI found " + recommendedEvents.size() + " recommendations!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    btnAIRecommendations.setEnabled(true);
                    showingAIRecommendations = false;
                    Toast.makeText(UserExploreActivity.this, "AI recommendations unavailable", Toast.LENGTH_SHORT).show();
                    loadAllEvents(); // Fallback to all events
                });
            }
        });
    }

    //return event object
    private Event parseEventFromDocument(QueryDocumentSnapshot doc) {
        Event event = new Event();
        try {
            // Set basic event properties
            event.setId(doc.getId());
            event.setEventID(doc.getString("eventID"));
            event.setAdminID(doc.getString("adminID"));
            event.setDescription(doc.getString("description"));
            event.setEndDateTime(doc.getString("endDateTime"));
            event.setEventName(doc.getString("eventName"));
            event.setStartDateTime(doc.getString("startDateTime"));
            event.setVenue(doc.getString("venue"));
            event.setImageName(doc.getString("imageName"));

            // Handle numeric fields with null checks
            Long genderSpec = doc.getLong("genderSpec");
            event.setGenderSpec(genderSpec != null ? genderSpec.intValue() : 2); // Default to "Any"

            Long currentAttendees = doc.getLong("currentAttendees");
            event.setCurrentAttendees(currentAttendees != null ? currentAttendees.intValue() : 0);

            Long pax = doc.getLong("pax");
            event.setPax(pax != null ? pax.intValue() : 0);

            return event;
        } catch (Exception e) {
            Log.e("UserExplore", "Error parsing event document: " + e.getMessage());
            return null;
        }
    }

    /** Check if event should be shown based on gender restrictions*/
    private boolean shouldShowEvent(Event event) {
        // Show event if: no gender restriction OR user gender not loaded OR gender matches
        if (event.getGenderSpec() == 2 || currentUserGender == -1) {
            return true;
        }
        return event.getGenderSpec() == currentUserGender;
    }

    /**Search events based on keyword in event name, venue, or description*/
    private void searchEvents() {
        String keyword = searchInput.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(keyword)) {
            loadAllEvents(); // Show all events if search is empty
            return;
        }

        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = parseEventFromDocument(doc);
                        if (event != null && shouldShowEvent(event) && matchesSearch(event, keyword)) {
                            eventList.add(event);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (eventList.isEmpty()) {
                        Toast.makeText(this, "No events found for: " + keyword, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show();
                });
    }

    /**Check if event matches search keyword*/
    private boolean matchesSearch(Event event, String keyword) {
        return (event.getEventName() != null && event.getEventName().toLowerCase().contains(keyword)) ||
                (event.getVenue() != null && event.getVenue().toLowerCase().contains(keyword)) ||
                (event.getDescription() != null && event.getDescription().toLowerCase().contains(keyword));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!showingAIRecommendations) {
            loadAllEvents(); // Reload events unless showing AI recommendations
        }
    }

    /**Event data model representing an event in the system*/
    public static class Event {
        private String id;
        private String eventID;
        private String adminID;
        private String description;
        private String endDateTime;
        private String eventName;
        private int genderSpec;
        private String startDateTime;
        private String venue;
        private int currentAttendees;
        private int pax;
        private String imageName;

        public Event() {}

        // Getter and setter methods for all properties
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

        public String getImageName() { return imageName; }
        public void setImageName(String imageName) { this.imageName = imageName; }
    }

    /**RecyclerView adapter for displaying events*/
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
            Event event = events.get(position);
            holder.bind(event);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        /**ViewHolder for event items*/
        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvVenue, tvDatetime, tvCapacity, tvAIRecBadge;
            Button btnJoin;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                // Initialize all view components
                tvEventName = itemView.findViewById(R.id.tv_event_name);
                tvVenue = itemView.findViewById(R.id.tv_event_venue);
                tvDatetime = itemView.findViewById(R.id.tv_event_datetime);
                tvCapacity = itemView.findViewById(R.id.tv_event_capacity);
                btnJoin = itemView.findViewById(R.id.btn_join);
                tvAIRecBadge = itemView.findViewById(R.id.tv_ai_recommendation_badge);
            }

            /**Bind event data to views*/
            void bind(Event event) {
                // Set event information
                tvEventName.setText(event.getEventName() != null ? event.getEventName() : "Unnamed Event");
                tvVenue.setText("Venue: " + (event.getVenue() != null ? event.getVenue() : "Not specified"));

                // Format date/time display
                String startTime = event.getStartDateTime() != null ? event.getStartDateTime() : "Not set";
                String endTime = event.getEndDateTime() != null ? event.getEndDateTime() : "Not set";
                tvDatetime.setText("Start: " + startTime + "\nEnd: " + endTime);

                // Display capacity
                tvCapacity.setText(event.getCurrentAttendees() + " / " + event.getPax());

                // Show AI recommendation badge if applicable
                tvAIRecBadge.setVisibility(showingAIRecommendations ? View.VISIBLE : View.GONE);

                // Handle join button state
                if (event.getCurrentAttendees() >= event.getPax()) {
                    // Event is full
                    btnJoin.setText("Full");
                    btnJoin.setEnabled(false);
                    btnJoin.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                } else {
                    // Event has space - check if user already joined
                    checkIfJoined(event, btnJoin);
                }

                // Set up join button click listener
                btnJoin.setOnClickListener(v -> joinEvent(event, btnJoin));

                // Set up item click listener for event details
                itemView.setOnClickListener(v -> openEventDetails(event));
            }

            /**Open event details screen*/
            private void openEventDetails(Event event) {
                Intent intent = new Intent(UserExploreActivity.this, ExploreDetailsActivity.class);
                // Pass all event data to details activity
                intent.putExtra("eventName", event.getEventName());
                intent.putExtra("venue", event.getVenue());
                intent.putExtra("startDateTime", event.getStartDateTime());
                intent.putExtra("endDateTime", event.getEndDateTime());
                intent.putExtra("description", event.getDescription());
                intent.putExtra("pax", event.getPax());
                intent.putExtra("currentAttendees", event.getCurrentAttendees());
                intent.putExtra("imageName", event.getImageName());
                startActivity(intent);
            }
        }

        /**Check if current user has already joined the event*/
        private void checkIfJoined(Event event, Button joinButton) {
            String firebaseUid = mAuth.getCurrentUser().getUid();

            db.collection("user").document(firebaseUid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String customUserID = userDoc.getString("userID");
                            if (customUserID == null) customUserID = firebaseUid; // Fallback to Firebase UID

                            // Check attendance collection for user's registration
                            db.collection("attendance")
                                    .whereEqualTo("eventID", event.getEventID())
                                    .whereEqualTo("userID", customUserID)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            // User has already joined
                                            joinButton.setText("Joined");
                                            joinButton.setEnabled(false);
                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                                        } else {
                                            // User can join
                                            joinButton.setText("Join");
                                            joinButton.setEnabled(true);
                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, R.color.colorPrimary));
                                        }
                                    });
                        }
                    });
        }

        /**Handle event joining process*/
        private void joinEvent(Event event, Button joinButton) {
            String firebaseUid = mAuth.getCurrentUser().getUid();

            db.collection("user").document(firebaseUid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (!userDoc.exists()) return;

                        String customUserID = userDoc.getString("userID");
                        if (customUserID == null) return;

                        // Create attendance record
                        Map<String, Object> attendance = new HashMap<>();
                        attendance.put("eventID", event.getEventID());
                        attendance.put("userID", customUserID);

                        // Add to attendance collection
                        db.collection("attendance").add(attendance)
                                .addOnSuccessListener(docRef -> {
                                    // Update event attendee count
                                    DocumentReference eventRef = db.collection("events").document(event.getId());
                                    eventRef.update("currentAttendees", event.getCurrentAttendees() + 1)
                                            .addOnSuccessListener(aVoid -> {
                                                // Update local event object and UI
                                                event.setCurrentAttendees(event.getCurrentAttendees() + 1);
                                                joinButton.setText("Joined");
                                                joinButton.setEnabled(false);
                                                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                                                notifyDataSetChanged(); // Refresh all items
                                                Toast.makeText(UserExploreActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(UserExploreActivity.this, "Failed to join", Toast.LENGTH_SHORT).show();
                                });
                    });
        }
    }
}