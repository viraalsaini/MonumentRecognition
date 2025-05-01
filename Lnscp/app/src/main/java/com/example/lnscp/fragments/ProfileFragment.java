package com.example.lnscp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lnscp.R;
import com.example.lnscp.database.MonumentDatabaseHelper;
import com.example.lnscp.achievements.Achievement;
import com.example.lnscp.achievements.AchievementManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class ProfileFragment extends Fragment {
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private TextView monumentsDiscoveredText;
    private RecyclerView achievementsRecycler;
    private MaterialButton viewBookmarksButton;
    private SwitchMaterial switchTheme;
    private MonumentDatabaseHelper dbHelper;
    private AchievementManager achievementManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new MonumentDatabaseHelper(requireContext());
        achievementManager = AchievementManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        monumentsDiscoveredText = view.findViewById(R.id.monuments_discovered);
        achievementsRecycler = view.findViewById(R.id.achievements_recycler);
        viewBookmarksButton = view.findViewById(R.id.view_bookmarks_button);
        switchTheme = view.findViewById(R.id.switchTheme);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerView
        achievementsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        achievementsRecycler.setAdapter(new AchievementsAdapter(achievementManager.getAchievements()));

        // Load user data
        loadUserData();

        // Set up bookmarks button
        viewBookmarksButton.setOnClickListener(v -> {
            // Navigate to bookmarks fragment
            Fragment bookmarksFragment = new BookmarksFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, bookmarksFragment)
                .addToBackStack(null)
                .commit();
        });

        // Set up dark mode switch
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        switchTheme.setChecked(isDarkMode);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Update stats
        updateStats();
    }

    private void loadUserData() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "User");
        String email = prefs.getString("user_email", "email@example.com");
        String phone = prefs.getString("user_phone", "No phone number");

        tvUserName.setText(name);
        tvUserEmail.setText(email);
        tvUserPhone.setText(phone);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Get fresh data from database for monuments count
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int currentCount = prefs.getInt("unique_monuments_count", 0);
        int actualCount = dbHelper.getUniqueMonumentCount();
        
        // Make sure the monuments count in SharedPreferences is accurate
        if (currentCount != actualCount) {
            prefs.edit().putInt("unique_monuments_count", actualCount).apply();
        }
        
        // Update the displayed stats and achievements
        updateStats();
    }

    private void updateStats() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int monumentsCount = prefs.getInt("unique_monuments_count", 0);
        monumentsDiscoveredText.setText("Monuments Discovered: " + monumentsCount);

        // Update monument count achievements
        achievementManager.checkAchievements(monumentsCount);
        
        // Check for bookmarked monuments achievement
        int bookmarksCount = dbHelper.getBookmarkedMonuments().size();
        achievementManager.checkBookmarkAchievement(bookmarksCount);
        
        // Ensure the Memories Saved achievement is properly checked every time
        if (bookmarksCount > 0) {
            achievementManager.unlockAchievement(AchievementManager.MEMORIES_SAVED);
        }
        
        // Update UI
        achievementsRecycler.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder> {
        private final List<Achievement> achievements;

        public AchievementsAdapter(List<Achievement> achievements) {
            this.achievements = achievements;
        }

        @NonNull
        @Override
        public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
            return new AchievementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
            Achievement achievement = achievements.get(position);
            holder.bind(achievement);
        }

        @Override
        public int getItemCount() {
            return achievements.size();
        }

        class AchievementViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleText;
            private final TextView descriptionText;
            private final android.widget.ImageView iconImage;
            private final TextView progressText;

            public AchievementViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.achievement_title);
                descriptionText = itemView.findViewById(R.id.achievement_description);
                iconImage = itemView.findViewById(R.id.achievement_icon);
                progressText = itemView.findViewById(R.id.achievement_progress);
            }

            public void bind(Achievement achievement) {
                titleText.setText(achievement.getTitle());
                descriptionText.setText(achievement.getDescription());
                
                // Show the unlocked or locked icon based on achievement status
                iconImage.setImageResource(achievement.getIconResId());
                
                // Show progress for multi-step achievements
                if (achievement.getMaxProgress() > 1) {
                    progressText.setVisibility(View.VISIBLE);
                    progressText.setText(achievement.getProgressText());
                } else {
                    // For binary achievements, just show Unlocked/Locked
                    progressText.setVisibility(View.VISIBLE);
                    progressText.setText(achievement.isUnlocked() ? "Unlocked" : "Locked");
                }
            }
        }
    }
} 