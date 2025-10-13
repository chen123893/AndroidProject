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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ViewListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout attendeesContainer;
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_list);

        db = FirebaseFirestore.getInstance();
        attendeesContainer = findViewById(R.id.attendees_container);

        // ✅ Get event ID from intent
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
    }

    private void loadAttendees() {
        db.collection("attendance")
                .whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    attendeesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No attendees yet.");
                        attendeesContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String attendanceDocID = doc.getId();
                        String userID = doc.getString("userID");
                        if (userID == null) continue;

                        db.collection("users")
                                .whereEqualTo("userID", userID)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (!userSnapshot.isEmpty()) {
                                        for (QueryDocumentSnapshot userDoc : userSnapshot) {
                                            View attendeeCard = getLayoutInflater().inflate(R.layout.item_attendee_card, attendeesContainer, false);

                                            ImageView profilePic = attendeeCard.findViewById(R.id.profile_pic);
                                            TextView usernameView = attendeeCard.findViewById(R.id.username);
                                            TextView emailView = attendeeCard.findViewById(R.id.email);
                                            TextView genderView = attendeeCard.findViewById(R.id.gender);
                                            ImageButton removeBtn = attendeeCard.findViewById(R.id.remove_button);

                                            String name = userDoc.getString("name");
                                            String email = userDoc.getString("email");
                                            String profileUrl = userDoc.getString("profilePic");
                                            Long gender = userDoc.getLong("gender");

                                            usernameView.setText("Username: " + (name != null ? name : userID));
                                            emailView.setText("Email: " + (email != null ? email : "N/A"));
                                            genderView.setText("Gender: " + ((gender != null && gender == 1) ? "Male" : "Female"));

                                            if (profileUrl != null && !profileUrl.isEmpty()) {
                                                Glide.with(this).load(profileUrl).into(profilePic);
                                            } else {
                                                profilePic.setImageResource(R.drawable.tofu); // fallback image
                                            }

                                            removeBtn.setOnClickListener(v -> removeAttendee(attendanceDocID, attendeeCard));

                                            attendeesContainer.addView(attendeeCard);
                                        }
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to load user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load attendees: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeAttendee(String attendanceDocID, View cardView) {
        db.collection("attendance").document(attendanceDocID)
                .delete()
                .addOnSuccessListener(unused -> {
                    attendeesContainer.removeView(cardView);
                    Toast.makeText(this, "Attendee removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove attendee: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
