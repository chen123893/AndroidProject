package com.example.androidproject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EditText eventName, venue, startDateTime, endDateTime, pax, description;
    private RadioGroup genderGroup;
    private Button saveBtn, deleteBtn, selectImageBtn;
    private ImageButton backBtn;
    private ImageView selectedImageView;
    private TextView tvTapHint;

    private FirebaseFirestore db;

    private String eventID;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateTimeFormatter;

    // EmailJS (unchanged)
    private static final String EMAILJS_SERVICE_ID = "service_bj4nogo";
    private static final String EMAILJS_TEMPLATE_ID = "template_e82chx6";
    private static final String EMAILJS_PUBLIC_KEY = "hxbMOwE1NOOZ8DY3u";

    // --- Built-in image catalog (same as Create) ---
    private final ArrayList<String> IMAGE_NAMES = new ArrayList<>();
    private final ArrayList<Integer> IMAGE_RES_IDS = new ArrayList<>();

    // Selection state
    private String selectedImageName = null;
    private int selectedImageResId = 0; // 0 = none

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        db = FirebaseFirestore.getInstance();
        dateTimeFormatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        // Views
        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startDateTime = findViewById(R.id.startDateTime);
        endDateTime = findViewById(R.id.endDateTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        saveBtn = findViewById(R.id.createBtn);
        deleteBtn = findViewById(R.id.deleteButton);
        selectImageBtn = findViewById(R.id.selectImageBtn);
        backBtn = findViewById(R.id.backButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        tvTapHint = findViewById(R.id.tvTapHint);

        saveBtn.setText("Save Changes");

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        // Prepare image catalog
        setupImageCatalog();

        eventID = getIntent().getStringExtra("eventID");
        if (eventID == null) {
            Toast.makeText(this, "No event ID found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventData();
        setupDateTimePickers();

        // Image picker
        selectImageBtn.setOnClickListener(v -> openBuiltinImageChooser());

        // Save
        saveBtn.setOnClickListener(v -> showNotifyDialog());

        // Delete
        deleteBtn.setOnClickListener(v -> confirmDelete());

        // Back
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

    private void setupImageCatalog() {
        IMAGE_NAMES.add("Badminton");
        IMAGE_RES_IDS.add(R.drawable.event_badminton);

        IMAGE_NAMES.add("Education Fair");
        IMAGE_RES_IDS.add(R.drawable.event_education);

        IMAGE_NAMES.add("Food Fair");
        IMAGE_RES_IDS.add(R.drawable.event_food_fair);

        IMAGE_NAMES.add("Pickle Ball");
        IMAGE_RES_IDS.add(R.drawable.event_pickleball);

        IMAGE_NAMES.add("Pilates");
        IMAGE_RES_IDS.add(R.drawable.event_pilates);

        IMAGE_NAMES.add("Test Drive");
        IMAGE_RES_IDS.add(R.drawable.event_testdrive);

        IMAGE_NAMES.add("Wine Test");
        IMAGE_RES_IDS.add(R.drawable.event_wine_test);
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
                        Object paxObj = data.get("pax");
                        if (paxObj != null) pax.setText(String.valueOf(paxObj));
                        description.setText((String) data.get("description"));

                        Long genderSpec = (Long) data.get("genderSpec");
                        if (genderSpec != null) {
                            if (genderSpec == 0) genderGroup.check(R.id.male);
                            else if (genderSpec == 1) genderGroup.check(R.id.female);
                            else genderGroup.check(R.id.none);
                        }

                        // Load imageName -> set preview
                        String imageName = (String) data.get("imageName");
                        if (imageName != null && !imageName.isEmpty()) {
                            Integer resId = resolveResForName(imageName);
                            if (resId != null && resId != 0) {
                                selectedImageName = imageName;
                                selectedImageResId = resId;
                            }
                        }
                        applySelectedImageUI();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Integer resolveResForName(String name) {
        for (int i = 0; i < IMAGE_NAMES.size(); i++) {
            if (IMAGE_NAMES.get(i).equalsIgnoreCase(name)) {
                return IMAGE_RES_IDS.get(i);
            }
        }
        return 0;
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

    // ---------- Image chooser (same UX as Create) ----------
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openBuiltinImageChooser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Event Image");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int hPad = dp(16), vPad = dp(8);
        root.setPadding(hPad, vPad, hPad, vPad);
        root.setClipToPadding(false); root.setClipChildren(false);

        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        scroller.setFillViewport(true);

        GridLayout grid = new GridLayout(this);
        grid.setUseDefaultMargins(false);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setClipToPadding(false);
        int gridHPad = dp(8);
        grid.setPadding(gridHPad, dp(12), gridHPad, dp(12));

        // width & columns
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        float density = dm.density;
        int targetDialogW = Math.min((int) (screenW * 0.92f), dp(520));
        int innerW = targetDialogW - (hPad * 2) - (gridHPad * 2);
        int screenWdp = Math.round(screenW / density);
        int cols = (screenWdp >= 380) ? 3 : 2;
        grid.setColumnCount(cols);

        int gap = dp(12);
        int totalGaps = gap * (cols + 1);
        int itemW = (innerW - totalGaps) / cols;

        for (int i = 0; i < IMAGE_NAMES.size(); i++) {
            String label = IMAGE_NAMES.get(i);
            int resId = IMAGE_RES_IDS.get(i);

            LinearLayout card = new LinearLayout(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = itemW;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            card.setLayoutParams(lp);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackgroundResource(R.drawable.image_item_background);
            card.setClickable(true);

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams ivLp =
                    new LinearLayout.LayoutParams(itemW - dp(36), itemW - dp(36));
            iv.setLayoutParams(ivLp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(resId).into(iv);

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams tvLp =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            tvLp.setMargins(0, dp(8), 0, 0);
            tv.setLayoutParams(tvLp);
            tv.setText(label);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);

            card.addView(iv);
            card.addView(tv);
            grid.addView(card);

            final int index = i;
            card.setOnClickListener(v -> {
                selectedImageName = IMAGE_NAMES.get(index);
                selectedImageResId = IMAGE_RES_IDS.get(index);
                applySelectedImageUI();
                AlertDialog d = (AlertDialog) v.getTag();
                if (d != null) d.dismiss();
            });
        }

        scroller.addView(grid);
        root.addView(scroller);
        builder.setView(root);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        grid.post(() -> {
            for (int i = 0; i < grid.getChildCount(); i++) {
                grid.getChildAt(i).setTag(dialog);
            }
        });

        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(targetDialogW, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void applySelectedImageUI() {
        if (selectedImageResId == 0) {
            selectedImageView.setVisibility(ImageView.GONE);
            if (tvTapHint != null) tvTapHint.setVisibility(TextView.VISIBLE);
            return;
        }
        Glide.with(this).load(selectedImageResId).into(selectedImageView);
        selectedImageView.setVisibility(ImageView.VISIBLE);
        if (tvTapHint != null) tvTapHint.setVisibility(TextView.GONE);
    }
    // ------------------------------------------------------

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

                        // build update map
                        com.google.firebase.firestore.FieldValue fv; // (placeholder to keep imports tidy if needed)

                        com.google.firebase.firestore.DocumentReference ref =
                                db.collection("events").document(docId);

                        // update fields (including imageName if changed/selected)
                        com.google.firebase.firestore.SetOptions merge = com.google.firebase.firestore.SetOptions.merge();
                        java.util.HashMap<String, Object> update = new java.util.HashMap<>();
                        update.put("eventName", name);
                        update.put("venue", loc);
                        update.put("startDateTime", start);
                        update.put("endDateTime", end);
                        update.put("pax", Integer.parseInt(paxNum));
                        update.put("description", desc);
                        update.put("genderSpec", genderCode);
                        if (selectedImageResId != 0 && selectedImageName != null) {
                            update.put("imageName", selectedImageName);
                        }

                        ref.set(update, merge)
                                .addOnSuccessListener(aVoid -> {
                                    if (notifyAttendees) {
                                        notifyAllAttendees(name, loc, start, end, paxNum);
                                    } else {
                                        Toast.makeText(this, "Event has been updated", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
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
                        goBackToMyEvents();
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
                            .setPositiveButton("OK", (dialog, which) -> goBackToMyEvents())
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Notify failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("This will delete the event and its attendance records. Proceed?")
                .setPositiveButton("Delete", (d, w) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent() {
        db.collection("events").whereEqualTo("eventID", eventID)
                .get()
                .addOnSuccessListener(eventSnap -> {
                    if (eventSnap.isEmpty()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String docId = eventSnap.getDocuments().get(0).getId();

                    // batch: delete attendance docs, then event
                    db.collection("attendance").whereEqualTo("eventID", eventID)
                            .get()
                            .addOnSuccessListener(attSnap -> {
                                WriteBatch batch = db.batch();
                                for (DocumentSnapshot d : attSnap) {
                                    batch.delete(d.getReference());
                                }
                                batch.delete(db.collection("events").document(docId));

                                batch.commit()
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                            goBackToMyEvents();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error loading attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error finding event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goBackToMyEvents() {
        startActivity(new Intent(EditEventActivity.this, MyEventsActivity.class));
        finish();
    }

    // EmailJS send
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
                android.util.Log.d("EmailJS", "Email sent to " + email + ", response: " + responseCode);

            } catch (Exception e) {
                android.util.Log.e("EmailJS", "Error sending email: " + e.getMessage());
            }
        }).start();
    }
}
