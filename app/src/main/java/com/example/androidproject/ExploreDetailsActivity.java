package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ExploreDetailsActivity extends AppCompatActivity {

    private ImageView ivEventImage;
    private TextView tvEventName, tvVenue, tvStart, tvEnd, tvDescription, tvCapacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        // --- Bind Views ---
        ivEventImage   = findViewById(R.id.iv_event_image);
        tvEventName    = findViewById(R.id.tv_event_name);
        tvVenue        = findViewById(R.id.tv_event_venue);
        tvStart        = findViewById(R.id.tv_event_start);
        tvEnd          = findViewById(R.id.tv_event_end);
        tvDescription  = findViewById(R.id.tv_event_description);
        tvCapacity     = findViewById(R.id.tv_event_capacity);

        // --- Setup Bottom Nav ---
        setupBottomNavigation();

        // --- Get Data from Intent ---
        String eventName       = getIntent().getStringExtra("eventName");
        String venue           = getIntent().getStringExtra("venue");
        String startDateTime   = getIntent().getStringExtra("startDateTime");
        String endDateTime     = getIntent().getStringExtra("endDateTime");
        String description     = getIntent().getStringExtra("description");
        int pax                = getIntent().getIntExtra("pax", 0);
        int currentAttendees   = getIntent().getIntExtra("currentAttendees", 0);
        String imageName       = getIntent().getStringExtra("imageName");

        // --- Set Texts Safely ---
        tvEventName.setText(eventName != null ? eventName : "Untitled Event");
        tvVenue.setText("Venue: " + (venue != null ? venue : "N/A"));
        tvStart.setText("Start: " + (startDateTime != null ? startDateTime : "-"));
        tvEnd.setText("End: " + (endDateTime != null ? endDateTime : "-"));
        tvDescription.setText("Description: " + (description != null && !description.isEmpty() ?
                description : "No description available."));
        tvCapacity.setText("Attendees: " + currentAttendees + " / " + pax);

        // --- Load Local Drawable Image by Name ---
        int resId = 0;
        if (imageName != null && !imageName.trim().isEmpty()) {
            //replace everything to underscore
            String safeName = imageName.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
            resId = getResources().getIdentifier(safeName, "drawable", getPackageName());
        }
        if (resId != 0) {
            ivEventImage.setImageResource(resId);
        } else {
            ivEventImage.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_explore);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) {
                startActivity(new Intent(this, UserExploreActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(this, UserTimetableActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
