package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etDescription, etPassword;
    private RadioGroup radioGroupGender;
    private RadioButton radioMale, radioFemale;
    private Button btnUpdate, btnEditPic, btnEditProfile, btnCancel;
    private ImageView btnLogout, profilePic;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentCollection;

    private boolean isEditMode = false;
    private Map<String, Object> originalData = new HashMap<>();

    // Local profile images
    private final List<Integer> LOCAL_PROFILE_IMAGES = Arrays.asList(
            R.drawable.tofu,
            R.drawable.profile1,
            R.drawable.profile2,
            R.drawable.profile3
    );

    private final List<String> IMAGE_NAMES = Arrays.asList(
            "Default",
            "Professional",
            "Casual",
            "Creative"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        currentUserId = currentUser.getUid();

        // Initialize views
        initializeViews();

        // Load user data
        loadUserData();

        // Setup button listeners
        setupButtonListeners();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDescription = findViewById(R.id.etDescription);
        etPassword = findViewById(R.id.etPassword);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnEditPic = findViewById(R.id.btnEditPic);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnCancel = findViewById(R.id.btnCancel);
        btnLogout = findViewById(R.id.btnLogout);
        profilePic = findViewById(R.id.profilePic);

        // Make profile image clickable
        profilePic.setClickable(true);
        profilePic.setFocusable(true);

        // Initially disable all fields except profile image functionality
        setEditMode(false);

        // Profile image button is always enabled
        btnEditPic.setEnabled(true);
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;

        etName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        radioGroupGender.setEnabled(enabled);
        radioMale.setEnabled(enabled);
        radioFemale.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        btnUpdate.setEnabled(enabled);

        // Show/hide buttons based on mode
        if (enabled) {
            btnEditProfile.setVisibility(Button.GONE);
            btnCancel.setVisibility(Button.VISIBLE);
            btnUpdate.setVisibility(Button.VISIBLE);
        } else {
            btnEditProfile.setVisibility(Button.VISIBLE);
            btnCancel.setVisibility(Button.GONE);
            btnUpdate.setVisibility(Button.GONE);
            // Clear password field when exiting edit mode
            etPassword.setText("");
        }
    }

    private void loadUserData() {
        Log.d("UserProfile", "Loading data for user UID: " + currentUserId);

        // First try user collection
        db.collection("user").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("UserProfile", "User found in 'user' collection");
                        currentCollection = "user";
                        populateUserData(documentSnapshot);
                    } else {
                        db.collection("admin").document(currentUserId).get()
                                .addOnSuccessListener(adminSnapshot -> {
                                    if (adminSnapshot.exists()) {
                                        Log.d("UserProfile", "User found in 'admin' collection");
                                        currentCollection = "admin";
                                        populateUserData(adminSnapshot);
                                    } else {
                                        Log.d("UserProfile", "User not found in any collection");
                                        Toast.makeText(this, "User profile not found. Please complete your profile.", Toast.LENGTH_LONG).show();
                                        createDefaultProfile();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserProfile", "Error checking admin collection: " + e.getMessage());
                                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error loading user data: " + e.getMessage());
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateUserData(DocumentSnapshot document) {
        try {
            String name = document.getString("name");
            String email = document.getString("email");
            String phone = document.getString("phoneNumber");
            String description = document.getString("description");
            String profilePicRef = document.getString("profilePic");
            Long gender = document.getLong("gender");

            if (name != null) etName.setText(name);
            if (email != null) etEmail.setText(email);
            if (phone != null) etPhone.setText(phone);
            if (description != null) etDescription.setText(description);

            // Store original data for cancel functionality
            originalData.put("name", name != null ? name : "");
            originalData.put("email", email != null ? email : "");
            originalData.put("phone", phone != null ? phone : "");
            originalData.put("description", description != null ? description : "");
            originalData.put("gender", gender != null ? gender : 1);
            originalData.put("profilePic", profilePicRef != null ? profilePicRef : "");

            // Load profile picture from local resources
            loadProfileImage(profilePicRef);

            // Set gender
            if (gender != null) {
                if (gender == 1) {
                    radioMale.setChecked(true);
                } else if (gender == 0) {
                    radioFemale.setChecked(true);
                }
            }

            Log.d("UserProfile", "User data loaded successfully from " + currentCollection + " collection");

        } catch (Exception e) {
            Log.e("UserProfile", "Error parsing user data: " + e.getMessage());
            Toast.makeText(this, "Error parsing user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage(String profilePicRef) {
        if (profilePicRef != null && !profilePicRef.isEmpty()) {
            int imageResId = getImageResource(profilePicRef);
            Glide.with(this)
                    .load(imageResId)
                    .placeholder(R.drawable.tofu)
                    .error(R.drawable.tofu)
                    .circleCrop()
                    .into(profilePic);
        } else {
            profilePic.setImageResource(R.drawable.tofu);
        }
    }

    private int getImageResource(String imageName) {
        int index = IMAGE_NAMES.indexOf(imageName);
        return index >= 0 ? LOCAL_PROFILE_IMAGES.get(index) : R.drawable.profile1;
    }

    private void createDefaultProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> user = new HashMap<>();
        user.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "New User");
        user.put("email", currentUser.getEmail());
        user.put("phoneNumber", "");
        user.put("description", "Tell us about yourself...");
        user.put("gender", 1);
        user.put("profilePic", "Default");

        long timestamp = System.currentTimeMillis();
        String generatedID = "U" + timestamp;
        user.put("userID", generatedID);

        currentCollection = "user";

        db.collection(currentCollection)
                .document(currentUserId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfile", "Default profile created successfully");
                    Toast.makeText(this, "Default profile created", Toast.LENGTH_SHORT).show();
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Failed to create default profile: " + e.getMessage());
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupButtonListeners() {
        // Profile picture click listener - ALWAYS enabled
        profilePic.setOnClickListener(v -> openImageChooser());

        // Edit picture button listener - ALWAYS enabled
        btnEditPic.setOnClickListener(v -> openImageChooser());

        // Edit profile button listener - for profile data only
        btnEditProfile.setOnClickListener(v -> enableEditMode());

        // Cancel button listener
        btnCancel.setOnClickListener(v -> cancelEditMode());

        btnUpdate.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void enableEditMode() {
        setEditMode(true);
        Toast.makeText(this, "You can now edit your profile data", Toast.LENGTH_SHORT).show();
    }

    private void cancelEditMode() {
        // Restore original data
        etName.setText((String) originalData.get("name"));
        etPhone.setText((String) originalData.get("phone"));
        etDescription.setText((String) originalData.get("description"));

        Long gender = (Long) originalData.get("gender");
        if (gender != null) {
            if (gender == 1) {
                radioMale.setChecked(true);
            } else if (gender == 0) {
                radioFemale.setChecked(true);
            }
        }

        // Restore original profile picture
        String originalProfilePic = (String) originalData.get("profilePic");
        loadProfileImage(originalProfilePic);

        etPassword.setText("");
        setEditMode(false);
        Toast.makeText(this, "Changes cancelled", Toast.LENGTH_SHORT).show();
    }

    private void openImageChooser() {
        showImageSelectionDialog();
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Profile Picture");

        // Create a GridLayout for the images
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(3); // Fixed 3 columns
        gridLayout.setPadding(32, 32, 32, 32);

        // Calculate the width for each item to fill the row properly
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int itemWidth = (screenWidth - (32 * 4)) / 3; // 32dp padding on both sides + gaps

        for (int i = 0; i < LOCAL_PROFILE_IMAGES.size(); i++) {
            final int position = i;
            final int imageResId = LOCAL_PROFILE_IMAGES.get(i);
            final String imageName = IMAGE_NAMES.get(i);

            // Create a container for each image item
            LinearLayout container = new LinearLayout(this);
            GridLayout.LayoutParams containerParams = new GridLayout.LayoutParams();
            containerParams.width = itemWidth;
            containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            containerParams.setMargins(8, 8, 8, 8);
            container.setLayoutParams(containerParams);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            container.setPadding(16, 16, 16, 16);
            container.setBackgroundResource(R.drawable.image_item_background);
            container.setClickable(true);
            container.setFocusable(true);

            // Create image view
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    itemWidth - 64, // Account for container padding
                    itemWidth - 64
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Load image with Glide
            Glide.with(this)
                    .load(imageResId)
                    .circleCrop()
                    .into(imageView);

            // Create text view for image name
            TextView textView = new TextView(this);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textParams.setMargins(0, 12, 0, 0);
            textView.setLayoutParams(textParams);
            textView.setText(imageName);
            textView.setTextSize(12);
            textView.setGravity(Gravity.CENTER);
            textView.setMaxWidth(itemWidth - 32);
            textView.setSingleLine(true);

            container.addView(imageView);
            container.addView(textView);

            // Set click listener
            container.setOnClickListener(v -> {
                // Update profile picture
                Glide.with(UserProfileActivity.this)
                        .load(imageResId)
                        .circleCrop()
                        .into(profilePic);

                // Save to Firestore
                saveSelectedImageToFirestore(imageName);

                // Dismiss dialog
                ((AlertDialog) v.getTag()).dismiss();

                Toast.makeText(UserProfileActivity.this, "Profile image updated to: " + imageName, Toast.LENGTH_SHORT).show();
            });

            gridLayout.addView(container);
        }

        builder.setView(gridLayout);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        // Set dialog as tag for all clickable views
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).setTag(dialog);
        }

        dialog.show();

        // Set dialog window size
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void saveSelectedImageToFirestore(String imageName) {
        if (currentCollection == null) {
            Toast.makeText(this, "Error: User collection not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection(currentCollection)
                .document(currentUserId)
                .update("profilePic", imageName)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfile", "Profile image reference saved: " + imageName);
                    // Update original data
                    originalData.put("profilePic", imageName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show();
                    Log.e("UserProfile", "Failed to save image reference: " + e.getMessage());
                });
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performFirebaseLogout();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void performFirebaseLogout() {
        try {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            mAuth.signOut();

            if (mAuth.getCurrentUser() == null) {
                redirectToLogin();
            } else {
                Toast.makeText(this, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("UserProfile", "Logout error: " + e.getMessage());
            Toast.makeText(this, "Error during logout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        int genderValue = (selectedGenderId == R.id.radioMale) ? 1 : 0;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phoneNumber", phone);
        updates.put("description", description);
        updates.put("gender", genderValue);

        db.collection(currentCollection)
                .document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfile", "Profile updated successfully");

                    // Update original data
                    originalData.put("name", name);
                    originalData.put("phone", phone);
                    originalData.put("description", description);
                    originalData.put("gender", (long) genderValue);

                    if (!newPassword.isEmpty()) {
                        if (newPassword.length() < 6) {
                            etPassword.setError("Password must be at least 6 characters");
                            return;
                        }
                        updatePassword(newPassword);
                    } else {
                        setEditMode(false);
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Failed to update profile: " + e.getMessage());
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("UserProfile", "Password updated successfully");
                            setEditMode(false);
                            etPassword.setText("");
                            Toast.makeText(this, "Profile and password updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("UserProfile", "Failed to update password: " + task.getException().getMessage());
                            Toast.makeText(this, "Profile updated but failed to update password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_explore) {
                startActivity(new Intent(UserProfileActivity.this, UserExploreActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_timetable) {
                startActivity(new Intent(UserProfileActivity.this, UserTimetableActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }
}