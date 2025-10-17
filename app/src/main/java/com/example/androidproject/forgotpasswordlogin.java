//package com.example.androidproject;
//
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.InputType;
//import android.util.Log;
//import android.util.Patterns;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.android.material.textfield.TextInputLayout;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseAuthInvalidUserException;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.net.UnknownHostException;
//
//public class forgotpasswordlogin extends AppCompatActivity {
//
//    private TextInputEditText etEmail, etPassword;
//    private Button btnLogin;
//    private ProgressBar progressBar;
//    private TextView tvForgot, tvSignup;
//
//    // Firebase Authentication
//    private FirebaseAuth mAuth;
//    private FirebaseFirestore db;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        // Initialize Firebase Auth and Firestore
//        mAuth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//
//        // Initialize views
//        etEmail = findViewById(R.id.et_email);
//        etPassword = findViewById(R.id.et_password);
//        btnLogin = findViewById(R.id.btn_login);
//        progressBar = findViewById(R.id.progress);
//        tvForgot = findViewById(R.id.tv_forgot);
//        tvSignup = findViewById(R.id.tv_signup);
//
//        // Login button click
//        btnLogin.setOnClickListener(v -> handleLogin());
//
//        // Forgot password click
//        tvForgot.setOnClickListener(v -> handleForgotPassword());
//
//        // Sign up click - Navigate to SignupActivity
//        tvSignup.setOnClickListener(v -> {
//            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
//            startActivity(intent);
//        });
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        mAuth.signOut(); // force logout
//    }
//
//    private void handleForgotPassword() {
//        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
//
//        // Create a dialog for forgot password
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Reset Password");
//        builder.setMessage("Enter your email address and we'll send you a password reset link.");
//
//        // Create input field
//        final TextInputLayout inputLayout = new TextInputLayout(this);
//        final TextInputEditText input = new TextInputEditText(this);
//        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
//        input.setHint("Email address");
//        inputLayout.addView(input);
//
//        // Set margins
//        int margin = (int) (16 * getResources().getDisplayMetrics().density);
//        inputLayout.setPadding(margin, 0, margin, 0);
//
//        if (!email.isEmpty()) {
//            input.setText(email); // Pre-fill with email from login field
//        }
//
//        builder.setView(inputLayout);
//
//        builder.setPositiveButton("Send Reset Link", null); // We'll override this
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        // Override the positive button to prevent dialog dismissal on validation error
//        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
//            String resetEmail = input.getText().toString().trim();
//            if (validateResetEmail(resetEmail)) {
//                sendResetEmail(resetEmail);
//                dialog.dismiss();
//            }
//        });
//    }
//
//    private boolean validateResetEmail(String email) {
//        if (email.isEmpty()) {
//            Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        return true;
//    }
//
//    private void sendResetEmail(String email) {
//        progressBar.setVisibility(View.VISIBLE);
//
//        mAuth.sendPasswordResetEmail(email)
//                .addOnCompleteListener(task -> {
//                    progressBar.setVisibility(View.GONE);
//
//                    if (task.isSuccessful()) {
//                        showSuccessDialog(email);
//                    } else {
//                        handleResetError(task.getException());
//                    }
//                });
//    }
//
//    private void showSuccessDialog(String email) {
//        new AlertDialog.Builder(this)
//                .setTitle("Check Your Email")
//                .setMessage("We've sent password reset instructions to:\n\n" + email + "\n\nPlease check your inbox and follow the instructions to reset your password.")
//                .setPositiveButton("OK", null)
//                .setIcon(android.R.drawable.ic_dialog_info)
//                .show();
//    }
//
//    private void handleResetError(Exception exception) {
//        String errorMessage = "Failed to send reset email. Please try again.";
//
//        if (exception != null) {
//            String exceptionMessage = exception.getMessage();
//            if (exception instanceof FirebaseAuthInvalidUserException) {
//                errorMessage = "No account found with this email address. Please check your email or sign up for a new account.";
//            } else if (exceptionMessage != null && exceptionMessage.toLowerCase().contains("network")) {
//                errorMessage = "Network error. Please check your internet connection and try again.";
//            } else if (exceptionMessage != null && exceptionMessage.contains("invalid-email")) {
//                errorMessage = "Invalid email address format. Please check your email and try again.";
//            } else if (exception.getCause() instanceof UnknownHostException) {
//                errorMessage = "No internet connection. Please check your network and try again.";
//            } else {
//                errorMessage = "Error: " + exceptionMessage;
//            }
//        }
//
//        new AlertDialog.Builder(this)
//                .setTitle("Reset Failed")
//                .setMessage(errorMessage)
//                .setPositiveButton("OK", null)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();
//    }
//
//    private void handleLogin() {
//        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
//        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
//
//        if (email.isEmpty()) {
//            etEmail.setError("Email is required");
//            etEmail.requestFocus();
//            return;
//        }
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            etEmail.setError("Enter a valid email");
//            etEmail.requestFocus();
//            return;
//        }
//        if (password.isEmpty()) {
//            etPassword.setError("Password is required");
//            etPassword.requestFocus();
//            return;
//        }
//        if (password.length() < 6) {
//            etPassword.setError("Password must be at least 6 characters");
//            etPassword.requestFocus();
//            return;
//        }
//
//        progressBar.setVisibility(View.VISIBLE);
//        btnLogin.setEnabled(false);
//
//        Log.d("LoginDebug", "Attempting login for: " + email);
//
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    progressBar.setVisibility(View.GONE);
//                    btnLogin.setEnabled(true);
//
//                    if (task.isSuccessful()) {
//                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
//                        if (firebaseUser != null) {
//                            Log.d("LoginDebug", "Login successful, UID: " + firebaseUser.getUid());
//                            // Check if email is verified
//                            if (!firebaseUser.isEmailVerified()) {
//                                showEmailVerificationDialog(firebaseUser);
//                            } else {
//                                // Check role by UID in Firestore
//                                checkUserRole(firebaseUser.getUid());
//                            }
//                        } else {
//                            Log.e("LoginDebug", "Login succeeded but no user found");
//                            Toast.makeText(LoginActivity.this, "Login succeeded but no user found.", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        String errorMessage = "Authentication failed";
//                        Exception exception = task.getException();
//
//                        if (exception != null) {
//                            if (exception instanceof FirebaseAuthInvalidUserException) {
//                                errorMessage = "No account found with this email address.";
//                            } else if (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("network")) {
//                                errorMessage = "Network error. Please check your internet connection.";
//                            } else if (exception.getCause() instanceof UnknownHostException) {
//                                errorMessage = "No internet connection. Please check your network and try again.";
//                            } else {
//                                errorMessage = exception.getMessage();
//                            }
//                        }
//
//                        Log.e("LoginDebug", "Login failed: " + errorMessage);
//                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
//                    }
//                });
//    }
//
//    private void showEmailVerificationDialog(FirebaseUser user) {
//        new AlertDialog.Builder(this)
//                .setTitle("Email Not Verified")
//                .setMessage("Your email address has not been verified. Would you like us to send a new verification email?")
//                .setPositiveButton("Send Verification", (dialog, which) -> {
//                    sendVerificationEmail(user);
//                })
//                .setNegativeButton("Later", (dialog, which) -> {
//                    Toast.makeText(LoginActivity.this, "Please verify your email to access all features.", Toast.LENGTH_LONG).show();
//                    mAuth.signOut();
//                })
//                .setCancelable(false)
//                .show();
//    }
//
//    private void sendVerificationEmail(FirebaseUser user) {
//        progressBar.setVisibility(View.VISIBLE);
//
//        user.sendEmailVerification()
//                .addOnCompleteListener(task -> {
//                    progressBar.setVisibility(View.GONE);
//
//                    if (task.isSuccessful()) {
//                        Toast.makeText(LoginActivity.this,
//                                "Verification email sent to " + user.getEmail(),
//                                Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(LoginActivity.this,
//                                "Failed to send verification email: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
//                                Toast.LENGTH_LONG).show();
//                    }
//                    mAuth.signOut();
//                });
//    }
//
//    private void checkUserRole(String uid) {
//        Log.d("LoginDebug", "Checking role for UID: " + uid);
//
//        // Check Admin collection first
//        db.collection("admin").document(uid).get()
//                .addOnSuccessListener(adminDoc -> {
//                    if (adminDoc.exists()) {
//                        Log.d("LoginDebug", "Admin document found, redirecting to AdminProfileActivity");
//                        Intent intent = new Intent(LoginActivity.this, AdminProfileActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        finish();
//                    } else {
//                        Log.d("LoginDebug", "Admin document not found, checking user collection");
//                        // Check User collection next
//                        db.collection("user").document(uid).get()
//                                .addOnSuccessListener(userDoc -> {
//                                    if (userDoc.exists()) {
//                                        Log.d("LoginDebug", "User document found, redirecting to UserExploreActivity");
//                                        // ✅ User detected -> Go to UserExploreActivity
//                                        Intent intent = new Intent(LoginActivity.this, UserExploreActivity.class);
//                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                                        startActivity(intent);
//                                        finish();
//                                    } else {
//                                        Log.d("LoginDebug", "User document also not found, user has no profile");
//                                        Toast.makeText(LoginActivity.this,
//                                                "Account exists but no profile found. Please contact support.",
//                                                Toast.LENGTH_LONG).show();
//                                        mAuth.signOut();
//                                    }
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.e("LoginDebug", "Error checking user data: " + e.getMessage());
//                                    Toast.makeText(LoginActivity.this, "Error checking user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                });
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("LoginDebug", "Error checking admin data: " + e.getMessage());
//                    Toast.makeText(LoginActivity.this, "Error checking admin data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//}