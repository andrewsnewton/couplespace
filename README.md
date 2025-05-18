# Bonded - Couples Wellness and Communication App

Bonded is an Android application designed to strengthen relationships through shared wellness and communication. The app allows couples to connect, track health metrics, maintain a shared timeline, and communicate with fun interactive features.

## Features

### Onboarding Flow
- Welcome screen with app introduction
- User profile setup (name, age, height, weight)
- Couple connection via unique codes

### Timeline
- View and manage personal and partner's calendars
- Add events and memories
- Nudge partner for important dates

### Health Tracking
- Log meals with calorie tracking
- View daily, weekly, and monthly health statistics
- Share health data with partner

### Chat
- Real-time messaging between partners
- Fun question generator with different topics (Naughty, Funny, Emotional)
- Shared question and answer threads

### Notifications
- Receive nudges for important events
- Get notified when partner answers questions
- Real-time message notifications

## Technical Overview

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Jetpack Compose for UI
- Firebase for backend services

### Backend (Firebase)
- Authentication: Anonymous sign-in with couple linking
- Firestore: Real-time database for messages, timeline entries, and health logs
- Cloud Messaging: Push notifications for real-time updates

### UI/UX
- Material 3 Expressive design language
- Romantic color palette
- Smooth transitions and animations

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Create a Firebase project and add the google-services.json file to the app directory
4. Enable Firebase Authentication, Firestore, and Cloud Messaging in your Firebase project
5. Build and run the application

## Firebase Collections

The app uses the following Firestore collections:

- **users**: User profiles and couple linking information
- **timelineEntries**: Events and memories for the timeline feature
- **healthLogs**: Meal and nutrition tracking data
- **messages**: Chat messages between partners
- **coupleQuestions**: Fun questions and partner responses

## Requirements

- Android Studio Arctic Fox or newer
- Android SDK 35 or higher
- Firebase project with required services enabled

## License

This project is licensed under the MIT License - see the LICENSE file for details.
