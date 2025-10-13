package com.example.androidproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_explore);

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

        // Load all events initially
        loadAllEvents();

        // Search button listener
        btnSearch.setOnClickListener(v -> searchEvents());
    }

    private void loadAllEvents() {
        db.collection("event").get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event e = doc.toObject(Event.class);
                        e.setId(doc.getId());
                        eventList.add(e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    private void searchEvents() {
        String keyword = searchInput.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            loadAllEvents();
            return;
        }

        db.collection("event")
                .whereGreaterThanOrEqualTo("eventName", keyword)
                .whereLessThanOrEqualTo("eventName", keyword + '\uf8ff')
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event e = doc.toObject(Event.class);
                        e.setId(doc.getId());
                        eventList.add(e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show());
    }

    // ---------------------- EVENT MODEL -----------------------
    public static class Event {
        private String id, adminID, description, endDateTime, eventName, genderSpec, startDateTime, venue;
        private int currentAttendees, pax;

        public Event() {} // Needed for Firestore

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEventName() { return eventName; }
        public String getVenue() { return venue; }
        public String getStartDateTime() { return startDateTime; }
        public String getEndDateTime() { return endDateTime; }
        public int getCurrentAttendees() { return currentAttendees; }
        public int getPax() { return pax; }
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
            Event event = events.get(position);
            holder.tvEventName.setText(event.getEventName());
            holder.tvVenue.setText("Venue: " + event.getVenue());
            holder.tvDatetime.setText("Start: " + event.getStartDateTime() + "\nEnd: " + event.getEndDateTime());
            holder.tvCapacity.setText(event.getCurrentAttendees() + " / " + event.getPax());

            checkIfJoined(event, holder.btnJoin);

            holder.btnJoin.setOnClickListener(v -> joinEvent(event, holder.btnJoin));
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
                tvEventName = itemView.findViewById(R.id.tv_event_name);
                tvVenue = itemView.findViewById(R.id.tv_event_venue);
                tvDatetime = itemView.findViewById(R.id.tv_event_datetime);
                tvCapacity = itemView.findViewById(R.id.tv_event_capacity);
                btnJoin = itemView.findViewById(R.id.btn_join);
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
                            joinButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                        } else {
                            joinButton.setText("Join");
                            joinButton.setEnabled(true);
                            joinButton.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
                        }
                    });
        }

        // ------------------ JOIN EVENT ------------------
        private void joinEvent(Event event, Button joinButton) {
            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> attendance = new HashMap<>();
            attendance.put("eventID", event.getId());
            attendance.put("userID", userId);

            db.collection("attendance")
                    .add(attendance)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(UserExploreActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();

                        // Update attendee count
                        DocumentReference eventRef = db.collection("event").document(event.getId());
                        eventRef.get().addOnSuccessListener(snapshot -> {
                            int current = snapshot.getLong("currentAttendees").intValue();
                            eventRef.update("currentAttendees", current + 1);
                            joinButton.setText("Joined");
                            joinButton.setEnabled(false);
                            joinButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                        });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(UserExploreActivity.this, "Failed to join", Toast.LENGTH_SHORT).show());
        }
    }
}
