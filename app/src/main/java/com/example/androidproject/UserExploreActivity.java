package com.example.androidproject;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserExploreActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<Event> eventList;
    private EventAdapter adapter;
    private int currentUserGender = -1; // -1 = not loaded, 0 = female, 1 = male

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_explore);

        Log.d("UserExplore", "Activity created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        recyclerView = findViewById(R.id.recycler_events);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        // Load current user's gender first, then load events
        loadCurrentUserGender();

        // Search button listener
        btnSearch.setOnClickListener(v -> searchEvents());
    }

    private void loadCurrentUserGender() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d("UserExplore", "Loading gender for user: " + userId);

        db.collection("user").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long gender = doc.getLong("gender");
                        currentUserGender = (gender != null) ? gender.intValue() : -1;
                        Log.d("UserExplore", "User gender loaded: " + currentUserGender);
                        // Now load events after gender is retrieved
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
                            Event event = doc.toObject(Event.class);
                            event.setId(doc.getId());

                            Log.d("UserExplore", "Event: " + event.getEventName() + ", GenderSpec: " + event.getGenderSpec());

                            // Filter by gender specification
                            if (shouldShowEvent(event)) {
                                eventList.add(event);
                            }
                        } catch (Exception e) {
                            Log.e("UserExplore", "Error parsing event: " + e.getMessage());
                            // Fallback to manual mapping if toObject fails
                            try {
                                Event fallbackEvent = new Event();
                                fallbackEvent.setId(doc.getId());
                                fallbackEvent.setAdminID(doc.getString("adminID"));
                                fallbackEvent.setDescription(doc.getString("description"));
                                fallbackEvent.setEndDateTime(doc.getString("endDateTime"));
                                fallbackEvent.setEventName(doc.getString("eventName"));
                                fallbackEvent.setGenderSpec(doc.getString("genderSpec"));
                                fallbackEvent.setStartDateTime(doc.getString("startDateTime"));
                                fallbackEvent.setVenue(doc.getString("venue"));

                                // Handle numeric fields
                                Long currentAttendees = doc.getLong("currentAttendees");
                                fallbackEvent.setCurrentAttendees(currentAttendees != null ? currentAttendees.intValue() : 0);

                                Long pax = doc.getLong("pax");
                                fallbackEvent.setPax(pax != null ? pax.intValue() : 0);

                                if (shouldShowEvent(fallbackEvent)) {
                                    eventList.add(fallbackEvent);
                                }
                            } catch (Exception ex) {
                                Log.e("UserExplore", "Error in fallback parsing: " + ex.getMessage());
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("UserExplore", "Events displayed: " + eventList.size());

                    // If no events loaded, show message
                    if (eventList.isEmpty()) {
                        Toast.makeText(UserExploreActivity.this, "No events found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserExplore", "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean shouldShowEvent(Event event) {
        // If genderSpec is null or empty, show to everyone
        if (event.getGenderSpec() == null || event.getGenderSpec().isEmpty()) {
            return true;
        }

        // If genderSpec is "All" or "Both", show to everyone
        String spec = event.getGenderSpec().toLowerCase();
        if (spec.equals("all") || spec.equals("both") || spec.equals("any")) {
            return true;
        }

        // If user gender not loaded, show all events
        if (currentUserGender == -1) {
            return true;
        }

        // Check if event matches user's gender
        if (currentUserGender == 1) { // Male
            return spec.equals("male") || spec.equals("1") || spec.equals("m");
        } else if (currentUserGender == 0) { // Female
            return spec.equals("female") || spec.equals("0") || spec.equals("f");
        }

        return true; // Default to showing if gender is unknown
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
                            Event event = doc.toObject(Event.class);
                            event.setId(doc.getId());

                            // Check if event matches search keyword and gender filter
                            if ((event.getEventName() != null && event.getEventName().toLowerCase().contains(keyword)) ||
                                    (event.getVenue() != null && event.getVenue().toLowerCase().contains(keyword)) ||
                                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(keyword))) {

                                if (shouldShowEvent(event)) {
                                    eventList.add(event);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("UserExplore", "Error parsing event in search: " + e.getMessage());
                            // Fallback to manual mapping
                            try {
                                Event fallbackEvent = new Event();
                                fallbackEvent.setId(doc.getId());
                                fallbackEvent.setAdminID(doc.getString("adminID"));
                                fallbackEvent.setDescription(doc.getString("description"));
                                fallbackEvent.setEndDateTime(doc.getString("endDateTime"));
                                fallbackEvent.setEventName(doc.getString("eventName"));
                                fallbackEvent.setGenderSpec(doc.getString("genderSpec"));
                                fallbackEvent.setStartDateTime(doc.getString("startDateTime"));
                                fallbackEvent.setVenue(doc.getString("venue"));

                                Long currentAttendees = doc.getLong("currentAttendees");
                                fallbackEvent.setCurrentAttendees(currentAttendees != null ? currentAttendees.intValue() : 0);

                                Long pax = doc.getLong("pax");
                                fallbackEvent.setPax(pax != null ? pax.intValue() : 0);

                                // Check search criteria
                                if ((fallbackEvent.getEventName() != null && fallbackEvent.getEventName().toLowerCase().contains(keyword)) ||
                                        (fallbackEvent.getVenue() != null && fallbackEvent.getVenue().toLowerCase().contains(keyword)) ||
                                        (fallbackEvent.getDescription() != null && fallbackEvent.getDescription().toLowerCase().contains(keyword))) {

                                    if (shouldShowEvent(fallbackEvent)) {
                                        eventList.add(fallbackEvent);
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e("UserExplore", "Error in fallback search parsing: " + ex.getMessage());
                            }
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
        private String adminID;
        private String description;
        private String endDateTime;
        private String eventName;
        private String genderSpec;
        private String startDateTime;
        private String venue;
        private int currentAttendees;
        private int pax;

        public Event() {} // Needed for Firestore

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getAdminID() { return adminID; }
        public void setAdminID(String adminID) { this.adminID = adminID; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }

        public String getGenderSpec() { return genderSpec; }
        public void setGenderSpec(String genderSpec) { this.genderSpec = genderSpec; }

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

                // Set event data to views
                holder.tvEventName.setText(event.getEventName() != null ? event.getEventName() : "Unnamed Event");
                holder.tvVenue.setText("Venue: " + (event.getVenue() != null ? event.getVenue() : "Not specified"));

                String startTime = event.getStartDateTime() != null ? event.getStartDateTime() : "Not set";
                String endTime = event.getEndDateTime() != null ? event.getEndDateTime() : "Not set";
                holder.tvDatetime.setText("Start: " + startTime + "\nEnd: " + endTime);

                holder.tvCapacity.setText(event.getCurrentAttendees() + " / " + event.getPax());

                // Check if event is full
                if (event.getCurrentAttendees() >= event.getPax()) {
                    holder.btnJoin.setText("Full");
                    holder.btnJoin.setEnabled(false);
                    holder.btnJoin.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                } else {
                    checkIfJoined(event, holder.btnJoin);
                }

                holder.btnJoin.setOnClickListener(v -> joinEvent(event, holder.btnJoin));

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
            TextView tvEventName, tvVenue, tvDatetime, tvCapacity;
            Button btnJoin;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    tvEventName = itemView.findViewById(R.id.tv_event_name);
                    tvVenue = itemView.findViewById(R.id.tv_event_venue);
                    tvDatetime = itemView.findViewById(R.id.tv_event_datetime);
                    tvCapacity = itemView.findViewById(R.id.tv_event_capacity);
                    btnJoin = itemView.findViewById(R.id.btn_join);

                } catch (Exception e) {
                    Log.e("EventViewHolder", "Error initializing views: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // ------------------ CHECK IF USER JOINED ------------------
        private void checkIfJoined(Event event, Button joinButton) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("attendance")
                    .whereEqualTo("eventID", event.getId())
                    .whereEqualTo("userID", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            joinButton.setText("Joined");
                            joinButton.setEnabled(false);
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                        } else {
                            joinButton.setText("Join");
                            joinButton.setEnabled(true);
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.holo_blue_light));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventAdapter", "Error checking attendance: " + e.getMessage());
                    });
        }

        // ------------------ JOIN EVENT ------------------
        private void joinEvent(Event event, Button joinButton) {
            String userId = mAuth.getCurrentUser().getUid();
            Log.d("EventAdapter", "Attempting to join event: " + event.getId() + " by user: " + userId);

            // Check if event is full before joining
            if (event.getCurrentAttendees() >= event.getPax()) {
                Toast.makeText(UserExploreActivity.this, "Event is full!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> attendance = new HashMap<>();
            attendance.put("eventID", event.getId());
            attendance.put("userID", userId);

            db.collection("attendance")
                    .add(attendance)
                    .addOnSuccessListener(docRef -> {
                        Log.d("EventAdapter", "Successfully joined event");
                        Toast.makeText(UserExploreActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();

                        // Update attendee count - FIXED: collection name should be "events" not "event"
                        DocumentReference eventRef = db.collection("events").document(event.getId());
                        eventRef.update("currentAttendees", event.getCurrentAttendees() + 1)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("EventAdapter", "Successfully updated attendee count");
                                    event.setCurrentAttendees(event.getCurrentAttendees() + 1);
                                    joinButton.setText("Joined");
                                    joinButton.setEnabled(false);
                                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(UserExploreActivity.this, android.R.color.darker_gray));
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("EventAdapter", "Error updating attendee count: " + e.getMessage());
                                    Toast.makeText(UserExploreActivity.this, "Joined but failed to update count", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventAdapter", "Failed to join event: " + e.getMessage());
                        Toast.makeText(UserExploreActivity.this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}