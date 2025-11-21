package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    // Views
    private TextInputEditText etName, etEmail, etPhoneNum, etPassword, etConfirm, etDescription;
    private RadioGroup rgGender;
    private CheckBox chkTerms;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private TextView tvAlreadyAccount;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Keep these to re-sign-in for verification checks / resend
    private String pendingEmail, pendingPassword, pendingName, pendingPhone, pendingDesc;
    private int pendingGender; // 1=male, 0=female

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        rgGender = findViewById(R.id.rg_gender);
        chkTerms = findViewById(R.id.chk_terms);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        progressBar = findViewById(R.id.progress_signup);
        tvAlreadyAccount = findViewById(R.id.tv_already_account);

        //trim spaces in email/phone inputs
        etEmail.setFilters(new InputFilter[]{(s, a, b, d, e, f) -> s != null ? s.toString().replace(" ", "") : null});
        etPhoneNum.setFilters(new InputFilter[]{(s, a, b, d, e, f) -> s != null ? s.toString().replace(" ", "") : null});

        btnCreateAccount.setOnClickListener(v -> handleSignup());
        tvAlreadyAccount.setOnClickListener(v -> finish());
    }

    private void handleSignup() {
        final String name = getTextTrim(etName);
        final String email = getTextTrim(etEmail);
        final String phoneNum = getTextTrim(etPhoneNum);
        final String password = getTextTrim(etPassword);
        final String confirm = getTextTrim(etConfirm);
        final String description = getTextTrim(etDescription);

        // Gender
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) { toast("Please select gender"); return; }
        int genderValue = (selectedGenderId == R.id.rb_male) ? 1 : 0; // 1=Male, 0=Female

        // Validation
        if (name.isEmpty()) { setErr(etName, "Name is required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { setErr(etEmail, "Enter a valid email"); return; }
        if (phoneNum.replaceAll("\\D", "").length() < 9) { setErr(etPhoneNum, "Enter a valid phone number"); return; }
        if (password.length() < 6) { setErr(etPassword, "Password must be at least 6 characters"); return; }
        if (!password.equals(confirm)) { setErr(etConfirm, "Passwords do not match"); return; }
        if (description.isEmpty()) { setErr(etDescription, "Description is required"); return; }
        if (!chkTerms.isChecked()) { toast("Please accept Terms and Privacy"); return; }

        setLoading(true);

        // Create Auth user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    //if create fail
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        toast(task.getException() != null ? task.getException().getMessage() : "Registration failed");
                        return;
                    }
                    FirebaseUser fbUser = mAuth.getCurrentUser();
                    if (fbUser == null) {
                        setLoading(false);
                        toast("Registration failed: user is null");
                        return;
                    }

                    // Update displayName
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    fbUser.updateProfile(profile);

                    // Send verification email
                    fbUser.sendEmailVerification()
                            .addOnSuccessListener(unused -> toast("Verification email sent to " + email + ". Check inbox/spam."))
                            .addOnFailureListener(e -> toast("Failed to send verification email: " + e.getMessage()));

                    //Store pending data
                    pendingEmail = email;
                    pendingPassword = password;
                    pendingName = name;
                    pendingPhone = phoneNum;
                    pendingDesc = description;
                    pendingGender = genderValue;

                    // Sign out and show a simple dialog to complete verification
                    mAuth.signOut();
                    setLoading(false);
                    showVerifyDialog();
                });
    }

    /** Shows a simple AlertDialog with 3 actions: Open Email, I've Verified, Resend */
    private void showVerifyDialog() {
        String msg = "We sent a verification link to:\n" + pendingEmail +
                "\n\nOpen your email and tap the link, then return here and press \"I've Verified\".";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verify your email")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("I've Verified", (d, which) -> checkVerifiedAndActivate())
                .setNegativeButton("Open Email App", (d, which) -> {
                    openEmailApp();
                    // Keep dialog open behavior: show again after returning
                    showVerifyDialog();
                })
                .setNeutralButton("Resend", (d, which) -> resendVerification(() -> showVerifyDialog()))
                .create();
        dialog.show();
    }

    /** Re-sign in, reload, check isEmailVerified; if verified, create Firestore profile and finish */
    private void checkVerifiedAndActivate() {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(pendingEmail, pendingPassword)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        toast(task.getException() != null ? task.getException().getMessage() : "Sign-in failed");
                        showVerifyDialog();
                        return;
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        toast("Sign-in failed: user is null");
                        showVerifyDialog();
                        return;
                    }
                    user.reload().addOnCompleteListener(r -> {
                        if (!r.isSuccessful()) {
                            setLoading(false);
                            toast("Failed to reload user");
                            showVerifyDialog();
                            return;
                        }
                        if (!user.isEmailVerified()) {
                            setLoading(false);
                            toast("Email not verified yet. Please click the link in your inbox.");
                            showVerifyDialog();
                            return;
                        }
                        // VERIFIED — now create Firestore profile
                        createProfileThenFinish(user);
                    });
                });
    }

    /** Resends verification email (requires sign-in), then runs onDone (usually re-open dialog) */
    private void resendVerification(Runnable onDone) {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(pendingEmail, pendingPassword)
                .addOnSuccessListener(auth -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        toast("Resend failed: user is null");
                        if (onDone != null) onDone.run();
                        return;
                    }
                    user.sendEmailVerification()
                            .addOnSuccessListener(u -> {
                                setLoading(false);
                                toast("Verification email resent to " + pendingEmail);
                                if (onDone != null) onDone.run();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Failed to resend email: " + e.getMessage());
                                if (onDone != null) onDone.run();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Sign-in failed: " + e.getMessage());
                    if (onDone != null) onDone.run();
                });
    }

    private void createProfileThenFinish(FirebaseUser fbUser) {
        final String uid = fbUser.getUid();
        String generatedID = "U" + System.currentTimeMillis();

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("name", pendingName);
        userDoc.put("email", pendingEmail);
        userDoc.put("phoneNumber", pendingPhone);
        userDoc.put("description", pendingDesc);
        userDoc.put("gender", pendingGender);
        userDoc.put("profilePic", null);
        userDoc.put("userID", generatedID);

        // Always save to "user" collection
        db.collection("user")
                .document(uid)
                .set(userDoc)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    toast("Email verified. Account activated!");
                    // Go to login (or MainActivity if you prefer)
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Failed to create profile: " + e.getMessage());
                });
    }

    private void openEmailApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
        if (intent != null) {
            startActivity(intent);
        } else {
            Intent mailIntent = new Intent(Intent.ACTION_MAIN);
            mailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
            startActivity(Intent.createChooser(mailIntent, "Open email app"));
        }
    }

    // ---------- helpers ----------
    private String getTextTrim(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private void setErr(TextInputEditText et, String msg) {
        et.setError(msg); et.requestFocus();
    }
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setEnabled(!loading);
    }
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}