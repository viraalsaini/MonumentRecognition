package com.example.lnscp.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.lnscp.R;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {
    private static final String TAG = "AchievementManager";
    private static AchievementManager instance;
    private final List<Achievement> achievements;
    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // Monument discovery achievements
    public static final String FIRST_DISCOVERY = "first_discovery";
    public static final String HERITAGE_HUNTER = "heritage_hunter";
    public static final String INDIANA_JONES = "indiana_jones";
    public static final String MASTER_HISTORIAN = "master_historian";

    // Feature usage achievements
    public static final String MEMORIES_SAVED = "memories_saved";
    
    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
        achievements = new ArrayList<>();
        initializeAchievements();
        loadAchievementProgress();
    }

    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }

    private void initializeAchievements() {
        // Monument discovery achievements
        achievements.add(new Achievement(
            FIRST_DISCOVERY,
            "First Discovery",
            "Scan your first monument",
            R.drawable.achievement_first_discovery,
            R.drawable.locked
        ));

        achievements.add(new Achievement(
            HERITAGE_HUNTER,
            "Heritage Hunter",
            "Scan 5 monuments",
            R.drawable.achievement_heritage_hunter,
            R.drawable.locked,
            5
        ));

        achievements.add(new Achievement(
            INDIANA_JONES,
            "Indiana Jones",
            "Scan 10 monuments",
            R.drawable.achievement_indiana_jones,
            R.drawable.locked,
            10
        ));

        achievements.add(new Achievement(
            MASTER_HISTORIAN,
            "Master Historian",
            "Scan all 17 monuments",
            R.drawable.achievement_master_historian,
            R.drawable.locked,
            17
        ));

        // Feature usage achievements
        achievements.add(new Achievement(
            MEMORIES_SAVED,
            "Memories Saved",
            "Bookmark your first monument",
            R.drawable.achievement_memories_saved,
            R.drawable.locked
        ));
    }

    private void loadAchievementProgress() {
        for (Achievement achievement : achievements) {
            boolean unlocked = preferences.getBoolean(achievement.getId() + "_unlocked", false);
            int progress = preferences.getInt(achievement.getId() + "_progress", 0);
            achievement.setUnlocked(unlocked);
            achievement.setCurrentProgress(progress);
        }
    }

    public void saveAchievementProgress() {
        for (Achievement achievement : achievements) {
            editor.putBoolean(achievement.getId() + "_unlocked", achievement.isUnlocked());
            editor.putInt(achievement.getId() + "_progress", achievement.getCurrentProgress());
        }
        editor.apply();
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }

    public void checkAchievements(int monumentsCount) {
        boolean anyUnlocked = false;
        String lastUnlockedName = "";
        
        for (Achievement achievement : achievements) {
            boolean wasUnlocked = achievement.isUnlocked();
            
            switch (achievement.getId()) {
                case FIRST_DISCOVERY:
                    if (monumentsCount >= 1) {
                        achievement.setCurrentProgress(1);
                    }
                    break;
                case HERITAGE_HUNTER:
                    achievement.setCurrentProgress(Math.min(monumentsCount, 5));
                    break;
                case INDIANA_JONES:
                    achievement.setCurrentProgress(Math.min(monumentsCount, 10));
                    break;
                case MASTER_HISTORIAN:
                    achievement.setCurrentProgress(Math.min(monumentsCount, 17));
                    break;
            }
            
            // Check if this achievement was just unlocked
            if (!wasUnlocked && achievement.isUnlocked()) {
                anyUnlocked = true;
                lastUnlockedName = achievement.getTitle();
                Log.d(TAG, "Achievement unlocked: " + achievement.getTitle());
            }
        }
        
        saveAchievementProgress();
        
        // Show toast if any achievement was unlocked
        if (anyUnlocked && context != null) {
            Toast.makeText(context, 
                "Achievement Unlocked: " + lastUnlockedName, 
                Toast.LENGTH_LONG).show();
        }
    }

    public void unlockAchievement(String achievementId) {
        for (Achievement achievement : achievements) {
            if (achievement.getId().equals(achievementId)) {
                if (!achievement.isUnlocked()) {
                    achievement.setCurrentProgress(achievement.getMaxProgress());
                    achievement.setUnlocked(true);
                    saveAchievementProgress();
                    Log.d(TAG, "Achievement unlocked: " + achievement.getTitle());
                    
                    // Show toast message
                    if (context != null) {
                        Toast.makeText(context, 
                            "Achievement Unlocked: " + achievement.getTitle(), 
                            Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
        }
    }

    public void checkFeatureAchievement(String featureId) {
        unlockAchievement(featureId);
    }
    
    // Check if the bookmark achievement should be unlocked
    public void checkBookmarkAchievement(int bookmarkedCount) {
        if (bookmarkedCount > 0) {
            unlockAchievement(MEMORIES_SAVED);
        }
    }
} 