# Monument Recognition App

A modern Android application that helps users identify and learn about historical monuments through image recognition technology.

## Features

### 1. User Authentication
- Simple sign-up process with name, email, and phone number
- Local authentication using SharedPreferences
- Persistent login state

### 2. Monument Recognition
- Image upload from gallery
- Camera capture functionality
- Real-time monument identification using TensorFlow Lite
- Detailed monument information display
- Wikipedia integration for additional information
- Map integration for monument location

### 3. User Profile
- Display user information
- Achievement system
- Bookmarks for saved monuments
- Dark mode support
- Statistics tracking

### 4. Navigation
- Bottom navigation with three main sections:
  - Home: Main scanning interface
  - Recents: Recently identified monuments
  - Profile: User information and settings

## Project Structure

### Activities
- `SplashActivity`: Initial screen with app logo and login check
- `SignUpActivity`: User registration screen
- `MainActivity`: Main container with bottom navigation

### Fragments
- `HomeFragment`: Monument scanning and identification
- `RecentsFragment`: List of recently identified monuments
- `ProfileFragment`: User profile and settings
- `MonumentDetailsFragment`: Detailed monument information

### Adapters
- `AchievementAdapter`: Handles achievement list display
- `RecentsAdapter`: Manages recent monuments list

### Models
- `Achievement`: Achievement data model
- `Monument`: Monument information model

### Layouts
- `activity_splash.xml`: Splash screen layout
- `activity_sign_up.xml`: Registration form
- `activity_main.xml`: Main container layout
- `fragment_home.xml`: Scanning interface
- `fragment_recents.xml`: Recent monuments list
- `fragment_profile.xml`: Profile and settings
- `fragment_monument_details.xml`: Monument details
- `item_achievement.xml`: Achievement list item
- `item_recent.xml`: Recent monument list item

### Resources
- `drawable/`: Icons and images
  - `ic_bookmark.xml`: Bookmark icon
  - `ic_check_circle.xml`: Achievement completion icon
  - `ic_lock.xml`: Achievement locked icon
  - `achievement_*.png`: Achievement-specific icons
- `values/`: App resources
  - `colors.xml`: Color definitions
  - `strings.xml`: String resources
  - `themes.xml`: App themes

## Implementation Details

### Authentication
- Uses SharedPreferences for local user data storage
- Validates user input (name, email, phone)
- Maintains login state across app restarts

### Monument Recognition
- Implements TensorFlow Lite for image classification
- Processes images to match model input requirements
- Provides confidence scores for predictions
- Stores recognition history in local database

### Achievements System
- Tracks user progress through various milestones
- Includes achievements for:
  - First monument discovery
  - Multiple discoveries
  - Specific monument types
- Visual indicators for locked/unlocked status
- Progress tracking for each achievement

### UI/UX
- Material Design implementation
- Responsive layouts
- Dark mode support
- Accessibility features
- Smooth navigation transitions

### Data Management
- Local SQLite database for monument history
- SharedPreferences for user preferences
- Efficient image processing and storage
- Caching for better performance

## Setup and Installation

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the application

## Requirements
- Android Studio Arctic Fox or later
- Android SDK 21 or higher
- Gradle 7.0 or higher

## Dependencies
- AndroidX Core
- Material Design Components
- TensorFlow Lite
- Navigation Component
- Room Database
- Glide for image loading

## Future Enhancements
- Cloud synchronization
- Social sharing features
- Advanced monument filtering
- Offline mode improvements
- Additional achievement types
- User feedback system 