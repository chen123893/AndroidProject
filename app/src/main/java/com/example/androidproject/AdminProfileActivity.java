package com.example.androidproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private TextInputEditText etPassword;
    private EditText etName, etEmail, etPhone;
    private RadioButton radioMale, radioFemale;
    private ImageView profilePic;
    private Button btnEditPic, btnUpdate;

    private Uri imageUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private ProgressDialog progressDialog;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("adminProfilePics");

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        profilePic = findViewById(R.id.profilePic);
        btnEditPic = findViewById(R.id.btnEditPic);
        btnUpdate = findViewById(R.id.btnUpdate);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving profile...");

        // Disable editing by default
        setEditingEnabled(false);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No logged in user. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadAdminProfile(currentUser.getUid());

        btnEditPic.setOnClickListener(v -> toggleEditMode());
        btnUpdate.setOnClickListener(v -> {
            if (isEditing) updateProfile();
            else Toast.makeText(this, "Click Edit first to modify profile.", Toast.LENGTH_SHORT).show();
        });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(AdminProfileActivity.this, CreateEventActivity.class)); // MUST CHANGE TO MY EVENTS
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_create_event) {
                startActivity(new Intent(AdminProfileActivity.this, CreateEventActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                // Already on profile page
                return true;
            }
            return false;
        });

        requestStoragePermission();
    }

    private void loadAdminProfile(String uid) {
        Log.d("AdminProfile", "Loading profile for UID: " + uid);
        db.collection("admin").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("AdminProfile", "Profile found: " + documentSnapshot.getData());

                        // Debug each field individually
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phoneNum = documentSnapshot.getString("phoneNum");
                        String password = documentSnapshot.getString("password");
                        Long gender = documentSnapshot.getLong("gender");
                        String imageUrl = documentSnapshot.getString("profilePic");

                        Log.d("AdminProfile", "Name: " + name);
                        Log.d("AdminProfile", "Email: " + email);
                        Log.d("AdminProfile", "Phone: " + phoneNum);
                        Log.d("AdminProfile", "Password: " + password);
                        Log.d("AdminProfile", "Gender: " + gender);
                        Log.d("AdminProfile", "Image URL: " + imageUrl);

                        // Set values
                        etName.setText(name != null ? name : "");
                        etEmail.setText(email != null ? email : "");
                        etPhone.setText(phoneNum != null ? phoneNum : "");
                        etPassword.setText(password != null ? password : "");

                        if (gender != null) {
                            if (gender == 0) {
                                radioMale.setChecked(true);
                                Log.d("AdminProfile", "Gender set to Male");
                            } else if (gender == 1) {
                                radioFemale.setChecked(true);
                                Log.d("AdminProfile", "Gender set to Female");
                            } else {
                                Log.d("AdminProfile", "Gender set to None/Other");
                            }
                        } else {
                            Log.d("AdminProfile", "Gender is null");
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Picasso.get().load(imageUrl).fit().centerCrop().into(profilePic);
                            Log.d("AdminProfile", "Image loaded from URL");
                        } else {
                            profilePic.setImageResource(R.drawable.tofu);
                            Log.d("AdminProfile", "Using default image");
                        }
                    } else {
                        Log.d("AdminProfile", "Profile not found, creating default");
                        Toast.makeText(this, "Admin profile not found.", Toast.LENGTH_SHORT).show();
                        profilePic.setImageResource(R.drawable.tofu);

                        // Create a default profile if it doesn't exist
                        createDefaultProfile(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminProfile", "Error loading profile: " + e.getMessage());
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createDefaultProfile(String uid) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> defaultProfile = new HashMap<>();
            defaultProfile.put("name", user.getDisplayName() != null ? user.getDisplayName() : "Admin User");
            defaultProfile.put("email", user.getEmail());
            defaultProfile.put("phoneNum", "");
            defaultProfile.put("password", "");
            defaultProfile.put("gender", 2);
            defaultProfile.put("profilePic", "");

            db.collection("admin").document(uid)
                    .set(defaultProfile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Default profile created.", Toast.LENGTH_SHORT).show();
                        loadAdminProfile(uid);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        setEditingEnabled(isEditing);

        if (isEditing) {
            btnEditPic.setText("Cancel Edit");
            profilePic.setOnClickListener(v -> openImagePicker());
        } else {
            btnEditPic.setText("Edit Profile");
            profilePic.setOnClickListener(null);
            imageUri = null;
        }
    }

    private void setEditingEnabled(boolean enabled) {
        etName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        radioMale.setEnabled(enabled);
        radioFemale.setEnabled(enabled);
        btnUpdate.setEnabled(enabled);
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profilePic.setImageURI(imageUri);
        }
    }

    private void updateProfile() {
        progressDialog.show();

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Make gender final so it can be used in lambda
        final int gender;
        if (radioMale.isChecked()) gender = 0;
        else if (radioFemale.isChecked()) gender = 1;
        else gender = 2;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User not found. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DocumentReference docRef = db.collection("admin").document(uid);

        docRef.get().addOnSuccessListener(snapshot -> {
            String existingImageUrl = snapshot.getString("profilePic");

            user.updateEmail(email)
                    .addOnSuccessListener(unused -> user.updatePassword(password)
                            .addOnSuccessListener(unused1 -> {
                                if (imageUri != null) {
                                    StorageReference fileRef = storageRef.child(uid + ".jpg");
                                    try {
                                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                        if (inputStream == null) {
                                            progressDialog.dismiss();
                                            Toast.makeText(AdminProfileActivity.this, "Cannot open image file.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        fileRef.putStream(inputStream)
                                                .addOnSuccessListener(taskSnapshot -> {
                                                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                                        saveProfileToFirestore(docRef, name, email, phone, password, gender, uri.toString());
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(AdminProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                });
                                    } catch (Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(AdminProfileActivity.this, "Failed to open image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    saveProfileToFirestore(docRef, name, email, phone, password, gender, existingImageUrl);
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(AdminProfileActivity.this, "Password update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(AdminProfileActivity.this, "Email update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(AdminProfileActivity.this, "Failed to retrieve profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfileToFirestore(DocumentReference docRef, String name, String email, String phone,
                                        String password, int gender, String imageUrl) {

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        profileData.put("phoneNum", phone);
        profileData.put("password", password);
        profileData.put("gender", gender);
        if (imageUrl != null && !imageUrl.isEmpty()) profileData.put("profilePic", imageUrl);

        docRef.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    loadAdminProfile(docRef.getId());
                    toggleEditMode();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to update Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }
    }
}