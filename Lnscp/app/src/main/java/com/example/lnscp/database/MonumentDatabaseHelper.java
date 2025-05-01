package com.example.lnscp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MonumentDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "monuments.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_MONUMENTS = "monuments";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_BOOKMARKED = "is_bookmarked";

    public MonumentDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_MONUMENTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_IMAGE + " BLOB,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_IS_BOOKMARKED + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add is_bookmarked column to existing table
            try {
                db.execSQL("ALTER TABLE " + TABLE_MONUMENTS + 
                          " ADD COLUMN " + COLUMN_IS_BOOKMARKED + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                // If column already exists or other error, recreate the table
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONUMENTS);
                onCreate(db);
            }
        }
    }

    public void addMonument(String name, Bitmap image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        
        // Convert Bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, stream); // Changed to JPEG with 80% quality
        byte[] byteArray = stream.toByteArray();
        values.put(COLUMN_IMAGE, byteArray);

        long id = db.insert(TABLE_MONUMENTS, null, values);
        db.close();
        
        if (id != -1) {
            Log.d("MonumentDB", "Successfully added monument: " + name);
        } else {
            Log.e("MonumentDB", "Failed to add monument: " + name);
        }
    }

    public List<Monument> getAllMonuments() {
        List<Monument> monumentList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MONUMENTS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        Log.d("MonumentDB", "Found " + cursor.getCount() + " monuments in database");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                byte[] imageBytes = cursor.getBlob(2);
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                String timestamp = cursor.getString(3);
                boolean isBookmarked = cursor.getInt(4) == 1;

                Monument monument = new Monument(id, name, image, timestamp, isBookmarked);
                monumentList.add(monument);
                Log.d("MonumentDB", "Loaded monument: " + name);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return monumentList;
    }

    public boolean isNewMonument(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_MONUMENTS + " WHERE " + COLUMN_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{name});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count == 0;
    }

    public int getUniqueMonumentCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(DISTINCT " + COLUMN_NAME + ") FROM " + TABLE_MONUMENTS;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    public void toggleBookmark(int monumentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        Cursor cursor = db.query(TABLE_MONUMENTS, new String[]{COLUMN_IS_BOOKMARKED},
                COLUMN_ID + "=?", new String[]{String.valueOf(monumentId)}, null, null, null);
        
        if (cursor.moveToFirst()) {
            boolean currentValue = cursor.getInt(0) == 1;
            values.put(COLUMN_IS_BOOKMARKED, !currentValue ? 1 : 0);
            db.update(TABLE_MONUMENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(monumentId)});
            Log.d("MonumentDB", "Toggled bookmark for monument " + monumentId + " to " + (!currentValue));
        }
        cursor.close();
        db.close();
    }

    public List<Monument> getBookmarkedMonuments() {
        List<Monument> monumentList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MONUMENTS + 
                           " WHERE " + COLUMN_IS_BOOKMARKED + "=1" +
                           " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                byte[] imageBytes = cursor.getBlob(2);
                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                String timestamp = cursor.getString(3);
                boolean isBookmarked = cursor.getInt(4) == 1;

                Monument monument = new Monument(id, name, image, timestamp, isBookmarked);
                monumentList.add(monument);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return monumentList;
    }

    public boolean isMonumentBookmarked(int monumentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MONUMENTS, new String[]{COLUMN_IS_BOOKMARKED},
                COLUMN_ID + "=?", new String[]{String.valueOf(monumentId)}, null, null, null);
        
        boolean isBookmarked = false;
        if (cursor.moveToFirst()) {
            isBookmarked = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return isBookmarked;
    }
} 