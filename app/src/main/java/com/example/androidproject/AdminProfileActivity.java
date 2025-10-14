package com.example.androidproject;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

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
    private String adminDocId;
    private String currentAdminID; // store the actual "Axxxx" ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("adminProfilePics");

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

        setEditingEnabled(false);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No logged in user. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // ✅ Load admin profile directly by UID
        loadAdminProfile(currentUser.getUid());

        btnEditPic.setOnClickListener(v -> toggleEditMode());
        btnUpdate.setOnClickListener(v -> {
            if (isEditing) updateProfile();
            else Toast.makeText(this, "Click Edit first to modify profile.", Toast.LENGTH_SHORT).show();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_events) {
                startActivity(new Intent(AdminProfileActivity.this, MyEventsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_create_event) {
                startActivity(new Intent(AdminProfileActivity.this, CreateEventActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        requestStoragePermission();
    }

    private void loadAdminProfile(String uid) {
        db.collection("admin").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        adminDocId = doc.getId();
                        currentAdminID = doc.getString("adminID"); // ✅ get "Axxxx" format

                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phoneNumber");
                        String password = doc.getString("password");
                        Long gender = doc.getLong("gender");
                        String imageUrl = doc.getString("profilePic");

                        etName.setText(name != null ? name : "");
                        etEmail.setText(email != null ? email : "");
                        etPhone.setText(phone != null ? phone : "");
                        etPassword.setText(password != null ? password : "");

                        if (gender != null) {
                            if (gender == 1) radioMale.setChecked(true);
                            else if (gender == 0) radioFemale.setChecked(true);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Picasso.get().load(imageUrl).fit().centerCrop().into(profilePic);
                        } else {
                            profilePic.setImageResource(R.drawable.tofu);
                        }

                    } else {
                        Toast.makeText(this, "Admin profile not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        setEditingEnabled(isEditing);
        btnEditPic.setText(isEditing ? "Cancel Edit" : "Edit Profile");

        if (isEditing)
            profilePic.setOnClickListener(v -> openImagePicker());
        else
            profilePic.setOnClickListener(null);
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
        if (adminDocId == null) {
            Toast.makeText(this, "No admin profile loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        int gender = radioMale.isChecked() ? 1 : 0;

        DocumentReference docRef = db.collection("admin").document(adminDocId);

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(adminDocId + ".jpg");
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream != null) {
                    fileRef.putStream(inputStream)
                            .addOnSuccessListener(taskSnapshot ->
                                    fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                                            saveProfileToFirestore(docRef, name, email, phone, password, gender, uri.toString())))
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            } catch (Exception e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to open image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            saveProfileToFirestore(docRef, name, email, phone, password, gender, null);
        }
    }

    private void saveProfileToFirestore(DocumentReference docRef, String name, String email, String phone,
                                        String password, int gender, String imageUrl) {

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        profileData.put("phoneNumber", phone);
        profileData.put("password", password);
        profileData.put("gender", gender);
        if (imageUrl != null) profileData.put("profilePic", imageUrl);

        docRef.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    toggleEditMode();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
