package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etDescription, etPassword;
    private RadioGroup radioGroupGender;
    private RadioButton radioMale, radioFemale;
    private Button btnUpdate, btnEditPic;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentCollection; // Track whether user is in "user" or "admin" collection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

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

        // Initially disable all fields except password
        setFieldsEnabled(false);
        etPassword.setEnabled(true); // Allow password changes
    }

    private void setFieldsEnabled(boolean enabled) {
        etName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        radioMale.setEnabled(enabled);
        radioFemale.setEnabled(enabled);
        btnUpdate.setEnabled(enabled);
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
                        // If not in user collection, try admin collection
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
            // Get user data
            String name = document.getString("name");
            String email = document.getString("email");
            String phone = document.getString("phoneNumber");
            String description = document.getString("description");
            Long gender = document.getLong("gender");

            // Populate fields
            if (name != null) etName.setText(name);
            if (email != null) etEmail.setText(email);
            if (phone != null) etPhone.setText(phone);
            if (description != null) etDescription.setText(description);

            // Set gender
            if (gender != null) {
                if (gender == 1) { // Male
                    radioMale.setChecked(true);
                } else if (gender == 0) { // Female
                    radioFemale.setChecked(true);
                }
            }

            Log.d("UserProfile", "User data loaded successfully from " + currentCollection + " collection");

        } catch (Exception e) {
            Log.e("UserProfile", "Error parsing user data: " + e.getMessage());
            Toast.makeText(this, "Error parsing user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void createDefaultProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> user = new HashMap<>();
        user.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "New User");
        user.put("email", currentUser.getEmail());
        user.put("phoneNumber", "");
        user.put("description", "Tell us about yourself...");
        user.put("gender", 1); // Default to male
        user.put("profilePic", null);

        // Generate dynamic ID
        long timestamp = System.currentTimeMillis();
        String generatedID = "U" + timestamp;
        user.put("userID", generatedID);

        // Default to user collection
        currentCollection = "user";

        db.collection(currentCollection)
                .document(currentUserId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfile", "Default profile created successfully");
                    Toast.makeText(this, "Default profile created", Toast.LENGTH_SHORT).show();
                    // Reload data to populate fields
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Failed to create default profile: " + e.getMessage());
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupButtonListeners() {
        btnEditPic.setOnClickListener(v -> toggleEditMode());

        btnUpdate.setOnClickListener(v -> updateProfile());
    }

    private void toggleEditMode() {
        boolean currentlyEnabled = etName.isEnabled();

        if (!currentlyEnabled) {
            // Enter edit mode
            setFieldsEnabled(true);
            btnEditPic.setText("Cancel Edit");
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Cancel edit mode
            setFieldsEnabled(false);
            btnEditPic.setText("Edit Profile");
            // Reload original data
            loadUserData();
            Toast.makeText(this, "Edit cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile() {
        // Validate inputs
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

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phoneNumber", phone);
        updates.put("description", description);
        updates.put("gender", genderValue);

        // Update Firestore
        db.collection(currentCollection)
                .document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfile", "Profile updated successfully");

                    // Update password if provided
                    if (!newPassword.isEmpty()) {
                        if (newPassword.length() < 6) {
                            etPassword.setError("Password must be at least 6 characters");
                            return;
                        }
                        updatePassword(newPassword);
                    } else {
                        // Success without password change
                        setFieldsEnabled(false);
                        btnEditPic.setText("Edit Profile");
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
                            setFieldsEnabled(false);
                            btnEditPic.setText("Edit Profile");
                            etPassword.setText(""); // Clear password field
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
                Toast.makeText(UserProfileActivity.this, "My Events feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update bottom navigation selection
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }
}