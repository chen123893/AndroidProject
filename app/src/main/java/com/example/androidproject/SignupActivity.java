package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhoneNum, etPassword, etConfirm, etDescription;
    private RadioGroup rgRole, rgGender;
    private RadioButton rbUser, rbAdmin, rbMale, rbFemale;
    private CheckBox chkTerms;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private TextView tvAlreadyAccount;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // View bindings
        etName = findViewById(R.id.et_signup_name);
        etEmail = findViewById(R.id.et_signup_email);
        etPhoneNum = findViewById(R.id.et_signup_phonenum);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirm = findViewById(R.id.et_signup_confirm);
        etDescription = findViewById(R.id.et_signup_description);
        rgRole = findViewById(R.id.rg_role);
        rbUser = findViewById(R.id.rb_user);
        rbAdmin = findViewById(R.id.rb_admin);
        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        chkTerms = findViewById(R.id.chk_terms);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        progressBar = findViewById(R.id.progress_signup);
        tvAlreadyAccount = findViewById(R.id.tv_already_account);

        // Button listeners
        btnCreateAccount.setOnClickListener(v -> handleSignup());
        tvAlreadyAccount.setOnClickListener(v -> finish());
    }

    private void handleSignup() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phoneNum = etPhoneNum.getText() != null ? etPhoneNum.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        String collection;
        if (selectedRoleId == R.id.rb_admin) {
            collection = "admin";
        } else if (selectedRoleId == R.id.rb_user) {
            collection = "user";
        } else {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        int genderValue = (selectedGenderId == R.id.rb_male) ? 1 : 0; // 1 = male, 0 = female

        // Input validation
        if (name.isEmpty()) { etName.setError("Name is required"); etName.requestFocus(); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Enter a valid email"); etEmail.requestFocus(); return; }
        if (phoneNum.isEmpty() || phoneNum.length() < 10) { etPhoneNum.setError("Enter a valid phone number"); etPhoneNum.requestFocus(); return; }
        if (password.isEmpty() || password.length() < 6) { etPassword.setError("Password must be at least 6 characters"); etPassword.requestFocus(); return; }
        if (!password.equals(confirm)) { etConfirm.setError("Passwords do not match"); etConfirm.requestFocus(); return; }
        if (description.isEmpty()) { etDescription.setError("Description is required"); etDescription.requestFocus(); return; }
        if (!chkTerms.isChecked()) { Toast.makeText(this, "Please accept Terms and Privacy", Toast.LENGTH_SHORT).show(); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnCreateAccount.setEnabled(false);

        // Create Firebase Auth user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), name, email, phoneNum, description, genderValue, collection);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnCreateAccount.setEnabled(true);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String firebaseUid, String name, String email,
                                     String phoneNum, String description, int genderValue, String collection) {

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phoneNumber", phoneNum);
        user.put("description", description);
        user.put("gender", genderValue); // 1 = Male, 0 = Female
        user.put("profilePic", null);

        // ✅ Generate dynamic ID
        String generatedID;
        if (collection.equals("admin")) {
            generatedID = "A" + System.currentTimeMillis();
            user.put("adminID", generatedID);
        } else {
            generatedID = "U" + System.currentTimeMillis();
            user.put("userID", generatedID);
        }

        // Optional role field (makes querying easier later)
        user.put("role", collection.equals("admin") ? "admin" : "user");

        db.collection(collection)
                .document(firebaseUid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}