package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView eventsRecyclerView;
    private List<Event> events;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        events = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadAdminEvents();
        setupBottomNavigation();
    }

    private void initializeViews() {
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
    }

    private void setupRecyclerView() {
        // Create a simple adapter for now - we'll add click listeners later
        EventAdapter eventAdapter = new EventAdapter(events);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(eventAdapter);
    }

    private void loadAdminEvents() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No logged in user", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminId = currentUser.getUid();

        db.collection("events")
                .whereEqualTo("createdBy", adminId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        events.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            events.add(event);
                        }

                        // Update the adapter
                        EventAdapter eventAdapter = new EventAdapter(events);
                        eventsRecyclerView.setAdapter(eventAdapter);

                        if (events.isEmpty()) {
                            Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNavigation() {
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
    }
}