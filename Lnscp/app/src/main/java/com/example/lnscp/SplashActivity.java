package com.example.lnscp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("user_prefs", 0);
                boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

                Intent intent;
                if (isLoggedIn) {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, SignUpActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                // If anything goes wrong, default to SignUpActivity
                startActivity(new Intent(SplashActivity.this, SignUpActivity.class));
                finish();
            }
        }, SPLASH_DELAY);
    }
} 