GOGO- EVENT MANAGEMENT APP
An Android event management application connecting event organizers with participants.

ABOUT
Gogo is an event management platform for Android that connects event organizers (admins) with participants (users). Organizers can create and manage events while users can discover, join, and track events through a personalized timetable.

FEATURES
1. For Organizers
- Create and edit events with venue, date/time, capacity, and descriptions
- View and manage attendee lists
- Send email notifications when events are updated
- Track real-time attendee counts

2. For Participants
- Browse and search events
- Get AI-powered event recommendations based on interests
- Join and leave events
- View personal timetable with date filtering
- Filter events by gender preferences

GENERAL
- Firebase authentication with email verification
- Customizable user profiles
- Real-time data synchronization
- Built-in image gallery for events and profiles

DEVELOPMENT TOOLS
- Java
- Android SDK 
- Firebase Authentication
- Cloud Firestore
- OpenAI API
- EmailJs

SETUP
- Clone the repository
- Create a Firebase project and download google-services.json to app/
- Enable Email/Password authentication in Firebase
- Create Firestore collections: admin, user, events, attendance
- Add OpenAI API key to your build configuration
- Build and run in Android Studio

Database Structure

admin: adminID, name, email, phoneNumber, gender, profilePic
user: userID, name, email, phoneNumber, gender, description, profilePic
events: eventID, adminID, eventName, venue, startDateTime, endDateTime, pax, currentAttendees, description, genderSpec, imageName
attendance: eventID, userID
