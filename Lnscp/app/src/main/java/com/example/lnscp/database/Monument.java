package com.example.lnscp.database;

import android.graphics.Bitmap;

public class Monument {
    private int id;
    private String name;
    private Bitmap image;
    private String timestamp;
    private boolean isBookmarked;

    public Monument(int id, String name, Bitmap image, String timestamp, boolean isBookmarked) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.timestamp = timestamp;
        this.isBookmarked = isBookmarked;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }
} 