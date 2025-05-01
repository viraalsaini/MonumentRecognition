package com.example.lnscp.achievements;

public class Achievement {
    private final String id;
    private final String title;
    private final String description;
    private final int iconResId;
    private final int lockedIconResId;
    private boolean unlocked;
    private int currentProgress;
    private final int maxProgress;

    public Achievement(String id, String title, String description, int iconResId, int lockedIconResId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.lockedIconResId = lockedIconResId;
        this.unlocked = false;
        this.currentProgress = 0;
        this.maxProgress = 1; // Default to 1 for boolean achievements
    }

    public Achievement(String id, String title, String description, int iconResId, int lockedIconResId, int maxProgress) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.lockedIconResId = lockedIconResId;
        this.unlocked = false;
        this.currentProgress = 0;
        this.maxProgress = maxProgress;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return unlocked ? iconResId : lockedIconResId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int progress) {
        this.currentProgress = progress;
        if (progress >= maxProgress) {
            this.unlocked = true;
        }
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public String getProgressText() {
        return currentProgress + "/" + maxProgress;
    }
} 