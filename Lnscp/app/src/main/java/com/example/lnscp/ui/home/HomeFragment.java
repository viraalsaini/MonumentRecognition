package com.example.lnscp.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lnscp.R;
import com.example.lnscp.database.MonumentDatabaseHelper;
import com.example.lnscp.fragments.MonumentDetailsFragment;
import com.example.lnscp.ml.ModelHelper;
import com.example.lnscp.achievements.AchievementManager;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HomeFragment extends Fragment {
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final String TEMP_IMAGE_NAME = "temp_image.jpg";

    private ModelHelper modelHelper;
    private MonumentDatabaseHelper dbHelper;
    private MaterialButton uploadButton;
    private MaterialButton takePhotoButton;
    private File tempImageFile;
    private JSONArray monumentsData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            modelHelper = new ModelHelper(requireContext());
            dbHelper = new MonumentDatabaseHelper(requireContext());
            loadMonumentsData();
        } catch (IOException e) {
            showToast("Error initializing model: " + e.getMessage());
        }
    }

    private void loadMonumentsData() {
        try {
            InputStream is = requireContext().getAssets().open("monuments.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            
            JSONObject obj = new JSONObject(sb.toString());
            monumentsData = obj.getJSONArray("monuments");
            
            // Verify monuments are loaded - don't show toast unless there's an error
            int count = monumentsData.length();
            if (count == 0) {
                showToast("Error: No monuments found in database");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error loading monuments data: " + e.getMessage());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        uploadButton = root.findViewById(R.id.upload_button);
        takePhotoButton = root.findViewById(R.id.take_photo_button);

        uploadButton.setOnClickListener(v -> openGallery());
        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());

        return root;
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            tempImageFile = new File(requireContext().getExternalFilesDir(null), TEMP_IMAGE_NAME);
            Uri photoURI = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.lnscp.fileprovider",
                    tempImageFile
            );
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            showToast("No camera app found");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode != getActivity().RESULT_OK) {
                showToast("Image selection cancelled");
                return;
            }

            Bitmap bitmap = null;
            try {
                if (requestCode == PICK_IMAGE && data != null) {
                    Uri imageUri = data.getData();
                    if (imageUri == null) {
                        showToast("Error: Could not get image URI");
                        return;
                    }
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    if (tempImageFile == null || !tempImageFile.exists()) {
                        showToast("Error: Captured image file not found");
                        return;
                    }
                    bitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath());
                }

                if (bitmap != null) {
                    showToast("Processing image...");
                    processImage(bitmap);
                } else {
                    showToast("Error: Could not load the image");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error processing image: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Unexpected error: " + e.getMessage());
        }
    }

    private void processImage(Bitmap bitmap) {
        try {
            // Show processing toast
            showToast("Analyzing monument...");
            
            // Get prediction from model
            String monumentName = modelHelper.classifyImage(bitmap);
            if (monumentName == null || monumentName.isEmpty()) {
                showToast("Error: Could not identify the monument");
                return;
            }
            
            showToast("Identified: " + monumentName);

            // Normalize monument name for comparison
            String normalizedMonumentName = normalizeMonumentName(monumentName);

            // Find monument details
            JSONObject monumentDetails = null;
            for (int i = 0; monumentsData != null && i < monumentsData.length(); i++) {
                try {
                    JSONObject monument = monumentsData.getJSONObject(i);
                    String dataName = monument.getString("name");
                    String normalizedDataName = normalizeMonumentName(dataName);
                    
                    if (normalizedDataName.equals(normalizedMonumentName) || 
                        normalizedDataName.contains(normalizedMonumentName) || 
                        normalizedMonumentName.contains(normalizedDataName)) {
                        monumentDetails = monument;
                        monumentName = dataName; // Use the exact name from the data
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continue to next monument
                }
            }

            // If monument not found, create a fallback details object
            if (monumentDetails == null) {
                showToast("Using fallback details for: " + monumentName);
                monumentDetails = createFallbackMonumentDetails(monumentName);
            }

            // Verify the structure of monumentDetails
            if (!verifyMonumentDetails(monumentDetails)) {
                showToast("Error: Monument details are incomplete");
                monumentDetails = createFallbackMonumentDetails(monumentName);
            }

            // Save to database for Recents
            try {
                dbHelper.addMonument(monumentName, bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Warning: Failed to save to database: " + e.getMessage());
                // Continue anyway
            }

            // Update unique monuments count if new
            try {
                if (dbHelper.isNewMonument(monumentName)) {
                    SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    int currentCount = prefs.getInt("unique_monuments_count", 0);
                    prefs.edit().putInt("unique_monuments_count", currentCount + 1).apply();
                    
                    // Check achievements for monument discovery
                    AchievementManager.getInstance(requireContext()).checkAchievements(currentCount + 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Continue anyway
            }

            // Navigate to details fragment
            try {
                showToast("Opening details...");
                MonumentDetailsFragment detailsFragment = MonumentDetailsFragment.newInstance(
                    monumentName,
                    bitmap,
                    monumentDetails
                );

                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, detailsFragment)
                    .addToBackStack(null)
                    .commit();
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error navigating to details: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error processing image: " + e.getMessage());
        }
    }
    
    // Verify that the JSON object contains all required fields
    private boolean verifyMonumentDetails(JSONObject details) {
        try {
            if (details == null) return false;
            
            // Check required fields
            if (!details.has("name")) return false;
            if (!details.has("description")) return false;
            if (!details.has("location")) return false;
            if (!details.has("coordinates")) return false;
            
            // Check coordinates
            JSONObject coordinates = details.getJSONObject("coordinates");
            if (!coordinates.has("lat")) return false;
            if (!coordinates.has("lon")) return false;
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to normalize monument names for comparison
    private String normalizeMonumentName(String name) {
        if (name == null) return "";
        // Convert to lowercase, remove extra spaces, punctuation
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    // Create fallback details for monuments not found in JSON
    private JSONObject createFallbackMonumentDetails(String monumentName) {
        try {
            JSONObject details = new JSONObject();
            details.put("name", monumentName);
            
            // Default description
            details.put("description", "This is " + monumentName + ", a famous monument in India. " +
                "It represents an important part of India's cultural heritage and history.");
            
            // Default location
            details.put("location", "New Delhi, India");
            
            // Set coordinates and wiki links based on monument name
            JSONObject coordinates = new JSONObject();
            
            // Set monument-specific details where possible
            if (monumentName.contains("Humayun") || monumentName.contains("Humayu")) {
                coordinates.put("lat", 28.5933);
                coordinates.put("lon", 77.2507);
                details.put("wiki", "https://en.wikipedia.org/wiki/Humayun%27s_Tomb");
                details.put("description", "Humayun's Tomb is the tomb of the Mughal Emperor Humayun in Delhi, India. " +
                    "The tomb was commissioned by Humayun's first wife and chief consort, Empress Bega Begum, in 1569-70. " +
                    "It was the first garden-tomb on the Indian subcontinent and is located in Nizamuddin East, Delhi.");
            } else if (monumentName.contains("Qutub") || monumentName.contains("Qutb")) {
                coordinates.put("lat", 28.5245);
                coordinates.put("lon", 77.1855);
                details.put("wiki", "https://en.wikipedia.org/wiki/Qutb_Minar");
                details.put("description", "The Qutb Minar is a minaret and victory tower that forms part of the Qutb complex. " +
                    "It is a UNESCO World Heritage Site in the Mehrauli area of New Delhi, India. " +
                    "The tower is 73 meters tall and has a base diameter of 14.3 meters which narrows to 2.7 meters at the top.");
            } else if (monumentName.contains("Taj Mahal")) {
                coordinates.put("lat", 27.1751);
                coordinates.put("lon", 78.0421);
                details.put("wiki", "https://en.wikipedia.org/wiki/Taj_Mahal");
                details.put("description", "The Taj Mahal is an ivory-white marble mausoleum on the right bank of the Yamuna river " +
                    "in the Indian city of Agra. It was commissioned in 1632 by the Mughal emperor Shah Jahan to house the tomb of " +
                    "his favourite wife, Mumtaz Mahal.");
            } else if (monumentName.contains("India Gate")) {
                coordinates.put("lat", 28.6129);
                coordinates.put("lon", 77.2295);
                details.put("wiki", "https://en.wikipedia.org/wiki/India_Gate");
                details.put("description", "The India Gate is a war memorial located astride the Rajpath, on the eastern edge of the " +
                    "ceremonial axis of New Delhi, India. It was formerly called the All India War Memorial. The names of the soldiers " +
                    "who died in the First World War are inscribed on the gate.");
            } else if (monumentName.contains("Red Fort")) {
                coordinates.put("lat", 28.6562);
                coordinates.put("lon", 77.2410);
                details.put("wiki", "https://en.wikipedia.org/wiki/Red_Fort");
                details.put("description", "The Red Fort is a historic fort in the city of Delhi that served as the main residence of the " +
                    "Mughal Emperors. Emperor Shah Jahan commissioned construction of the Red Fort on 12 May 1638, when he decided to shift " +
                    "his capital from Agra to Delhi.");
            } else {
                // Generate monument-specific wiki link and provide coordinates for Delhi
                String normalizedName = normalizeMonumentName(monumentName);
                String wikiLink = "https://en.wikipedia.org/wiki/" + normalizedName.replace(" ", "_");
                details.put("wiki", wikiLink);
                
                // Use general coordinates for New Delhi
                coordinates.put("lat", 28.6139);
                coordinates.put("lon", 77.2090);
            }
            
            details.put("coordinates", coordinates);
            return details;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (modelHelper != null) {
            modelHelper.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
} 
