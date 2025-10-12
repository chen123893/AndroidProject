package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName, venue, startTime, endTime, pax, description;
    private RadioGroup genderGroup;
    private Button createBtn;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();

        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        createBtn = findViewById(R.id.createBtn);

        createBtn.setOnClickListener(v -> createEvent());


        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_create_event);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(CreateEventActivity.this, AdminProfileActivity.class)); // replace with AdminMyEventsActivity if you have one
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_create_event) {
                return true; // already here
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(CreateEventActivity.this, AdminProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });


        bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    private void createEvent() {
        String name = eventName.getText().toString().trim();
        String loc = venue.getText().toString().trim();
        String start = startTime.getText().toString().trim();
        String end = endTime.getText().toString().trim();
        String paxNum = pax.getText().toString().trim();
        String desc = description.getText().toString().trim();

        if (name.isEmpty() || loc.isEmpty() || start.isEmpty() || end.isEmpty() || paxNum.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int genderCode = 2; // Default None
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.male) genderCode = 0;
        else if (selectedId == R.id.female) genderCode = 1;

        Map<String, Object> event = new HashMap<>();
        event.put("eventName", name);
        event.put("venue", loc);
        event.put("startTime", start);
        event.put("endTime", end);
        event.put("pax", Integer.parseInt(paxNum));
        event.put("description", desc);
        event.put("genderSpec", genderCode);
        event.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());

        db.collection("events").add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        eventName.setText("");
        venue.setText("");
        startTime.setText("");
        endTime.setText("");
        pax.setText("");
        description.setText("");
        genderGroup.clearCheck();
    }
}
