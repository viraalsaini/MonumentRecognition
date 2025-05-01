package com.example.lnscp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lnscp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;
import com.example.lnscp.achievements.AchievementManager;
import com.example.lnscp.database.MonumentDatabaseHelper;
import com.example.lnscp.database.Monument;

import java.util.List;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MonumentDetailsFragment extends Fragment {
    private static final String ARG_MONUMENT_NAME = "monument_name";
    private static final String ARG_MONUMENT_IMAGE = "monument_image";
    private static final String ARG_MONUMENT_DATA = "monument_data";

    private String monumentName;
    private Bitmap monumentImage;
    private JSONObject monumentData;
    private LinearLayout nearbyRestaurantsContainer;
    private LinearLayout nearbyCafesContainer;
    private ProgressBar restaurantsLoading;
    private ProgressBar cafesLoading;
    private TextView nearestMetroText;
    private MaterialButton bookmarkButton;
    private MonumentDatabaseHelper dbHelper;

    public static MonumentDetailsFragment newInstance(String name, Bitmap image, JSONObject data) {
        MonumentDetailsFragment fragment = new MonumentDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MONUMENT_NAME, name);
        args.putParcelable(ARG_MONUMENT_IMAGE, image);
        args.putString(ARG_MONUMENT_DATA, data.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dbHelper = new MonumentDatabaseHelper(requireContext());
            if (getArguments() != null) {
                monumentName = getArguments().getString(ARG_MONUMENT_NAME, "Unknown Monument");
                monumentImage = getArguments().getParcelable(ARG_MONUMENT_IMAGE);
                try {
                    String jsonStr = getArguments().getString(ARG_MONUMENT_DATA);
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        monumentData = new JSONObject(jsonStr);
                    } else {
                        monumentData = createEmptyMonumentData();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Error parsing monument data: " + e.getMessage());
                    monumentData = createEmptyMonumentData();
                }
            } else {
                monumentName = "Unknown Monument";
                monumentData = createEmptyMonumentData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error initializing: " + e.getMessage());
            monumentName = "Unknown Monument";
            monumentData = createEmptyMonumentData();
        }
    }

    private JSONObject createEmptyMonumentData() {
        try {
            JSONObject data = new JSONObject();
            data.put("name", monumentName != null ? monumentName : "Unknown Monument");
            data.put("description", "Description not available");
            data.put("location", "Location not available");
            
            JSONObject coordinates = new JSONObject();
            coordinates.put("lat", 28.6129); // Default to India Gate
            coordinates.put("lon", 77.2295);
            data.put("coordinates", coordinates);
            
            data.put("wiki", "https://en.wikipedia.org/wiki/Monuments_of_India");
            
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monument_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            ImageView imageView = view.findViewById(R.id.monument_image);
            TextView nameView = view.findViewById(R.id.monument_name);
            TextView descriptionView = view.findViewById(R.id.monument_description);
            MaterialButton mapButton = view.findViewById(R.id.view_on_map);
            MaterialButton wikiButton = view.findViewById(R.id.view_wikipedia);
            nearbyRestaurantsContainer = view.findViewById(R.id.nearby_restaurants_container);
            nearbyCafesContainer = view.findViewById(R.id.nearby_cafes_container);
            restaurantsLoading = view.findViewById(R.id.restaurants_loading);
            cafesLoading = view.findViewById(R.id.cafes_loading);
            nearestMetroText = view.findViewById(R.id.nearest_metro);
            bookmarkButton = view.findViewById(R.id.bookmark_button);
            
            // Set up back button directly from layout
            ImageView backButton = view.findViewById(R.id.back_button);
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().onBackPressed();
                    }
                });
            }

            if (monumentImage != null) {
                imageView.setImageBitmap(monumentImage);
            }
            nameView.setText(monumentName);

            try {
                if (monumentData != null) {
                    // Set description
                    try {
                        String description = monumentData.optString("description", "No description available");
                        descriptionView.setText(description);
                    } catch (Exception e) {
                        e.printStackTrace();
                        descriptionView.setText("Description not available");
                    }

                    // Get coordinates
                    try {
                        JSONObject coordinates = monumentData.getJSONObject("coordinates");
                        double lat = coordinates.getDouble("lat");
                        double lon = coordinates.getDouble("lon");

                        // Fetch nearby places
                        fetchNearbyPlaces(lat, lon);
                    } catch (Exception e) {
                        e.printStackTrace();
                        nearestMetroText.setText("Location data not available");
                    }

                    // Set up map button
                    mapButton.setOnClickListener(v -> {
                        try {
                            String location = monumentData.optString("location", "India");
                            
                            // Use geo: URI with coordinates for more reliable mapping
                            double mapLat = 28.6129;
                            double mapLon = 77.2295;
                            
                            try {
                                JSONObject coordinates = monumentData.getJSONObject("coordinates");
                                mapLat = coordinates.getDouble("lat");
                                mapLon = coordinates.getDouble("lon");
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Continue with default coordinates
                            }
                            
                            Uri geoUri = Uri.parse("geo:" + mapLat + "," + mapLon + "?q=" + 
                                Uri.encode(monumentName + ", " + location));
                            Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
                            
                            // Check if there's an app available to handle this intent
                            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                                startActivity(intent);
                            } else {
                                // Fallback to web-based Google Maps
                                Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + 
                                    Uri.encode(monumentName + ", " + location));
                                Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                                startActivity(webIntent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("Error opening map");
                        }
                    });

                    // Set up wiki button
                    wikiButton.setOnClickListener(v -> {
                        try {
                            String wikiUrl = monumentData.optString("wiki", "");
                            if (wikiUrl == null || wikiUrl.isEmpty()) {
                                // Fallback to search on Wikipedia
                                wikiUrl = "https://en.wikipedia.org/wiki/Special:Search?search=" + 
                                    Uri.encode(monumentName);
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Fallback to search on Wikipedia
                            String searchUrl = "https://en.wikipedia.org/wiki/Special:Search?search=" + 
                                Uri.encode(monumentName);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl));
                            startActivity(intent);
                        }
                    });

                    setupBookmarkButton();
                    
                    // Update monument count if this is a new monument
                    updateMonumentCount();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error loading monument details: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error initializing view: " + e.getMessage());
        }
    }

    private void fetchNearbyPlaces(double lat, double lon) {
        String monumentName = getMonumentNameFromCoordinates(lat, lon);
        
        if (monumentName != null) {
            // Clear existing containers
            restaurantsLoading.setVisibility(View.GONE);
            cafesLoading.setVisibility(View.GONE);
            nearbyRestaurantsContainer.removeAllViews();
            nearbyCafesContainer.removeAllViews();
            
            // 1. Display restaurant link from food.json
            String restaurantLink = getRestaurantLinkForMonument(monumentName);
            if (restaurantLink != null && !restaurantLink.isEmpty()) {
                addLinkCard(nearbyRestaurantsContainer, "Nearby Restaurants", restaurantLink);
            } else {
                addPlainTextCard(nearbyRestaurantsContainer, "No restaurant information available");
            }
            
            // 2. Display cafe/fast food link from food.json
            String cafeLink = getCafeLinkForMonument(monumentName);
            if (cafeLink != null && !cafeLink.isEmpty()) {
                addLinkCard(nearbyCafesContainer, "Nearby Cafes & Fast Food", cafeLink);
            } else {
                addPlainTextCard(nearbyCafesContainer, "No cafe information available");
            }
            
            // 3. Display metro information from metro.txt and metro.json
            displayMetroInformation(monumentName);
        } else {
            // Fallback for unknown monuments
            restaurantsLoading.setVisibility(View.GONE);
            cafesLoading.setVisibility(View.GONE);
            addPlainTextCard(nearbyRestaurantsContainer, "No restaurant information available");
            addPlainTextCard(nearbyCafesContainer, "No cafe information available");
            nearestMetroText.setText("No metro information available");
        }
    }

    private String getMonumentNameFromCoordinates(double lat, double lon) {
        // Simplified approach to get the monument name from the current display
        return monumentName;
    }

    private void addLinkCard(LinearLayout container, String title, String link) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.setRadius(getResources().getDimensionPixelSize(R.dimen.card_corner_radius));
        card.setCardElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
        card.setCardBackgroundColor(getResources().getColor(R.color.card_background));
        card.setUseCompatPadding(true);
        card.setClickable(true);
        
        // Set ripple effect
        card.setForeground(requireContext().getDrawable(R.drawable.ripple_effect));
        
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.card_content_padding);
        contentLayout.setPadding(padding, padding, padding, padding);
        
        TextView titleText = new TextView(requireContext());
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleText.setText(title);
        titleText.setTextSize(18);
        titleText.setTextColor(getResources().getColor(R.color.colorPrimary));
        contentLayout.addView(titleText);
        
        TextView linkText = new TextView(requireContext());
        linkText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linkText.setText("View on Google Maps");
        linkText.setTextSize(16);
        linkText.setTextColor(getResources().getColor(R.color.primary_text));
        linkText.setCompoundDrawablePadding(16);
        // Set icon if available
        try {
            linkText.setCompoundDrawablesWithIntrinsicBounds(
                requireContext().getDrawable(R.drawable.ic_map), null, null, null);
        } catch (Exception e) {
            // Icon not available, continue without it
        }
        contentLayout.addView(linkText);
        
        card.addView(contentLayout);
        container.addView(card);
        
        // Open link when clicked
        card.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Error opening link");
            }
        });
    }

    private void addPlainTextCard(LinearLayout container, String text) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.setRadius(getResources().getDimensionPixelSize(R.dimen.card_corner_radius));
        card.setCardElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
        card.setCardBackgroundColor(getResources().getColor(R.color.card_background));
        card.setUseCompatPadding(true);

        TextView textView = new TextView(requireContext());
        textView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        int padding = getResources().getDimensionPixelSize(R.dimen.card_content_padding);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(R.color.primary_text));
        textView.setPadding(padding, padding, padding, padding);

        card.addView(textView);
        container.addView(card);
    }

    private String getRestaurantLinkForMonument(String monumentName) {
        try {
            JSONObject foodJson = com.example.lnscp.utils.ApplicationContextProvider.getJSONFromAsset("food.json");
            if (foodJson != null && foodJson.has("restaurants")) {
                JSONObject restaurants = foodJson.getJSONObject("restaurants");
                if (restaurants.has(monumentName)) {
                    return restaurants.getString(monumentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getCafeLinkForMonument(String monumentName) {
        try {
            JSONObject foodJson = com.example.lnscp.utils.ApplicationContextProvider.getJSONFromAsset("food.json");
            if (foodJson != null && foodJson.has("cafes_fastfood")) {
                JSONObject cafes = foodJson.getJSONObject("cafes_fastfood");
                if (cafes.has(monumentName)) {
                    return cafes.getString(monumentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void displayMetroInformation(String monumentName) {
        try {
            // Get metro station name and color from metro.txt
            String metroInfo = getMetroInfoFromTxt(monumentName);
            String stationName = null;
            String lineColor = null;
            
            if (metroInfo != null) {
                // Parse the metro info (format: "Monument - Station (Color)")
                int dashIndex = metroInfo.indexOf(" - ");
                int openBracketIndex = metroInfo.indexOf("(");
                int closeBracketIndex = metroInfo.indexOf(")");
                
                if (dashIndex > 0 && openBracketIndex > dashIndex && closeBracketIndex > openBracketIndex) {
                    stationName = metroInfo.substring(dashIndex + 3, openBracketIndex).trim();
                    lineColor = metroInfo.substring(openBracketIndex + 1, closeBracketIndex).trim();
                }
            }
            
            // Get metro station link from metro.json
            String metroLink = getMetroLinkForMonument(monumentName);
            
            // Create metro card
            if (stationName != null && !stationName.isEmpty()) {
                // Convert color text to emoji
                String colorEmoji = getColorEmoji(lineColor);
                
                MaterialCardView metroCard = new MaterialCardView(requireContext());
                metroCard.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                metroCard.setRadius(getResources().getDimensionPixelSize(R.dimen.card_corner_radius));
                metroCard.setCardElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
                metroCard.setUseCompatPadding(true);
                metroCard.setClickable(metroLink != null && !metroLink.isEmpty());
                metroCard.setCardBackgroundColor(getResources().getColor(R.color.card_background));
                
                // Set ripple effect for clicking
                if (metroLink != null && !metroLink.isEmpty()) {
                    metroCard.setForeground(requireContext().getDrawable(R.drawable.ripple_effect));
                }
                
                // Create container for content
                LinearLayout container = new LinearLayout(requireContext());
                container.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                container.setOrientation(LinearLayout.VERTICAL);
                int padding = getResources().getDimensionPixelSize(R.dimen.card_content_padding);
                container.setPadding(padding, padding, padding, padding);
                
                // Station name with icon
                LinearLayout nameRow = new LinearLayout(requireContext());
                nameRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                nameRow.setOrientation(LinearLayout.HORIZONTAL);
                nameRow.setGravity(Gravity.CENTER_VERTICAL);
                
                // Metro icon
                ImageView metroIcon = new ImageView(requireContext());
                int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_size);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.setMarginEnd(padding/2);
                metroIcon.setLayoutParams(iconParams);
                metroIcon.setImageResource(R.drawable.ic_metro);
                nameRow.addView(metroIcon);
                
                // Station name
                TextView stationNameText = new TextView(requireContext());
                stationNameText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                stationNameText.setText(stationName);
                stationNameText.setTextSize(18);
                stationNameText.setTextColor(getResources().getColor(R.color.colorPrimary));
                nameRow.addView(stationNameText);
                
                // Add name row to container
                container.addView(nameRow);
                
                // Line Color
                if (colorEmoji != null && !colorEmoji.isEmpty()) {
                    TextView lineColorText = new TextView(requireContext());
                    lineColorText.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    lineColorText.setText(colorEmoji);
                    lineColorText.setTextSize(16);
                    
                    // Add spacing
                    LinearLayout.LayoutParams lineParams = (LinearLayout.LayoutParams) lineColorText.getLayoutParams();
                    lineParams.setMargins(0, padding/4, 0, padding/4);
                    lineColorText.setLayoutParams(lineParams);
                    
                    container.addView(lineColorText);
                }
                
                // Add map button if link available
                if (metroLink != null && !metroLink.isEmpty()) {
                    MaterialButton mapButton = new MaterialButton(requireContext(), null, 
                        com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
                    mapButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    mapButton.setText("View on Maps");
                    mapButton.setIcon(requireContext().getDrawable(R.drawable.ic_map));
                    mapButton.setIconGravity(MaterialButton.ICON_GRAVITY_START);
                    mapButton.setMaxLines(1);
                    
                    container.addView(mapButton);
                    
                    mapButton.setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(metroLink));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("Error opening map");
                        }
                    });
                    
                    // Make whole card clickable
                    metroCard.setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(metroLink));
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("Error opening map");
                        }
                    });
                }
                
                // Add the container to the card
                metroCard.addView(container);
                
                // Replace the text view with our card
                ViewGroup parent = (ViewGroup) nearestMetroText.getParent();
                int index = parent.indexOfChild(nearestMetroText);
                parent.removeView(nearestMetroText);
                parent.addView(metroCard, index);
            } else {
                nearestMetroText.setText("No metro information available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            nearestMetroText.setText("Error loading metro information");
        }
    }

    private String getMetroInfoFromTxt(String monumentName) {
        try {
            InputStream is = requireContext().getAssets().open("metro.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(monumentName)) {
                    reader.close();
                    return line;
                }
            }
            
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMetroLinkForMonument(String monumentName) {
        try {
            JSONObject metroJson = com.example.lnscp.utils.ApplicationContextProvider.getJSONFromAsset("metro.json");
            if (metroJson != null && metroJson.has("nearestMetro")) {
                JSONObject metros = metroJson.getJSONObject("nearestMetro");
                if (metros.has(monumentName)) {
                    return metros.getString(monumentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getColorEmoji(String colorText) {
        if (colorText == null) return "";
        
        String lowerColor = colorText.toLowerCase();
        
        if (lowerColor.contains("yellow")) return "🟨 Yellow";
        if (lowerColor.contains("blue")) return "🟦 Blue";
        if (lowerColor.contains("violet")) return "🟪 Violet";
        if (lowerColor.contains("red")) return "🟥 Red";
        if (lowerColor.contains("green")) return "🟩 Green";
        if (lowerColor.contains("orange")) return "🟧 Orange";
        if (lowerColor.contains("pink")) return "🟪 Pink";
        if (lowerColor.contains("magenta")) return "🟪 Magenta";
        
        return colorText;  // Return original if no match
    }

    private void setupBookmarkButton() {
        // Get the monument ID from the database
        List<Monument> monuments = dbHelper.getAllMonuments();
        int foundMonumentId = -1;
        
        for (Monument monument : monuments) {
            if (monument.getName().equals(monumentName)) {
                foundMonumentId = monument.getId();
                break;
            }
        }

        if (foundMonumentId == -1) {
            showToast("Error: Monument not found in database");
            return;
        }

        // Capture the ID in a final variable for use in the lambda
        final int monumentId = foundMonumentId;
        
        // Set initial bookmark state
        final boolean[] isBookmarked = {dbHelper.isMonumentBookmarked(monumentId)};
        bookmarkButton.setIconResource(
            isBookmarked[0] ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border
        );

        bookmarkButton.setOnClickListener(v -> {
            // First toggle the bookmark
            dbHelper.toggleBookmark(monumentId);
            
            // Then check if it's now bookmarked
            isBookmarked[0] = dbHelper.isMonumentBookmarked(monumentId);
            
            // Update the button icon
            bookmarkButton.setIconResource(
                isBookmarked[0] ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border
            );
            
            // If bookmark was added, check for achievement
            if (isBookmarked[0]) {
                // Get count of bookmarked monuments
                List<Monument> bookmarkedMonuments = dbHelper.getBookmarkedMonuments();
                int bookmarkedCount = bookmarkedMonuments.size();
                
                // Check for bookmark achievement - also directly unlock it to ensure visibility
                AchievementManager achievementManager = AchievementManager.getInstance(requireContext());
                achievementManager.checkBookmarkAchievement(bookmarkedCount);
                achievementManager.unlockAchievement(AchievementManager.MEMORIES_SAVED);
                
                showToast("Monument bookmarked!");
            } else {
                showToast("Bookmark removed");
            }
        });
    }

    private void updateMonumentCount() {
        // Check if this monument has been seen before
        if (dbHelper.isNewMonument(monumentName)) {
            // This is a new unique monument, add it to the database
            if (monumentImage != null) {
                dbHelper.addMonument(monumentName, monumentImage);
                
                // Update the unique monuments count in SharedPreferences
                SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                int newCount = dbHelper.getUniqueMonumentCount();
                
                // Always update the count
                prefs.edit().putInt("unique_monuments_count", newCount).apply();
                
                // Trigger achievement check
                AchievementManager achievementManager = AchievementManager.getInstance(requireContext());
                achievementManager.checkAchievements(newCount);
                
                // Show toast for discovered monument
                showToast("New monument discovered! Total: " + newCount);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
} 