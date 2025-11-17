package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MyEventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout eventsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        db = FirebaseFirestore.getInstance();
        eventsContainer = findViewById(R.id.events_container);

        loadEvents();

        // Bottom Navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_my_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) return true;
            else if (id == R.id.nav_create_event) {
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
    }

    private void loadEvents() {
        String adminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch adminID (e.g., "A001") first
        db.collection("admin")
                .document(adminUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String adminID = doc.getString("adminID");

                        if (adminID != null) {
                            db.collection("events")
                                    .whereEqualTo("adminID", adminID)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        eventsContainer.removeAllViews();

                                        if (querySnapshot.isEmpty()) {
                                            Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        LayoutInflater inflater = LayoutInflater.from(this);

                                        for (QueryDocumentSnapshot eventDoc : querySnapshot) {
                                            View eventCard = inflater.inflate(R.layout.item_event_card, eventsContainer, false);

                                            // Bind layout views
                                            TextView name = eventCard.findViewById(R.id.tvEventName);
                                            TextView venue = eventCard.findViewById(R.id.tvVenue);
                                            TextView startEnd = eventCard.findViewById(R.id.tvStartEndTime);
                                            TextView paxCount = eventCard.findViewById(R.id.tvPax);
                                            Button viewBtn = eventCard.findViewById(R.id.btnViewList);
                                            Button editBtn = eventCard.findViewById(R.id.btnEditEvent);

                                            // Retrieve Firestore fields
                                            String eventName = eventDoc.getString("eventName");
                                            String eventVenue = eventDoc.getString("venue");
                                            String eventStart = eventDoc.getString("startDateTime");
                                            String eventEnd = eventDoc.getString("endDateTime");
                                            String eventID = eventDoc.getString("eventID");
                                            Long maxPax = eventDoc.getLong("pax");

                                            // Display base data
                                            name.setText(eventName != null ? eventName : "Unnamed Event");
                                            venue.setText(eventVenue != null ? "Venue: " + eventVenue : "Venue: N/A");
                                            if (eventStart != null && eventEnd != null) {
                                                startEnd.setText(eventStart + " - " + eventEnd);
                                            } else {
                                                startEnd.setText("Date/time not set");
                                            }

                                            // Count attendees dynamically from attendance collection
                                            if (eventID != null) {
                                                AggregateQuery countQuery = db.collection("attendance")
                                                        .whereEqualTo("eventID", eventID)
                                                        .count();

                                                countQuery.get(AggregateSource.SERVER)
                                                        .addOnSuccessListener((AggregateQuerySnapshot snapshot) -> {
                                                            long count = snapshot.getCount();
                                                            long totalPax = maxPax != null ? maxPax : 0;
                                                            paxCount.setText(count + " / " + totalPax);
                                                        })
                                                        .addOnFailureListener(e ->
                                                                paxCount.setText("0 / " + (maxPax != null ? maxPax : 0)));

                                            }

                                            // View attendee list
                                            viewBtn.setOnClickListener(v -> {
                                                Intent intent = new Intent(MyEventsActivity.this, ViewListActivity.class);
                                                intent.putExtra("eventID", eventID);
                                                startActivity(intent);
                                            });

                                            // Edit event
                                            editBtn.setOnClickListener(v -> {
                                                Intent intent = new Intent(MyEventsActivity.this, EditEventActivity.class);
                                                intent.putExtra("eventID", eventID);
                                                startActivity(intent);
                                            });

                                            eventsContainer.addView(eventCard);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "Admin ID not found in record.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch admin info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}