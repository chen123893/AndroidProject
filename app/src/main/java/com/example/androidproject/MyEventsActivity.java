package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class MyEventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout eventsContainer;
    private String adminUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        eventsContainer = findViewById(R.id.events_container);
        db = FirebaseFirestore.getInstance();

        // Get logged-in admin's UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            adminUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            adminUID = "TEST_ADMIN_UID"; // fallback
        }

        // Bottom navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_my_events);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                return true;
            } else if (id == R.id.nav_create_event) {
                startActivity(new Intent(MyEventsActivity.this, CreateEventActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(MyEventsActivity.this, AdminProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        loadAdminEvents();
    }

    private void loadAdminEvents() {
        db.collection("events")
                .whereEqualTo("adminID", adminUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyMsg = new TextView(this);
                        emptyMsg.setText("No events created yet.");
                        emptyMsg.setTextColor(android.graphics.Color.parseColor("#000000"));
                        emptyMsg.setTextSize(16f);
                        eventsContainer.addView(emptyMsg);
                        return;
                    }

                    // Sort and group by start date
                    Map<String, java.util.List<QueryDocumentSnapshot>> groupedEvents = new java.util.TreeMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String startDateTime = (String) doc.get("startDateTime");
                        if (startDateTime == null) startDateTime = "Unknown Date";

                        String dateKey = startDateTime.split(",")[0];
                        groupedEvents.computeIfAbsent(dateKey, k -> new java.util.ArrayList<>()).add(doc);
                    }

                    // Display by date
                    for (String date : groupedEvents.keySet()) {
                        // Add date header
                        TextView dateHeader = new TextView(this);
                        dateHeader.setText(date);
                        dateHeader.setTextSize(18f);
                        dateHeader.setTextColor(android.graphics.Color.parseColor("#4A3AFF"));
                        dateHeader.setPadding(8, 16, 8, 8);
                        dateHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        eventsContainer.addView(dateHeader);

                        for (QueryDocumentSnapshot doc : groupedEvents.get(date)) {
                            // ✅ Use your custom eventID field - make it final
                            final String eventID; // Declare as final
                            String tempEventID = doc.getString("eventID");
                            if (tempEventID == null || tempEventID.isEmpty()) {
                                eventID = doc.getId(); // fallback
                            } else {
                                eventID = tempEventID;
                            }

                            Map<String, Object> eventData = doc.getData();

                            String eventName = (String) eventData.get("eventName");
                            String venue = (String) eventData.get("venue");
                            String startDate = (String) eventData.get("startDateTime");
                            String endDate = (String) eventData.get("endDateTime");
                            long pax = eventData.get("pax") != null ? (long) eventData.get("pax") : 0L;

                            View eventCard = getLayoutInflater().inflate(R.layout.item_event_card, eventsContainer, false);

                            TextView nameView = eventCard.findViewById(R.id.tvEventName);
                            TextView venueView = eventCard.findViewById(R.id.tvVenue);
                            TextView timeView = eventCard.findViewById(R.id.tvStartEndTime);
                            TextView paxView = eventCard.findViewById(R.id.tvPax);

                            nameView.setText(eventName);
                            venueView.setText("Venue: " + venue);
                            timeView.setText("From: " + startDate + "\nTo:   " + endDate);

                            countAttendees(eventID, paxView, pax);

                            Button editBtn = eventCard.findViewById(R.id.btnEditEvent);
                            editBtn.setOnClickListener(v -> {
                                Intent intent = new Intent(MyEventsActivity.this, EditEventActivity.class);
                                intent.putExtra("eventID", eventID);  // ✅ consistent key
                                startActivity(intent);
                            });

                            Button viewBtn = eventCard.findViewById(R.id.btnViewList);
                            viewBtn.setOnClickListener(v -> {
                                Intent intent = new Intent(MyEventsActivity.this, ViewListActivity.class);
                                intent.putExtra("eventID", eventID);  // ✅ consistent key
                                startActivity(intent);
                            });

                            eventsContainer.addView(eventCard);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MyEventsActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void countAttendees(String eventID, TextView attendeeView, long pax) {
        CollectionReference attendanceRef = db.collection("attendance");

        attendanceRef.whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    attendeeView.setText(count + " / " + pax + " attending");
                })
                .addOnFailureListener(e -> attendeeView.setText("0 / " + pax + " attending"));
    }
}