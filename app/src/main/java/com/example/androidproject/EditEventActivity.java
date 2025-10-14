package com.example.androidproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EditText eventName, venue, startDateTime, endDateTime, pax, description;
    private RadioGroup genderGroup;
    private Button saveBtn;
    private ImageButton backBtn;
    private FirebaseFirestore db;

    private String eventID;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateTimeFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        db = FirebaseFirestore.getInstance();
        dateTimeFormatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startDateTime = findViewById(R.id.startDateTime);
        endDateTime = findViewById(R.id.endDateTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        saveBtn = findViewById(R.id.createBtn);
        backBtn = findViewById(R.id.backButton);

        saveBtn.setText("Save Changes");

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        // ✅ Get custom eventID instead of Firestore doc ID
        eventID = getIntent().getStringExtra("eventID");
        if (eventID == null) {
            Toast.makeText(this, "No event ID found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventData();

        setupDateTimePickers();

        saveBtn.setOnClickListener(v -> updateEvent());

        backBtn.setOnClickListener(v -> {
            startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
            finish();
        });
    }

    private void loadEventData() {
        // ✅ Query by eventID field
        db.collection("events").whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Map<String, Object> data = doc.getData();
                        if (data == null) return;

                        eventName.setText((String) data.get("eventName"));
                        venue.setText((String) data.get("venue"));
                        startDateTime.setText((String) data.get("startDateTime"));
                        endDateTime.setText((String) data.get("endDateTime"));
                        pax.setText(String.valueOf(data.get("pax")));
                        description.setText((String) data.get("description"));

                        Long genderSpec = (Long) data.get("genderSpec");
                        if (genderSpec != null) {
                            if (genderSpec == 0) genderGroup.check(R.id.male);
                            else if (genderSpec == 1) genderGroup.check(R.id.female);
                            else genderGroup.check(R.id.none);
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupDateTimePickers() {
        startDateTime.setOnClickListener(v -> showDateTimePicker(startDateTime, startCalendar, "Start Date & Time"));
        endDateTime.setOnClickListener(v -> showDateTimePicker(endDateTime, endCalendar, "End Date & Time"));
    }

    private void showDateTimePicker(EditText field, Calendar calendar, String title) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker(field, calendar, title);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showTimePicker(EditText field, Calendar calendar, String title) {
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    field.setText(dateTimeFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false);
        timePicker.show();
    }

    private void updateEvent() {
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

        // ✅ Make genderCode final by not reassigning it
        final int genderCode;
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.male) {
            genderCode = 0;
        } else if (selectedId == R.id.female) {
            genderCode = 1;
        } else {
            genderCode = 2;
        }

        // ✅ Update by custom eventID
        db.collection("events")
                .whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();

                        db.collection("events").document(docId).update(
                                "eventName", name,
                                "venue", loc,
                                "startDateTime", start,
                                "endDateTime", end,
                                "pax", Integer.parseInt(paxNum),
                                "description", desc,
                                "genderSpec", genderCode
                        ).addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                            finish();
                        }).addOnFailureListener(e ->
                                Toast.makeText(this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error finding event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}