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
    private RadioGroup rgRole;
    private RadioButton rbUser, rbAdmin;
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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etName = findViewById(R.id.et_signup_name);
        etEmail = findViewById(R.id.et_signup_email);
        etPhoneNum = findViewById(R.id.et_signup_phonenum);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirm = findViewById(R.id.et_signup_confirm);
        etDescription = findViewById(R.id.et_signup_description);
        rgRole = findViewById(R.id.rg_role);
        rbUser = findViewById(R.id.rb_user);
        rbAdmin = findViewById(R.id.rb_admin);
        chkTerms = findViewById(R.id.chk_terms);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        progressBar = findViewById(R.id.progress_signup);
        tvAlreadyAccount = findViewById(R.id.tv_already_account);

        // Create account button click
        btnCreateAccount.setOnClickListener(v -> handleSignup());

        // Already have account click - Navigate back to LoginActivity
        tvAlreadyAccount.setOnClickListener(v -> finish());
    }

    private void handleSignup() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phoneNum = etPhoneNum.getText() != null ? etPhoneNum.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        // Get selected role
        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        String role = selectedRoleId == R.id.rb_admin ? "Admin" : "User";

        // Validate input
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (name.length() < 3) {
            etName.setError("Name must be at least 3 characters");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (phoneNum.isEmpty()) {
            etPhoneNum.setError("Phone number is required");
            etPhoneNum.requestFocus();
            return;
        }
        if (phoneNum.length() < 10) {
            etPhoneNum.setError("Enter a valid phone number");
            etPhoneNum.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (confirm.isEmpty()) {
            etConfirm.setError("Please confirm your password");
            etConfirm.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            etConfirm.setError("Passwords do not match");
            etConfirm.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }
        if (!chkTerms.isChecked()) {
            Toast.makeText(this, "Please accept Terms and Privacy", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnCreateAccount.setEnabled(false);

        // Create user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Account created successfully
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Save additional user data to Firestore
                            saveUserToFirestore(user.getUid(), name, email, phoneNum, role, description);
                        }
                    } else {
                        // Account creation failed
                        progressBar.setVisibility(View.GONE);
                        btnCreateAccount.setEnabled(true);

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(SignupActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email,
                                     String phoneNum, String role, String description) {
        // Create user data map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phoneNumber", phoneNum);
        user.put("role", role);
        user.put("description", description);
        user.put("userID", System.currentTimeMillis());

        // Save to Firestore
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);

                    Toast.makeText(SignupActivity.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Navigate back to login screen
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);

                    Toast.makeText(SignupActivity.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}