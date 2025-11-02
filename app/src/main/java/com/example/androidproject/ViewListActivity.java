package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ViewListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout attendeesContainer;
    private String eventID;
    private ImageButton backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_list);

        backBtn = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();
        attendeesContainer = findViewById(R.id.attendees_container);

        Intent intent = getIntent();
        if (intent != null) {
            eventID = intent.getStringExtra("eventID");
            Log.d("ViewListActivity", "Received eventID: " + eventID);
        }

        if (eventID == null || eventID.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAttendees();

        // Bottom Navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_my_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(ViewListActivity.this, MyEventsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }else if (id == R.id.nav_create_event) {
                startActivity(new Intent(ViewListActivity.this, CreateEventActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(ViewListActivity.this, AdminProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        backBtn.setOnClickListener(v -> {
            startActivity(new Intent(ViewListActivity.this, MyEventsActivity.class));
            finish();
        });
    }

    private void loadAttendees() {
        Log.d("ViewListActivity", "Loading attendees for eventID: " + eventID);

        db.collection("attendance")
                .whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    attendeesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        Log.d("ViewListActivity", "No attendance records found for eventID: " + eventID);
                        TextView empty = new TextView(this);
                        empty.setText("No attendees yet.");
                        empty.setTextColor(getResources().getColor(android.R.color.black));
                        empty.setTextSize(16f);
                        attendeesContainer.addView(empty);
                        return;
                    }

                    Log.d("ViewListActivity", "Found " + snapshot.size() + " attendance records");

                    // Loop through all attendance documents
                    for (QueryDocumentSnapshot attendanceDoc : snapshot) {
                        String attendanceDocID = attendanceDoc.getId();
                        String userID = attendanceDoc.getString("userID");

                        Log.d("ViewListActivity", "Processing attendance: " + attendanceDocID + ", userID: " + userID);

                        if (userID == null || userID.isEmpty()) {
                            Log.w("ViewListActivity", "Attendance doc missing userID: " + attendanceDocID);
                            continue;
                        }

                        // Find the matching user by userID
                        db.collection("user")
                                .whereEqualTo("userID", userID)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (!userSnapshot.isEmpty()) {
                                        for (DocumentSnapshot userDoc : userSnapshot) {
                                            addAttendeeCard(userDoc, attendanceDocID);
                                            Log.d("ViewListActivity", "Added attendee card for userID: " + userID);
                                        }
                                    } else {
                                        Log.w("ViewListActivity", "No user found with userID: " + userID);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Log.e("ViewListActivity", "Error loading user with userID: " + userID, e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewListActivity", "Failed to load attendees", e);
                    Toast.makeText(this, "Failed to load attendees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addAttendeeCard(DocumentSnapshot userDoc, String attendanceDocID) {
        View attendeeCard = getLayoutInflater().inflate(R.layout.item_attendee_card, attendeesContainer, false);

        ImageView profilePic = attendeeCard.findViewById(R.id.profile_pic);
        TextView usernameView = attendeeCard.findViewById(R.id.username);
        TextView emailView = attendeeCard.findViewById(R.id.email);
        ImageButton removeBtn = attendeeCard.findViewById(R.id.remove_button);

        // Get fields safely
        String name = userDoc.getString("name");
        String email = userDoc.getString("email");
        String phone = userDoc.getString("phoneNumber");
        String desc = userDoc.getString("description");
        String profileVal = userDoc.getString("profilePic");

        // Populate fields
        usernameView.setText("Name: " + (name != null ? name : "Unknown"));
        emailView.setText("Email: " + (email != null ? email : "N/A"));

        if (profileVal != null && (profileVal.startsWith("http://") || profileVal.startsWith("https://"))) {
            Glide.with(this).load(profileVal).into(profilePic);
        } else {
            profilePic.setImageResource(avatarResForKey(profileVal));
        }

        removeBtn.setOnClickListener(v -> removeAttendee(attendanceDocID, attendeeCard));

        attendeesContainer.addView(attendeeCard);
    }

    private void removeAttendee(String attendanceDocID, View cardView) {
        db.collection("attendance").document(attendanceDocID)
                .delete()
                .addOnSuccessListener(unused -> {
                    attendeesContainer.removeView(cardView);
                    Toast.makeText(this, "Attendee removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove attendee: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int avatarResForKey(String key) {
        if (key == null) return R.drawable.tofu; // default
        switch (key.toLowerCase()) {
            case "profile1": return R.drawable.profile1;
            case "profile2": return R.drawable.profile2;
            case "profile3": return R.drawable.profile3;
            case "default":
            default:         return R.drawable.tofu;
        }
    }

}
