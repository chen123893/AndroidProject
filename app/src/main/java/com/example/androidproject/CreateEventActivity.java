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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText eventName, venue, startDateTime, endDateTime, pax, description;
    private RadioGroup genderGroup;
    private Button createBtn, selectImageBtn;
    private ImageView selectedImageView;
    private TextView tvTapHint;

    private FirebaseFirestore db;

    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateTimeFormatter;

    private final ArrayList<String> IMAGE_NAMES = new ArrayList<>();
    private final ArrayList<Integer> IMAGE_RES_IDS = new ArrayList<>();

    private String selectedImageName = null;
    private int selectedImageResId = 0; // 0 = none chosen yet

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2);
        dateTimeFormatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        // Views
        eventName = findViewById(R.id.eventName);
        venue = findViewById(R.id.venue);
        startDateTime = findViewById(R.id.startDateTime);
        endDateTime = findViewById(R.id.endDateTime);
        pax = findViewById(R.id.pax);
        description = findViewById(R.id.description);
        genderGroup = findViewById(R.id.genderGroup);
        createBtn = findViewById(R.id.createBtn);
        selectImageBtn = findViewById(R.id.selectImageBtn);
        selectedImageView = findViewById(R.id.selectedImageView);
        tvTapHint = findViewById(R.id.tvTapHint);

        setupImageCatalog();       // fill lists only (no default selection)
        setupDateTimePickers();

        // Start state: no image, hint visible
        applySelectedImageUI();

        // Open built-in chooser
        selectImageBtn.setOnClickListener(v -> openBuiltinImageChooser());

        createBtn.setOnClickListener(v -> createEvent());

        // Bottom navigation
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

    private void setupImageCatalog() {
        // Add entries: (label shown to user, drawable resource)
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
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private void openBuiltinImageChooser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Event Image");

        // Root with inner padding so cards don’t touch rounded edges
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int hPad = dp(16), vPad = dp(8);
        root.setPadding(hPad, vPad, hPad, vPad);
        root.setClipToPadding(false); root.setClipChildren(false);

        // Scroll (prevents vertical crop)
        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        scroller.setFillViewport(true);

        // Grid
        GridLayout grid = new GridLayout(this);
        grid.setUseDefaultMargins(false);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setClipToPadding(false);
        int gridHPad = dp(8);
        grid.setPadding(gridHPad, dp(12), gridHPad, dp(12));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        float density = dm.density;

        int dialogSideInset = dp(24);

        int outerHPad = hPad;          // same as above (dp 16)
        int innerHPad = gridHPad;      // same as above (dp 8)
        int gap = dp(10);

        int cols = (int) (screenW / density) >= 360 ? 3 : 2;
        grid.setColumnCount(cols);

        int usable = screenW - (2 * dialogSideInset) - (2 * outerHPad) - (2 * innerHPad);
        int itemW = (usable - gap * (cols - 1)) / cols;

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
            card.setPadding(dp(10), dp(10), dp(10), dp(10));            card.setBackgroundResource(R.drawable.image_item_background);
            card.setClickable(true);

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams ivLp =
                    new LinearLayout.LayoutParams(itemW - dp(28), itemW - dp(28));            iv.setLayoutParams(ivLp);
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

        // Tag dialog on each card so we can dismiss on click
        grid.post(() -> {
            for (int i = 0; i < grid.getChildCount(); i++) {
                grid.getChildAt(i).setTag(dialog);
            }
        });

        dialog.show();

        // Apply the computed dialog width
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


    private void applySelectedImageUI() {
        if (selectedImageResId == 0) {
            // nothing chosen yet
            selectedImageView.setVisibility(ImageView.GONE);
            if (tvTapHint != null) tvTapHint.setVisibility(TextView.VISIBLE);
            return;
        }
        // show only the image; hide hint (never show any name/path)
        Glide.with(this).load(selectedImageResId).into(selectedImageView);
        selectedImageView.setVisibility(ImageView.VISIBLE);
        if (tvTapHint != null) tvTapHint.setVisibility(TextView.GONE);
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

        int genderCode = 2; // 0=male,1=female,2=none
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.male) genderCode = 1;
        else if (selectedId == R.id.female) genderCode = 0;
        else genderCode = 2;

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

        // Only save image info if the user actually picked one
        if (selectedImageResId != 0) {
            String resKey = getResources().getResourceEntryName(selectedImageResId);
            event.put("imageName", selectedImageName);
            event.put("imageResKey", resKey);
        }


        String adminUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (adminUid == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch adminID and save
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
        genderGroup.check(R.id.none);

        // Reset date state
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 2);

        // Reset image selection UI
        selectedImageName = null;
        selectedImageResId = 0;
        applySelectedImageUI();
    }
}
