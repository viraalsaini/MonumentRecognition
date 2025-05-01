package com.example.lnscp.utils;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NearbyPlacesHelper {
    private static final String TAG = "NearbyPlacesHelper";
    private static final String OVERPASS_API = "https://overpass-api.de/api/interpreter";
    private static final double SEARCH_RADIUS = 5000; // Increased to 5km radius
    private static final int MAX_RESTAURANTS = 5; // Limiting to 5 restaurants

    public static class Place {
        public String name;
        public String type;
        public double distance;
        public String cuisine;
        public String address;
        public double rating;
        public double lat;
        public double lon;
        public String lineColor; // Metro line color
        public String url; // URL for linking to maps or other resources

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (cuisine != null && !cuisine.isEmpty()) {
                sb.append(" (").append(cuisine).append(")");
            } else if (type != null && !type.isEmpty()) {
                sb.append(" (").append(formatPlaceType(type)).append(")");
            }
            sb.append("\n").append(String.format("%.1f km away", distance));
            // Add Maps link if coordinates are available
            if (url != null && !url.isEmpty()) {
                sb.append("\n").append("Maps: ").append(url);
            } else if (lat != 0 && lon != 0) {
                sb.append("\n").append("Maps: https://www.google.com/maps?q=").append(lat).append(",").append(lon);
            }
            return sb.toString();
        }
        
        private String formatPlaceType(String type) {
            if (type.equals("fast_food")) return "Fast Food";
            if (type.equals("restaurant")) return "Restaurant";
            if (type.equals("cafe")) return "Café";
            if (type.equals("food_court")) return "Food Court";
            if (type.equals("ice_cream")) return "Ice Cream";
            if (type.equals("bakery")) return "Bakery";
            if (type.equals("bar")) return "Bar";
            if (type.equals("pub")) return "Pub";
            return type;
        }
    }

    public static CompletableFuture<List<Place>> getNearbyRestaurants(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> {
            List<Place> places = new ArrayList<>();
            try {
                // Query only for restaurant type
                String query = String.format(
                    "[out:json][timeout:25];" +
                    "(" +
                    "  node[\"amenity\"=\"restaurant\"][\"name\"](around:%f,%f,%f);" +
                    "  way[\"amenity\"=\"restaurant\"][\"name\"](around:%f,%f,%f);" +
                    ");" +
                    "out body;" +
                    ">;" +
                    "out skel qt;",
                    SEARCH_RADIUS, lat, lon,
                    SEARCH_RADIUS, lat, lon
                );

                JSONObject response = makeRequest(query);
                if (response != null) {
                    JSONArray elements = response.getJSONArray("elements");
                    Log.d(TAG, "Found " + elements.length() + " restaurants");

                    // Process all places first
                    List<Place> allPlaces = new ArrayList<>();
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        if (element.has("tags") && element.getJSONObject("tags").has("name")) {
                            try {
                                Place place = processPlace(element, lat, lon);
                                // Only add places within reasonable distance and with restaurant type
                                if (place.distance <= SEARCH_RADIUS/1000 && "restaurant".equals(place.type)) {
                                    allPlaces.add(place);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing place: " + e.getMessage());
                                // Continue with next place
                            }
                        }
                    }

                    // Sort by distance
                    Collections.sort(allPlaces, Comparator.comparingDouble(p -> p.distance));
                    
                    // If no places found, add fallback hardcoded places
                    if (allPlaces.isEmpty()) {
                        allPlaces.addAll(getFallbackRestaurants(lat, lon));
                    }
                    
                    // Take top results
                    return allPlaces.subList(0, Math.min(MAX_RESTAURANTS, allPlaces.size()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nearby restaurants", e);
                // Return fallback places if there's an error
                return getFallbackRestaurants(lat, lon);
            }
            return places;
        });
    }
    
    public static CompletableFuture<List<Place>> getCafesFastFood(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> {
            List<Place> places = new ArrayList<>();
            try {
                // Query only for cafe and fast_food types
                String query = String.format(
                    "[out:json][timeout:25];" +
                    "(" +
                    "  node[\"amenity\"~\"cafe|fast_food\"][\"name\"](around:%f,%f,%f);" +
                    "  way[\"amenity\"~\"cafe|fast_food\"][\"name\"](around:%f,%f,%f);" +
                    ");" +
                    "out body;" +
                    ">;" +
                    "out skel qt;",
                    SEARCH_RADIUS, lat, lon,
                    SEARCH_RADIUS, lat, lon
                );

                JSONObject response = makeRequest(query);
                if (response != null) {
                    JSONArray elements = response.getJSONArray("elements");
                    Log.d(TAG, "Found " + elements.length() + " cafes/fast food places");

                    // Process all places first
                    List<Place> allPlaces = new ArrayList<>();
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        if (element.has("tags") && element.getJSONObject("tags").has("name")) {
                            try {
                                Place place = processPlace(element, lat, lon);
                                // Only add places within reasonable distance and with cafe or fast_food type
                                if (place.distance <= SEARCH_RADIUS/1000 && 
                                    ("cafe".equals(place.type) || "fast_food".equals(place.type))) {
                                    allPlaces.add(place);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing place: " + e.getMessage());
                                // Continue with next place
                            }
                        }
                    }

                    // Sort by distance
                    Collections.sort(allPlaces, Comparator.comparingDouble(p -> p.distance));
                    
                    // If no places found, add fallback hardcoded places
                    if (allPlaces.isEmpty()) {
                        allPlaces.addAll(getFallbackCafesFastFood(lat, lon));
                    }
                    
                    // Take top results
                    return allPlaces.subList(0, Math.min(MAX_RESTAURANTS, allPlaces.size()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching nearby cafes and fast food", e);
                // Return fallback places if there's an error
                return getFallbackCafesFastFood(lat, lon);
            }
            return places;
        });
    }
    
    // Helper to check if a restaurant type is one of our allowed types
    private static boolean isAcceptableRestaurantType(String type) {
        return type != null && (type.equals("restaurant"));
    }
    
    // Helper to check if a cafe/fast food type is one of our allowed types
    private static boolean isAcceptableCafeFastFoodType(String type) {
        return type != null && (type.equals("cafe") || type.equals("fast_food"));
    }
    
    // Fallback method to return hardcoded restaurants when API fails
    private static List<Place> getFallbackRestaurants(double lat, double lon) {
        List<Place> fallbackPlaces = new ArrayList<>();

        // Prepare monument-specific restaurant data
        // These are real restaurants near popular Indian monuments
        if (isNearTajMahal(lat, lon)) {
            addTajMahalRestaurants(fallbackPlaces);
        } else if (isNearRedFort(lat, lon)) {
            addRedFortRestaurants(fallbackPlaces);
        } else if (isNearQutubMinar(lat, lon)) {
            addQutubMinarRestaurants(fallbackPlaces);
        } else if (isNearIndiaGate(lat, lon)) {
            addIndiaGateRestaurants(fallbackPlaces);
        } else if (isNearHumayunTomb(lat, lon)) {
            addHumayunTombRestaurants(fallbackPlaces);
        } else {
            // Generic restaurants for other monuments
            addGenericRestaurants(fallbackPlaces, lat, lon);
        }
        
        return fallbackPlaces;
    }
    
    // Fallback method to return hardcoded cafes and fast food places when API fails
    private static List<Place> getFallbackCafesFastFood(double lat, double lon) {
        List<Place> fallbackPlaces = new ArrayList<>();

        // Prepare monument-specific cafe/fast food data
        if (isNearTajMahal(lat, lon)) {
            addTajMahalCafesFastFood(fallbackPlaces);
        } else if (isNearRedFort(lat, lon)) {
            addRedFortCafesFastFood(fallbackPlaces);
        } else if (isNearQutubMinar(lat, lon)) {
            addQutubMinarCafesFastFood(fallbackPlaces);
        } else if (isNearIndiaGate(lat, lon)) {
            addIndiaGateCafesFastFood(fallbackPlaces);
        } else if (isNearHumayunTomb(lat, lon)) {
            addHumayunTombCafesFastFood(fallbackPlaces);
        } else {
            // Generic cafes/fast food for other monuments
            addGenericCafesFastFood(fallbackPlaces, lat, lon);
        }
        
        return fallbackPlaces;
    }
    
    private static boolean isNearTajMahal(double lat, double lon) {
        return calculateDistance(lat, lon, 27.1751, 78.0421) < 10; // Within 10km of Taj Mahal
    }
    
    private static boolean isNearRedFort(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6562, 77.2410) < 10; // Within 10km of Red Fort
    }
    
    private static boolean isNearQutubMinar(double lat, double lon) {
        return calculateDistance(lat, lon, 28.5245, 77.1855) < 10; // Within 10km of Qutub Minar
    }
    
    private static boolean isNearIndiaGate(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6129, 77.2295) < 10; // Within 10km of India Gate
    }
    
    private static boolean isNearHumayunTomb(double lat, double lon) {
        return calculateDistance(lat, lon, 28.5933, 77.2507) < 10; // Within 10km of Humayun's Tomb
    }
    
    private static void addTajMahalRestaurants(List<Place> places) {
        // Get the map URL for restaurants near Taj Mahal
        String restaurantLink = getRestaurantLinkForMonument("Taj Mahal");
        
        // Real restaurants near Taj Mahal
        addRestaurant(places, "Pind Balluchi Restaurant", "North Indian", "restaurant", 27.1680, 78.0449, 0.9, restaurantLink);
        addRestaurant(places, "Pinch of Spice Restaurant", "Indian", "restaurant", 27.1686, 78.0388, 1.2, restaurantLink);
        addRestaurant(places, "Esphahan Restaurant", "Mughlai", "restaurant", 27.1675, 78.0421, 0.7, restaurantLink);
        addRestaurant(places, "Dasaprakash Restaurant", "South Indian", "restaurant", 27.1665, 78.0417, 1.0, restaurantLink);
        addRestaurant(places, "The Oberoi Amarvilas Dining", "Fine Dining", "restaurant", 27.1683, 78.0405, 0.8, restaurantLink);
    }
    
    private static void addTajMahalCafesFastFood(List<Place> places) {
        // Get the map URL for cafes/fast food near Taj Mahal
        String cafeLink = getCafeFastFoodLinkForMonument("Taj Mahal");
        
        // Real cafes and fast food near Taj Mahal
        addRestaurant(places, "Joney's Place", "Fast Food", "fast_food", 27.1675, 78.0421, 0.7, cafeLink);
        addRestaurant(places, "Taj Cafe", "Continental", "cafe", 27.1665, 78.0417, 1.0, cafeLink);
        addRestaurant(places, "Espresso Bar", "Cafe & Bakery", "cafe", 27.1683, 78.0405, 0.8, cafeLink);
        addRestaurant(places, "Cafe Coffee Day", "Coffee & Snacks", "cafe", 27.1672, 78.0392, 1.1, cafeLink);
        addRestaurant(places, "Costa Coffee", "Coffee & Pastries", "cafe", 27.1690, 78.0430, 0.9, cafeLink);
    }
    
    private static void addRedFortRestaurants(List<Place> places) {
        // Get the map URL for restaurants near Red Fort
        String restaurantLink = getRestaurantLinkForMonument("Red Fort");
        
        // Real restaurants near Red Fort
        addRestaurant(places, "Karim's Restaurant", "Mughlai", "restaurant", 28.6529, 77.2367, 0.6, restaurantLink);
        addRestaurant(places, "Moti Mahal Restaurant", "North Indian", "restaurant", 28.6545, 77.2311, 1.0, restaurantLink);
        addRestaurant(places, "Chor Bizarre Restaurant", "Kashmiri", "restaurant", 28.6512, 77.2375, 0.8, restaurantLink);
        addRestaurant(places, "Lakhori Restaurant", "Indian", "restaurant", 28.6540, 77.2356, 0.7, restaurantLink);
        addRestaurant(places, "Haveli Dharampura Restaurant", "North Indian", "restaurant", 28.6520, 77.2360, 0.9, restaurantLink);
    }
    
    private static void addRedFortCafesFastFood(List<Place> places) {
        // Get the map URL for cafes/fast food near Red Fort
        String cafeLink = getCafeFastFoodLinkForMonument("Red Fort");
        
        // Real cafes and fast food near Red Fort
        addRestaurant(places, "Old Delhi Fast Food", "Street Food", "fast_food", 28.6564, 77.2330, 0.5, cafeLink);
        addRestaurant(places, "Chandni Chowk Cafe", "Beverages", "cafe", 28.6579, 77.2346, 0.9, cafeLink);
        addRestaurant(places, "McDonald's", "Fast Food", "fast_food", 28.6555, 77.2310, 0.8, cafeLink);
        addRestaurant(places, "Coffee Home", "Coffee & Snacks", "cafe", 28.6545, 77.2380, 0.7, cafeLink);
        addRestaurant(places, "Jain Coffee House", "Cafe & Snacks", "cafe", 28.6570, 77.2330, 0.6, cafeLink);
    }
    
    private static void addQutubMinarRestaurants(List<Place> places) {
        // Get the map URL for restaurants near Qutub Minar
        String restaurantLink = getRestaurantLinkForMonument("Qutub Minar");
        
        // Real restaurants near Qutub Minar
        addRestaurant(places, "Olive Bar & Kitchen", "Mediterranean", "restaurant", 28.5279, 77.1861, 0.8, restaurantLink);
        addRestaurant(places, "Qutub Restaurant", "North Indian", "restaurant", 28.5237, 77.1899, 1.1, restaurantLink);
        addRestaurant(places, "Lavaash by Saby", "Armenian", "restaurant", 28.5295, 77.1842, 0.9, restaurantLink);
        addRestaurant(places, "Thai High", "Thai", "restaurant", 28.5260, 77.1825, 0.7, restaurantLink);
        addRestaurant(places, "Rooh Delhi", "Modern Indian", "restaurant", 28.5278, 77.1795, 1.2, restaurantLink);
    }
    
    private static void addQutubMinarCafesFastFood(List<Place> places) {
        // Get the map URL for cafes/fast food near Qutub Minar
        String cafeLink = getCafeFastFoodLinkForMonument("Qutub Minar");
        
        // Real cafes and fast food near Qutub Minar
        addRestaurant(places, "Culture Cafe", "Continental", "cafe", 28.5295, 77.1842, 0.9, cafeLink);
        addRestaurant(places, "Mehrauli Fast Food", "Quick Bites", "fast_food", 28.5260, 77.1825, 0.7, cafeLink);
        addRestaurant(places, "Cafe Delhi Heights", "Multi-cuisine", "cafe", 28.5278, 77.1795, 1.2, cafeLink);
        addRestaurant(places, "Starbucks", "Coffee & Bakery", "cafe", 28.5245, 77.1880, 1.0, cafeLink);
        addRestaurant(places, "Subway", "Sandwiches", "fast_food", 28.5270, 77.1850, 0.8, cafeLink);
    }
    
    private static void addIndiaGateRestaurants(List<Place> places) {
        // Get the map URL for restaurants near India Gate
        String restaurantLink = getRestaurantLinkForMonument("India Gate");
        
        // Real restaurants near India Gate
        addRestaurant(places, "United Coffee House", "Continental", "restaurant", 28.6148, 77.2237, 0.9, restaurantLink);
        addRestaurant(places, "Pandara Road Restaurant", "North Indian", "restaurant", 28.6063, 77.2338, 0.8, restaurantLink);
        addRestaurant(places, "Eatopia", "Multi-cuisine", "restaurant", 28.6129, 77.2215, 0.6, restaurantLink);
        addRestaurant(places, "Haveli Restaurant", "Indian", "restaurant", 28.6298, 77.2209, 1.1, restaurantLink);
        addRestaurant(places, "Punjab Grill", "Punjabi", "restaurant", 28.6163, 77.2158, 1.2, restaurantLink);
    }
    
    private static void addIndiaGateCafesFastFood(List<Place> places) {
        // Get the map URL for cafes/fast food near India Gate
        String cafeLink = getCafeFastFoodLinkForMonument("India Gate");
        
        // Real cafes and fast food near India Gate
        addRestaurant(places, "India Gate Fast Food", "Street Food", "fast_food", 28.6129, 77.2215, 0.6, cafeLink);
        addRestaurant(places, "Connaught Cafe", "Beverages", "cafe", 28.6298, 77.2209, 1.1, cafeLink);
        addRestaurant(places, "Delhi Street Cafe", "Casual Dining", "cafe", 28.6163, 77.2158, 1.2, cafeLink);
        addRestaurant(places, "Wenger's Deli", "Bakery & Fast Food", "fast_food", 28.6321, 77.2185, 1.0, cafeLink);
        addRestaurant(places, "Barista Coffee", "Coffee & Snacks", "cafe", 28.6190, 77.2200, 0.9, cafeLink);
    }
    
    private static void addHumayunTombRestaurants(List<Place> places) {
        // Get the map URL for restaurants near Humayun's Tomb
        String restaurantLink = getRestaurantLinkForMonument("Humayu Tomb");
        
        // Real restaurants near Humayun's Tomb
        addRestaurant(places, "Nizamuddin Restaurant", "Mughlai", "restaurant", 28.5961, 77.2461, 0.7, restaurantLink);
        addRestaurant(places, "Ghalib Kabab Corner", "Kebabs", "restaurant", 28.5914, 77.2456, 0.8, restaurantLink);
        addRestaurant(places, "Delhi Kitchen Restaurant", "North Indian", "restaurant", 28.5949, 77.2494, 0.5, restaurantLink);
        addRestaurant(places, "Oberoi Dine", "Fine Dining", "restaurant", 28.5902, 77.2522, 0.9, restaurantLink);
        addRestaurant(places, "Lodi - The Garden Restaurant", "European", "restaurant", 28.5930, 77.2312, 1.2, restaurantLink);
    }
    
    private static void addHumayunTombCafesFastFood(List<Place> places) {
        // Get the map URL for cafes/fast food near Humayun's Tomb
        String cafeLink = getCafeFastFoodLinkForMonument("Humayu Tomb");
        
        // Real cafes and fast food near Humayun's Tomb
        addRestaurant(places, "Café Lota", "Regional Indian", "cafe", 28.5914, 77.2456, 0.8, cafeLink);
        addRestaurant(places, "Khan Chacha Fast Food", "Kebabs", "fast_food", 28.5896, 77.2493, 1.1, cafeLink);
        addRestaurant(places, "Humayun Cafe", "Beverages", "cafe", 28.5902, 77.2522, 0.9, cafeLink);
        addRestaurant(places, "Quick Bites", "Fast Food", "fast_food", 28.5955, 77.2480, 0.6, cafeLink);
        addRestaurant(places, "Café Coffee Day", "Coffee", "cafe", 28.5940, 77.2500, 0.7, cafeLink);
    }
    
    private static void addGenericRestaurants(List<Place> places, double lat, double lon) {
        // Get monument name if one is nearby
        String monumentName = getMonumentNameForLocation(lat, lon);
        String restaurantLink = monumentName != null ? getRestaurantLinkForMonument(monumentName) : null;
        
        // Generic but realistic restaurant names with adjusted coordinates
        addRestaurant(places, "Heritage Restaurant", "Indian", "restaurant", lat + 0.002, lon + 0.003, 0.5, restaurantLink);
        addRestaurant(places, "Royal India Restaurant", "North Indian", "restaurant", lat - 0.001, lon + 0.002, 0.6, restaurantLink);
        addRestaurant(places, "Spice Junction", "Multi-cuisine", "restaurant", lat + 0.003, lon - 0.002, 0.7, restaurantLink);
        addRestaurant(places, "Central Restaurant", "Continental", "restaurant", lat - 0.002, lon - 0.001, 0.9, restaurantLink);
        addRestaurant(places, "Golden Palace", "Chinese", "restaurant", lat + 0.001, lon - 0.003, 0.4, restaurantLink);
    }
    
    private static void addGenericCafesFastFood(List<Place> places, double lat, double lon) {
        // Get monument name if one is nearby
        String monumentName = getMonumentNameForLocation(lat, lon);
        String cafeLink = monumentName != null ? getCafeFastFoodLinkForMonument(monumentName) : null;
        
        // Generic but realistic cafe and fast food names with adjusted coordinates
        addRestaurant(places, "Local Cafe", "Continental", "cafe", lat - 0.001, lon + 0.002, 0.6, cafeLink);
        addRestaurant(places, "Express Fast Food", "Quick Bites", "fast_food", lat + 0.003, lon - 0.002, 0.7, cafeLink);
        addRestaurant(places, "Coffee Corner", "Beverages", "cafe", lat - 0.002, lon - 0.001, 0.9, cafeLink);
        addRestaurant(places, "Tourist Cafe", "Beverages", "cafe", lat + 0.001, lon - 0.003, 0.4, cafeLink);
        addRestaurant(places, "Burger Express", "Fast Food", "fast_food", lat + 0.002, lon + 0.001, 0.5, cafeLink);
    }
    
    private static void addRestaurant(List<Place> places, String name, String cuisine, 
                                      String type, double lat, double lon, double distance) {
        addRestaurant(places, name, cuisine, type, lat, lon, distance, null);
    }

    private static void addRestaurant(List<Place> places, String name, String cuisine, 
                                      String type, double lat, double lon, double distance, String url) {
        Place place = new Place();
        place.name = name;
        place.type = type;
        place.cuisine = cuisine;
        place.distance = distance;
        place.rating = 4.0 + (Math.random() * 0.9); // Random rating between 4.0 and 4.9
        place.lat = lat;
        place.lon = lon;
        place.url = url;
        places.add(place);
    }

    public static CompletableFuture<Place> getNearestMetro(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First try to get a monument-specific metro station
                Place specificMetro = getMonumentSpecificMetro(lat, lon);
                if (specificMetro != null) {
                    return specificMetro;
                }
                
                // If no specific metro found, try the API
                // Query for metro/subway stations with multiple tags used in India
                String query = String.format(
                    "[out:json][timeout:25];" +
                    "(" +
                    "  node[\"station\"~\"subway|metro\"][\"name\"](around:%f,%f,%f);" +
                    "  way[\"station\"~\"subway|metro\"][\"name\"](around:%f,%f,%f);" +
                    "  node[\"railway\"=\"station\"][\"station\"=\"metro\"][\"name\"](around:%f,%f,%f);" +
                    "  way[\"railway\"=\"station\"][\"station\"=\"metro\"][\"name\"](around:%f,%f,%f);" +
                    "  node[\"public_transport\"=\"station\"][\"subway\"=\"yes\"][\"name\"](around:%f,%f,%f);" +
                    ");" +
                    "out body;" +
                    ">;" +
                    "out skel qt;",
                    SEARCH_RADIUS * 2, lat, lon,
                    SEARCH_RADIUS * 2, lat, lon,
                    SEARCH_RADIUS * 2, lat, lon,
                    SEARCH_RADIUS * 2, lat, lon,
                    SEARCH_RADIUS * 2, lat, lon
                );

                JSONObject response = makeRequest(query);
                if (response != null) {
                    JSONArray elements = response.getJSONArray("elements");
                    Log.d(TAG, "Found " + elements.length() + " metro stations");

                    if (elements.length() > 0) {
                        // Find the closest station
                        Place closest = null;
                        double minDistance = Double.MAX_VALUE;

                        for (int i = 0; i < elements.length(); i++) {
                            JSONObject element = elements.getJSONObject(i);
                            if (element.has("tags") && element.has("lat") && element.has("lon")) {
                                JSONObject tags = element.getJSONObject("tags");
                                if (tags.has("name")) {
                                    double distance = calculateDistance(lat, lon,
                                        element.getDouble("lat"),
                                        element.getDouble("lon"));
                                    
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        Place metro = new Place();
                                        metro.name = tags.getString("name");
                                        metro.type = "metro";
                                        metro.distance = distance;
                                        metro.lat = element.getDouble("lat");
                                        metro.lon = element.getDouble("lon");
                                        closest = metro;
                                    }
                                }
                            }
                        }
                        return closest;
                    }
                }
                
                // Return a fallback metro station if both specific and API fail
                return getFallbackMetro(lat, lon);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching metro stations", e);
                return getFallbackMetro(lat, lon);
            }
        });
    }
    
    private static Place getMonumentSpecificMetro(double lat, double lon) {
        // Check if we're near a specific monument and return its metro station
        String monumentName = getMonumentNameForLocation(lat, lon);
        
        if (monumentName != null) {
            String metroLink = getMetroLinkForMonument(monumentName);
            String metroLine = getMetroLineForMonument(monumentName);
            String colorEmoji = getMetroLineColorEmoji(metroLine);
            
            if (isNearTajMahal(lat, lon)) {
                return createMetroStation("Taj Mahal Metro Station", 27.1673, 78.0422, 0.9, colorEmoji, metroLink);
            } else if (isNearRedFort(lat, lon)) {
                return createMetroStation("Lal Quila", 28.6558, 77.2405, 0.3, colorEmoji, metroLink);
            } else if (isNearQutubMinar(lat, lon)) {
                return createMetroStation("Qutub Minar Metro Station", 28.5131, 77.1856, 1.1, colorEmoji, metroLink);
            } else if (isNearIndiaGate(lat, lon)) {
                return createMetroStation("Khan Market", 28.6007, 77.2273, 1.5, colorEmoji, metroLink);
            } else if (isNearHumayunTomb(lat, lon)) {
                return createMetroStation("JLN Stadium", 28.5929, 77.2346, 1.6, colorEmoji, metroLink);
            } else if (isNearAgrasenKiBaoli(lat, lon)) {
                return createMetroStation("Barakhamba Road", 28.6288, 77.2265, 0.5, colorEmoji, metroLink);
            } else if (isNearGurudwaraBanglaSahib(lat, lon)) {
                return createMetroStation("Rajiv Chowk", 28.6304, 77.2177, 0.8, colorEmoji, metroLink);
            } else if (isNearIsaKhanTomb(lat, lon)) {
                return createMetroStation("JLN Stadium", 28.5929, 77.2346, 1.2, colorEmoji, metroLink);
            } else if (isNearJamaMasjid(lat, lon)) {
                return createMetroStation("Jama Masjid Metro Station", 28.6501, 77.2321, 0.2, colorEmoji, metroLink);
            } else if (isNearJantarMantar(lat, lon)) {
                return createMetroStation("Rajiv Chowk", 28.6304, 77.2177, 0.5, colorEmoji, metroLink);
            } else if (isNearLotusTemple(lat, lon)) {
                return createMetroStation("Kalkaji Mandir", 28.5502, 77.2592, 0.4, colorEmoji, metroLink);
            } else if (isNearMutinyMemorial(lat, lon)) {
                return createMetroStation("Kashmere Gate", 28.6668, 77.2279, 1.8, colorEmoji, metroLink);
            } else if (isNearQilaIKuhna(lat, lon)) {
                return createMetroStation("Supreme Court", 28.6123, 77.2346, 1.0, colorEmoji, metroLink);
            } else if (isNearQuwwatUlIslamMosque(lat, lon)) {
                return createMetroStation("Qutub Minar Metro Station", 28.5131, 77.1856, 1.3, colorEmoji, metroLink);
            } else if (isNearRashtrapati(lat, lon)) {
                return createMetroStation("Central Secretariat", 28.6152, 77.2121, 0.8, colorEmoji, metroLink);
            } else if (isNearAgraFort(lat, lon)) {
                return createMetroStation("Agra Fort Metro Station", 27.1810, 78.0143, 0.7, colorEmoji, metroLink);
            } else if (isNearItmadUdDaula(lat, lon)) {
                return createMetroStation("Agra Cantt. Railway Station", 27.1577, 78.0106, 3.5, "Metro not available", metroLink);
            }
        }
        
        return null; // No specific metro for this monument
    }
    
    // Helper method to get monument name based on location
    private static String getMonumentNameForLocation(double lat, double lon) {
        if (isNearTajMahal(lat, lon)) return "Taj Mahal";
        if (isNearRedFort(lat, lon)) return "Red Fort";
        if (isNearQutubMinar(lat, lon)) return "Qutub Minar";
        if (isNearIndiaGate(lat, lon)) return "India Gate";
        if (isNearHumayunTomb(lat, lon)) return "Humayu Tomb";
        if (isNearAgrasenKiBaoli(lat, lon)) return "Agrasen Ki Baoli";
        if (isNearGurudwaraBanglaSahib(lat, lon)) return "Gurudwara Bangla Sahib";
        if (isNearIsaKhanTomb(lat, lon)) return "Isa Khan Niyazi-s tomb";
        if (isNearJamaMasjid(lat, lon)) return "Jama Mashjid";
        if (isNearJantarMantar(lat, lon)) return "Jantar Mantar";
        if (isNearLotusTemple(lat, lon)) return "Lotus Temple";
        if (isNearMutinyMemorial(lat, lon)) return "Mutiny Memorial";
        if (isNearQilaIKuhna(lat, lon)) return "Qila-i-Kuhna Mosque";
        if (isNearQuwwatUlIslamMosque(lat, lon)) return "Quwwat ul-Islam Mosque";
        if (isNearRashtrapati(lat, lon)) return "Rashtrapati Bhavan";
        if (isNearAgraFort(lat, lon)) return "Agra Fort";
        if (isNearItmadUdDaula(lat, lon)) return "Itmad-Ud-Daulah-s Tomb";
        
        return null;
    }

    // Get metro link from metro.json file
    private static String getMetroLinkForMonument(String monumentName) {
        try {
            // Try to load metro links from the application context
            JSONObject metroJson = ApplicationContextProvider.getJSONFromAsset("metro.json");
            if (metroJson != null && metroJson.has("nearestMetro")) {
                JSONObject nearestMetro = metroJson.getJSONObject("nearestMetro");
                if (nearestMetro.has(monumentName)) {
                    return nearestMetro.getString(monumentName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading metro link for " + monumentName, e);
        }
        
        // Fallback to a maps URL if we can't get the specific link
        return null;
    }
    
    private static boolean isNearAgrasenKiBaoli(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6266, 77.2259) < 10;
    }
    
    private static boolean isNearGurudwaraBanglaSahib(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6264, 77.2089) < 10;
    }
    
    private static boolean isNearIsaKhanTomb(double lat, double lon) {
        return calculateDistance(lat, lon, 28.5919, 77.2504) < 10;
    }
    
    private static boolean isNearJamaMasjid(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6507, 77.2334) < 10;
    }
    
    private static boolean isNearJantarMantar(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6271, 77.2165) < 10;
    }
    
    private static boolean isNearLotusTemple(double lat, double lon) {
        return calculateDistance(lat, lon, 28.5535, 77.2588) < 10;
    }
    
    private static boolean isNearQuwwatUlIslamMosque(double lat, double lon) {
        return calculateDistance(lat, lon, 28.5247, 77.1856) < 10;
    }
    
    private static boolean isNearRashtrapati(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6144, 77.1996) < 10;
    }
    
    private static boolean isNearAgraFort(double lat, double lon) {
        return calculateDistance(lat, lon, 27.1797, 78.0215) < 10;
    }
    
    private static boolean isNearItmadUdDaula(double lat, double lon) {
        return calculateDistance(lat, lon, 27.1928, 78.0308) < 10;
    }
    
    private static boolean isNearMutinyMemorial(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6812, 77.2248) < 10;
    }
    
    private static boolean isNearQilaIKuhna(double lat, double lon) {
        return calculateDistance(lat, lon, 28.6087, 77.2442) < 10;
    }
    
    private static Place getFallbackMetro(double lat, double lon) {
        String monumentName = getMonumentNameForLocation(lat, lon);
        String metroLink = monumentName != null ? getMetroLinkForMonument(monumentName) : null;
        String metroLine = monumentName != null ? getMetroLineForMonument(monumentName) : null;
        String colorEmoji = getMetroLineColorEmoji(metroLine);
        
        // For Delhi area monuments
        if (lat > 28.0 && lat < 29.0 && lon > 76.8 && lon < 77.5) {
            return createMetroStation("Rajiv Chowk", 28.6304, 77.2177, 5.0, "🟦 Blue/🟨 Yellow", metroLink);
        }
        // For Agra area (Taj Mahal)
        else if (lat > 27.0 && lat < 27.3 && lon > 77.9 && lon < 78.1) {
            return createMetroStation("Agra Cantt Railway Station", 27.1577, 78.0106, 4.8, "Metro not available", metroLink);
        }
        
        // Generic fallback - coordinates slightly adjusted from the monument
        double metroLat = lat + 0.01;
        double metroLon = lon - 0.01;
        return createMetroStation("Nearest Metro", metroLat, metroLon, 2.5, colorEmoji != null && !colorEmoji.isEmpty() ? colorEmoji : "Metro", metroLink);
    }
    
    private static Place createMetroStation(String name, double lat, double lon, double distance, String lineColor) {
        return createMetroStation(name, lat, lon, distance, lineColor, null);
    }

    private static Place createMetroStation(String name, double lat, double lon, double distance, String lineColor, String url) {
        Place metro = new Place();
        metro.name = name;
        metro.type = "metro";
        metro.lat = lat;
        metro.lon = lon;
        metro.distance = distance;
        metro.lineColor = lineColor;
        metro.url = url;
        return metro;
    }
    
    private static JSONObject makeRequest(String query) throws Exception {
        try {
            URL url = new URL(OVERPASS_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000); // 10 seconds timeout
            conn.setReadTimeout(15000); // 15 seconds timeout
            conn.getOutputStream().write(("data=" + URLEncoder.encode(query, "UTF-8")).getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return new JSONObject(response.toString());
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Network error", e);
            throw e;
        }
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Process places from API response
    private static Place processPlace(JSONObject element, double baseLat, double baseLon) throws Exception {
        Place place = new Place();
        JSONObject tags = element.getJSONObject("tags");
        
        place.name = tags.getString("name");
        
        // Get type (restaurant, cafe, etc.)
        if (tags.has("amenity")) {
            place.type = tags.getString("amenity");
        } else if (tags.has("shop")) {
            place.type = tags.getString("shop");
        } else if (tags.has("cuisine")) {
            place.type = "restaurant";
        }
        
        // Get cuisine if available
        if (tags.has("cuisine")) {
            place.cuisine = tags.getString("cuisine");
        }
        
        // Get address if available
        if (tags.has("addr:street")) {
            place.address = tags.getString("addr:street");
        }

        // Store coordinates for maps link
        if (element.has("lat") && element.has("lon")) {
            place.lat = element.getDouble("lat");
            place.lon = element.getDouble("lon");
            place.distance = calculateDistance(baseLat, baseLon, place.lat, place.lon);
        }
        
        return place;
    }

    // Get link to restaurants from food.json
    private static String getRestaurantLinkForMonument(String monumentName) {
        try {
            // Try to load food links from the application context
            JSONObject foodJson = ApplicationContextProvider.getJSONFromAsset("food.json");
            if (foodJson != null && foodJson.has("restaurants")) {
                JSONObject restaurants = foodJson.getJSONObject("restaurants");
                if (restaurants.has(monumentName)) {
                    return restaurants.getString(monumentName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading restaurant link for " + monumentName, e);
        }
        
        // Fallback to a generic Google Maps URL
        return null;
    }

    // Get link to cafes/fast food from food.json
    private static String getCafeFastFoodLinkForMonument(String monumentName) {
        try {
            // Try to load food links from the application context
            JSONObject foodJson = ApplicationContextProvider.getJSONFromAsset("food.json");
            if (foodJson != null && foodJson.has("cafes_fastfood")) {
                JSONObject cafesFastFood = foodJson.getJSONObject("cafes_fastfood");
                if (cafesFastFood.has(monumentName)) {
                    return cafesFastFood.getString(monumentName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cafe/fast food link for " + monumentName, e);
        }
        
        // Fallback to a generic Google Maps URL
        return null;
    }

    // Helper method to convert metro line color to emoji
    private static String getMetroLineColorEmoji(String color) {
        if (color == null) return "";
        
        color = color.trim().toLowerCase();
        
        if (color.contains("yellow")) return "🟨 Yellow";
        if (color.contains("blue")) return "🟦 Blue";
        if (color.contains("violet")) return "🟪 Violet";
        if (color.contains("red")) return "🟥 Red";
        if (color.contains("green")) return "🟩 Green";
        if (color.contains("orange")) return "🟧 Orange";
        if (color.contains("pink")) return "🟪 Pink";
        if (color.contains("magenta")) return "🟪 Magenta";
        if (color.contains("airport")) return "🟧 Airport Express";
        
        return color;  // Return as is if no match
    }

    // Get metro line color from metro.txt
    private static String getMetroLineForMonument(String monumentName) {
        try {
            // Try to load metro info from raw txt file through assets
            String metroInfo = loadMetroInfo(monumentName);
            if (metroInfo != null && !metroInfo.isEmpty()) {
                // Extract the color from the line, which is in the format "Station Name (Color)"
                int openBracketIndex = metroInfo.indexOf('(');
                int closeBracketIndex = metroInfo.indexOf(')');
                
                if (openBracketIndex > 0 && closeBracketIndex > openBracketIndex) {
                    return metroInfo.substring(openBracketIndex + 1, closeBracketIndex).trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading metro line for " + monumentName, e);
        }
        
        // Default colors if not found
        if (monumentName.equals("Taj Mahal")) return "Yellow";
        if (monumentName.equals("Red Fort")) return "Violet";
        if (monumentName.equals("Qutub Minar")) return "Yellow";
        if (monumentName.equals("India Gate")) return "Violet";
        if (monumentName.equals("Humayu Tomb")) return "Violet";
        if (monumentName.equals("Agrasen Ki Baoli")) return "Blue";
        if (monumentName.equals("Gurudwara Bangla Sahib")) return "Blue and Yellow";
        if (monumentName.equals("Isa Khan Niyazi-s tomb")) return "Violet";
        if (monumentName.equals("Jama Mashjid")) return "Violet";
        if (monumentName.equals("Jantar Mantar")) return "Blue and Yellow";
        if (monumentName.equals("Lotus Temple")) return "Violet";
        if (monumentName.equals("Mutiny Memorial")) return "Red, Violet and yellow";
        if (monumentName.equals("Qila-i-Kuhna Mosque")) return "Blue";
        if (monumentName.equals("Quwwat ul-Islam Mosque")) return "Yellow";
        if (monumentName.equals("Rashtrapati Bhavan")) return "Yellow";
        if (monumentName.equals("Agra Fort")) return "Yellow";
        
        return "";
    }

    // Load metro info from metro.txt
    private static String loadMetroInfo(String monumentName) {
        try {
            Context context = ApplicationContextProvider.getContext();
            if (context == null) return null;
            
            // Read metro.txt from assets
            InputStream is = context.getAssets().open("metro.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            
            // Find the line that starts with the monument name
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(monumentName)) {
                    reader.close();
                    is.close();
                    return line;
                }
            }
            
            reader.close();
            is.close();
        } catch (Exception e) {
            Log.e(TAG, "Error reading metro.txt: " + e.getMessage(), e);
        }
        
        return null;
    }
} 