package com.example.androidproject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    // 🔹 Replace with your actual EmailJS details
    private static final String EMAILJS_SERVICE_ID = "service_bj4nogo";
    private static final String EMAILJS_TEMPLATE_ID = "template_e82chx6";
    private static final String EMAILJS_PUBLIC_KEY = "hxbMOwE1NOOZ8DY3u";

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

        eventID = getIntent().getStringExtra("eventID");
        if (eventID == null) {
            Toast.makeText(this, "No event ID found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventData();
        setupDateTimePickers();

        saveBtn.setOnClickListener(v -> showNotifyDialog());

        backBtn.setOnClickListener(v -> {
            startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
            finish();
        });

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_my_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_create_event) {
                startActivity(new Intent(EditEventActivity.this, CreateEventActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(EditEventActivity.this, AdminProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadEventData() {
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
        startDateTime.setOnClickListener(v -> showDateTimePicker(startDateTime, startCalendar));
        endDateTime.setOnClickListener(v -> showDateTimePicker(endDateTime, endCalendar));
    }

    private void showDateTimePicker(EditText field, Calendar calendar) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker(field, calendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showTimePicker(EditText field, Calendar calendar) {
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

    private void showNotifyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notify Attendees?")
                .setMessage("Do you want to notify all attendees about the changes made?")
                .setPositiveButton("Yes", (dialog, which) -> updateEvent(true))
                .setNegativeButton("No", (dialog, which) -> updateEvent(false))
                .show();
    }

    private void updateEvent(boolean notifyAttendees) {
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

        final int genderCode;
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.male) genderCode = 0;
        else if (selectedId == R.id.female) genderCode = 1;
        else genderCode = 2;

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
                            if (notifyAttendees) {
                                notifyAllAttendees(name, loc, start, end, paxNum);
                            } else {
                                Toast.makeText(this, "Event has been updated", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                                finish();
                            }
                        }).addOnFailureListener(e ->
                                Toast.makeText(this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error finding event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void notifyAllAttendees(String name, String venue, String start, String end, String paxNum) {
        db.collection("attendance")
                .whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(attendanceSnapshot -> {
                    if (attendanceSnapshot.isEmpty()) {
                        Toast.makeText(this, "No attendees to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : attendanceSnapshot) {
                        String userID = doc.getString("userID");
                        if (userID != null && !userID.isEmpty()) {
                            db.collection("user").whereEqualTo("userID", userID)
                                    .get()
                                    .addOnSuccessListener(userSnap -> {
                                        if (!userSnap.isEmpty()) {
                                            String email = userSnap.getDocuments().get(0).getString("email");
                                            if (email != null && !email.isEmpty()) {
                                                sendEmailToAttendee(email, name, venue, start, end, paxNum);
                                            }
                                        }
                                    });
                        }
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Update Sent")
                            .setMessage("Event has been updated. All attendees have been notified by email.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                                finish();
                            })
                            .show();
                });
    }

    // 🔹 EmailJS send email function
    private void sendEmailToAttendee(String email, String name, String venue, String start, String end, String paxNum) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject params = new JSONObject();
                params.put("service_id", EMAILJS_SERVICE_ID);
                params.put("template_id", EMAILJS_TEMPLATE_ID);
                params.put("user_id", EMAILJS_PUBLIC_KEY);

                JSONObject templateParams = new JSONObject();
                templateParams.put("email", email);
                templateParams.put("event_name", name);
                templateParams.put("event_venue", venue);
                templateParams.put("event_start", start);
                templateParams.put("event_end", end);
                templateParams.put("event_pax", paxNum);

                params.put("template_params", templateParams);

                OutputStream os = conn.getOutputStream();
                os.write(params.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("EmailJS", "Email sent to " + email + ", response: " + responseCode);

            } catch (Exception e) {
                Log.e("EmailJS", "Error sending email: " + e.getMessage());
            }
        }).start();
    }
}
