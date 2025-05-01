package com.example.lnscp.models;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private int currentProgress;
    private int targetProgress;
    private boolean unlocked;
    private String iconResId;

    public Achievement(String id, String title, String description, int currentProgress, int targetProgress, boolean unlocked, String iconResId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currentProgress = currentProgress;
        this.targetProgress = targetProgress;
        this.unlocked = unlocked;
        this.iconResId = iconResId;
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

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public String getIconResId() {
        return iconResId;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
        if (this.currentProgress >= this.targetProgress) {
            this.unlocked = true;
        }
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
} 