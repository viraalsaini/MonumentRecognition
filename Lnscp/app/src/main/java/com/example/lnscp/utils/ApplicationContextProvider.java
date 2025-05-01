package com.example.lnscp.utils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ApplicationContextProvider extends Application {
    private static Context applicationContext;
    private static final String TAG = "AppContextProvider";

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
    }

    public static Context getContext() {
        return applicationContext;
    }
    
    /**
     * Load a JSON object from the assets folder
     * @param fileName The name of the file in the assets folder (e.g. "metro.json")
     * @return JSONObject representing the file contents, or null if error
     */
    public static JSONObject getJSONFromAsset(String fileName) {
        if (applicationContext == null) {
            Log.e(TAG, "Application context is null, cannot load JSON");
            return null;
        }
        
        try {
            InputStream is = applicationContext.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonString = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            
            reader.close();
            is.close();
            
            return new JSONObject(jsonString.toString());
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading JSON from asset: " + fileName, e);
            return null;
        }
    }
} 