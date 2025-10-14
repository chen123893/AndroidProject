package com.example.androidproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName, venue, startDateTime, endDateTime, pax, description;
    private RadioGroup genderGroup;
    private Button createBtn;
    private FirebaseFirestore db;

    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateTimeFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();

        // Initialize calendars and formatter
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2); // Default end time: 2 hours from now
        dateTimeFormatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startDateTime = findViewById(R.id.startDateTime);
        endDateTime = findViewById(R.id.endDateTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        createBtn = findViewById(R.id.createBtn);

        // Set up date-time pickers
        setupDateTimePickers();

        createBtn.setOnClickListener(v -> createEvent());

        // Bottom navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_create_event);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(CreateEventActivity.this, MyEventsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_create_event) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(CreateEventActivity.this, AdminProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupDateTimePickers() {
        // Start Date-Time Picker
        startDateTime.setFocusable(false);
        startDateTime.setClickable(true);
        startDateTime.setOnClickListener(v -> showDateTimePicker(startDateTime, startCalendar, "Start Date & Time"));

        // End Date-Time Picker
        endDateTime.setFocusable(false);
        endDateTime.setClickable(true);
        endDateTime.setOnClickListener(v -> showDateTimePicker(endDateTime, endCalendar, "End Date & Time"));

        // REMOVED: Don't set initial values, let hint text show instead
    }

    private void showDateTimePicker(EditText dateTimeField, Calendar calendar, String title) {
        // First show date picker
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // After date is selected, show time picker
                    showTimePicker(dateTimeField, calendar, title);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.setTitle("Select " + title + " - Date");
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.show();
    }

    private void showTimePicker(EditText dateTimeField, Calendar calendar, String title) {
        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    dateTimeField.setText(dateTimeFormatter.format(calendar.getTime()));

                    // Validate that end date-time is after start date-time
                    if (dateTimeField == endDateTime && !endCalendar.after(startCalendar)) {
                        Toast.makeText(this, "End date-time must be after start date-time", Toast.LENGTH_SHORT).show();
                        endDateTime.setText(""); // Clear the invalid selection
                        endCalendar.setTime(startCalendar.getTime());
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12-hour format
        );

        timePicker.setTitle("Select " + title + " - Time");
        timePicker.show();
    }

    private void createEvent() {
        String name = eventName.getText().toString().trim();
        String loc = venue.getText().toString().trim();
        String start = startDateTime.getText().toString().trim();
        String end = endDateTime.getText().toString().trim();
        String paxNum = pax.getText().toString().trim();
        String desc = description.getText().toString().trim();

        if (name.isEmpty() || loc.isEmpty() || start.isEmpty() || end.isEmpty() || paxNum.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endCalendar.after(startCalendar)) {
            Toast.makeText(this, "End date-time must be after start date-time", Toast.LENGTH_SHORT).show();
            return;
        }

        int genderCode = 2;
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.male) genderCode = 0;
        else if (selectedId == R.id.female) genderCode = 1;

        String eventID = "E" + System.currentTimeMillis();

        Map<String, Object> event = new HashMap<>();
        event.put("eventID", eventID);
        event.put("eventName", name);
        event.put("venue", loc);
        event.put("startDateTime", start);
        event.put("endDateTime", end);
        event.put("pax", Integer.parseInt(paxNum));
        event.put("description", desc);
        event.put("genderSpec", genderCode);
        event.put("currentAttendees", 0);

        // Get the admin’s Firestore record
        // inside createEvent()
        String adminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("admin")
                .document(adminUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String adminID = doc.getString("adminID"); // ✅ use the same stored Axxxx
                        if (adminID != null) {
                            event.put("adminID", adminID);
                        } else {
                            Toast.makeText(this, "Admin ID not found. Please re-login.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    db.collection("events")
                            .document(eventID)
                            .set(event)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                                clearFields();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch admin ID: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }


    private void clearFields() {
        eventName.setText("");
        venue.setText("");
        startDateTime.setText(""); // Clear the text to show hint again
        endDateTime.setText("");   // Clear the text to show hint again
        pax.setText("");
        description.setText("");
        genderGroup.clearCheck();

        // Reset calendars but don't display the values
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2);
    }
}