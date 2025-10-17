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
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2);
        dateTimeFormatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startDateTime = findViewById(R.id.startDateTime);
        endDateTime = findViewById(R.id.endDateTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        createBtn = findViewById(R.id.createBtn);

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
        startDateTime.setFocusable(false);
        startDateTime.setOnClickListener(v -> showDateTimePicker(startDateTime, startCalendar, "Start Date & Time"));

        endDateTime.setFocusable(false);
        endDateTime.setOnClickListener(v -> showDateTimePicker(endDateTime, endCalendar, "End Date & Time"));
    }

    private void showDateTimePicker(EditText field, Calendar calendar, String title) {
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker(field, calendar, title);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.setTitle("Select " + title + " - Date");
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.show();
    }

    private void showTimePicker(EditText field, Calendar calendar, String title) {
        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    field.setText(dateTimeFormatter.format(calendar.getTime()));

                    if (field == endDateTime && !endCalendar.after(startCalendar)) {
                        Toast.makeText(this, "End date-time must be after start date-time", Toast.LENGTH_SHORT).show();
                        endDateTime.setText("");
                        endCalendar.setTime(startCalendar.getTime());
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
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

        String adminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ✅ Fetch adminID (like A001) using admin UID
        db.collection("admin")
                .document(adminUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String adminID = doc.getString("adminID");
                        if (adminID != null) {
                            event.put("adminID", adminID);

                            db.collection("events")
                                    .document(eventID)
                                    .set(event)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "Admin ID missing in Firestore record.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Admin record not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch adminID: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        eventName.setText("");
        venue.setText("");
        startDateTime.setText("");
        endDateTime.setText("");
        pax.setText("");
        description.setText("");
        genderGroup.clearCheck();

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2);
    }
}
