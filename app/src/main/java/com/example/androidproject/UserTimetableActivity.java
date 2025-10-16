package com.example.androidproject;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserTimetableActivity extends AppCompatActivity {

    private Button btnPickStartDate, btnPickEndDate, btnApplyFilter, btnClearFilter;
    private TextView tvSelectedStartDate, tvSelectedEndDate, tvEventsCount;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<Event> joinedEventsList;
    private ArrayList<Event> filteredEventsList;
    private TimetableEventAdapter adapter;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        Log.d("Timetable", "Activity created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        joinedEventsList = new ArrayList<>();
        filteredEventsList = new ArrayList<>();
        adapter = new TimetableEventAdapter(filteredEventsList);
        recyclerView.setAdapter(adapter);

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup date pickers
        setupDatePickers();

        // Setup button listeners
        setupButtonListeners();

        // Load user's joined events
        loadJoinedEvents();
    }

    private void initializeViews() {
        btnPickStartDate = findViewById(R.id.btn_pick_start_date);
        btnPickEndDate = findViewById(R.id.btn_pick_end_date);
        btnApplyFilter = findViewById(R.id.btn_apply_filter);
        btnClearFilter = findViewById(R.id.btn_clear_filter);
        tvSelectedStartDate = findViewById(R.id.tv_selected_start_date);
        tvSelectedEndDate = findViewById(R.id.tv_selected_end_date);
        tvEventsCount = findViewById(R.id.tv_events_count);
        recyclerView = findViewById(R.id.recycler_joined_events);
        emptyState = findViewById(R.id.empty_state);
    }

    private void setupDatePickers() {
        btnPickStartDate.setOnClickListener(v -> showDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupButtonListeners() {
        btnApplyFilter.setOnClickListener(v -> applyDateFilter());
        btnClearFilter.setOnClickListener(v -> clearDateFilter());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar currentCalendar = isStartDate ? startCalendar : endCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    currentCalendar.set(year, month, dayOfMonth);
                    updateDateDisplay(isStartDate);
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateDisplay(boolean isStartDate) {
        if (isStartDate) {
            tvSelectedStartDate.setText(displayDateFormat.format(startCalendar.getTime()));
        } else {
            tvSelectedEndDate.setText(displayDateFormat.format(endCalendar.getTime()));
        }
    }

    private void applyDateFilter() {
        // Check if both dates are selected
        if (tvSelectedStartDate.getText().toString().equals("Not selected") ||
                tvSelectedEndDate.getText().toString().equals("Not selected")) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate date range
        if (startCalendar.after(endCalendar)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
            return;
        }

        filterEventsByDateRange();
    }

    private void filterEventsByDateRange() {
        filteredEventsList.clear();

        for (Event event : joinedEventsList) {
            if (event.getStartDateTime() != null && isEventInDateRange(event)) {
                filteredEventsList.add(event);
            }
        }

        updateEventsDisplay();

        if (filteredEventsList.isEmpty()) {
            Toast.makeText(this, "No events found in the selected date range", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Found " + filteredEventsList.size() + " events", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEventInDateRange(Event event) {
        try {
            String eventDateString = event.getStartDateTime().split(" ")[0];
            Date eventDate = dateFormat.parse(eventDateString);

            if (eventDate != null) {
                Calendar eventCalendar = Calendar.getInstance();
                eventCalendar.setTime(eventDate);

                // Set time to beginning of day for comparison
                Calendar startCal = (Calendar) startCalendar.clone();
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);

                Calendar endCal = (Calendar) endCalendar.clone();
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);

                return !eventCalendar.before(startCal) && !eventCalendar.after(endCal);
            }
        } catch (ParseException e) {
            Log.e("Timetable", "Error parsing event date: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e("Timetable", "Invalid date format in event: " + event.getStartDateTime());
        }

        return false;
    }

    private void clearDateFilter() {
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        tvSelectedStartDate.setText("Not selected");
        tvSelectedEndDate.setText("Not selected");

        // Show all events
        filteredEventsList.clear();
        filteredEventsList.addAll(joinedEventsList);
        updateEventsDisplay();

        Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show();
    }

    private void loadJoinedEvents() {
        String userId = mAuth.getCurrentUser().getUid();
        Log.d("Timetable", "Loading joined events for user: " + userId);

        joinedEventsList.clear();
        filteredEventsList.clear();

        db.collection("attendance")
                .whereEqualTo("userID", userId)
                .get()
                .addOnSuccessListener(attendanceQuery -> {
                    Log.d("Timetable", "Found " + attendanceQuery.size() + " attendance records");

                    if (attendanceQuery.isEmpty()) {
                        updateEventsDisplay();
                        return;
                    }

                    for (QueryDocumentSnapshot attendanceDoc : attendanceQuery) {
                        String eventId = attendanceDoc.getString("eventID");
                        if (eventId != null) {
                            db.collection("events").document(eventId).get()
                                    .addOnSuccessListener(eventDoc -> {
                                        if (eventDoc.exists()) {
                                            Event event = parseEventFromDocument(eventDoc);
                                            if (event != null) {
                                                joinedEventsList.add(event);
                                                filteredEventsList.add(event);
                                                updateEventsDisplay();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Timetable", "Error loading event: " + e.getMessage());
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Timetable", "Failed to load attendance records: " + e.getMessage());
                    Toast.makeText(this, "Failed to load your events", Toast.LENGTH_SHORT).show();
                    updateEventsDisplay();
                });
    }

    private void updateEventsDisplay() {
        adapter.notifyDataSetChanged();
        tvEventsCount.setText(filteredEventsList.size() + " events");

        if (filteredEventsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private Event parseEventFromDocument(DocumentSnapshot doc) {
        Event event = new Event();
        try {
            event.setId(doc.getId());
            event.setEventID(doc.getString("eventID"));
            event.setAdminID(doc.getString("adminID"));
            event.setDescription(doc.getString("description"));
            event.setEndDateTime(doc.getString("endDateTime"));
            event.setEventName(doc.getString("eventName"));
            event.setStartDateTime(doc.getString("startDateTime"));
            event.setVenue(doc.getString("venue"));

            Long genderSpec = doc.getLong("genderSpec");
            if (genderSpec != null) {
                event.setGenderSpec(genderSpec.intValue());
            } else {
                event.setGenderSpec(2);
            }

            Long currentAttendees = doc.getLong("currentAttendees");
            event.setCurrentAttendees(currentAttendees != null ? currentAttendees.intValue() : 0);

            Long pax = doc.getLong("pax");
            event.setPax(pax != null ? pax.intValue() : 0);

            return event;
        } catch (Exception e) {
            Log.e("Timetable", "Error parsing event: " + e.getMessage());
            return null;
        }
    }

    // Event Model (same as before)
    public static class Event {
        private String id;
        private String eventID;
        private String adminID;
        private String description;
        private String endDateTime;
        private String eventName;
        private int genderSpec;
        private String startDateTime;
        private String venue;
        private int currentAttendees;
        private int pax;

        public Event() {}

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEventID() { return eventID; }
        public void setEventID(String eventID) { this.eventID = eventID; }

        public String getAdminID() { return adminID; }
        public void setAdminID(String adminID) { this.adminID = adminID; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }

        public int getGenderSpec() { return genderSpec; }
        public void setGenderSpec(int genderSpec) { this.genderSpec = genderSpec; }

        public String getStartDateTime() { return startDateTime; }
        public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }

        public int getCurrentAttendees() { return currentAttendees; }
        public void setCurrentAttendees(int currentAttendees) { this.currentAttendees = currentAttendees; }

        public int getPax() { return pax; }
        public void setPax(int pax) { this.pax = pax; }
    }

    // Adapter for Timetable (same as before, but with updated position handling)
    private class TimetableEventAdapter extends RecyclerView.Adapter<TimetableEventAdapter.EventViewHolder> {

        private final ArrayList<Event> events;

        TimetableEventAdapter(ArrayList<Event> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item_timetable, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            try {
                Event event = events.get(position);
                holder.bind(event, position);
            } catch (Exception e) {
                Log.e("TimetableAdapter", "Error binding view holder: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvVenue, tvDatetime, tvCapacity;
            Button btnLeave;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tv_event_name);
                tvVenue = itemView.findViewById(R.id.tv_event_venue);
                tvDatetime = itemView.findViewById(R.id.tv_event_datetime);
                tvCapacity = itemView.findViewById(R.id.tv_event_capacity);
                btnLeave = itemView.findViewById(R.id.btn_leave);
            }

            void bind(Event event, int position) {
                tvEventName.setText(event.getEventName() != null ? event.getEventName() : "Unnamed Event");
                tvVenue.setText("Venue: " + (event.getVenue() != null ? event.getVenue() : "Not specified"));

                String startTime = event.getStartDateTime() != null ? event.getStartDateTime() : "Not set";
                String endTime = event.getEndDateTime() != null ? event.getEndDateTime() : "Not set";
                tvDatetime.setText("Start: " + startTime + "\nEnd: " + endTime);

                tvCapacity.setText(event.getCurrentAttendees() + " / " + event.getPax());

                btnLeave.setText("Leave Event");
                btnLeave.setEnabled(true);
                btnLeave.setBackgroundTintList(ContextCompat.getColorStateList(UserTimetableActivity.this, R.color.colorPrimary));

                btnLeave.setOnClickListener(v -> leaveEvent(event, position));
            }
        }

        private void leaveEvent(Event event, int position) {
            String userId = mAuth.getCurrentUser().getUid();
            String eventDocId = event.getId();

            db.collection("attendance")
                    .whereEqualTo("eventID", eventDocId)
                    .whereEqualTo("userID", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            querySnapshot.getDocuments().get(0).getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(UserTimetableActivity.this, "Left event successfully!", Toast.LENGTH_SHORT).show();

                                        DocumentReference eventRef = db.collection("events").document(eventDocId);
                                        eventRef.update("currentAttendees", Math.max(0, event.getCurrentAttendees() - 1))
                                                .addOnSuccessListener(aVoid1 -> {
                                                    // Remove from both lists
                                                    joinedEventsList.remove(event);
                                                    events.remove(position);
                                                    notifyItemRemoved(position);
                                                    updateEventsDisplay();
                                                })
                                                .addOnFailureListener(e -> {
                                                    joinedEventsList.remove(event);
                                                    events.remove(position);
                                                    notifyItemRemoved(position);
                                                    updateEventsDisplay();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(UserTimetableActivity.this, "Failed to leave event", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserTimetableActivity.this, "Error leaving event", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_timetable);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) {
                startActivity(new Intent(UserTimetableActivity.this, UserExploreActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_timetable) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(UserTimetableActivity.this, UserProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}