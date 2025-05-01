package com.example.lnscp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lnscp.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private EditText etName, etEmail, etPhone;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        etName = binding.etName;
        etEmail = binding.etEmail;
        etPhone = binding.etPhone;
        btnSignUp = binding.btnSignUp;

        btnSignUp.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (validateInputs(name, email, phone)) {
                try {
                    // Save user data
                    SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
                    editor.putString("user_name", name);
                    editor.putString("user_email", email);
                    editor.putString("user_phone", phone);
                    editor.putBoolean("is_logged_in", true);
                    editor.apply();

                    // Redirect to MainActivity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean validateInputs(String name, String email, String phone) {
        if (name.isEmpty()) {
            etName.setError("Name is required");
            return false;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return false;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Phone is required");
            return false;
        }
        return true;
    }
} 