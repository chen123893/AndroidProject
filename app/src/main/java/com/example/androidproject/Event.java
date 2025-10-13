package com.example.androidproject;

public class Event {
    private String eventId;
    private String eventName;
    private String venue;
    private String startDateTime;
    private String endDateTime;
    private int pax;
    private int currentAttendees;
    private String description;
    private int genderSpec;
    private String adminID;

    // Required empty constructor for Firestore
    public Event() {}

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

    public int getPax() { return pax; }
    public void setPax(int pax) { this.pax = pax; }

    public int getCurrentAttendees() { return currentAttendees; }
    public void setCurrentAttendees(int currentAttendees) { this.currentAttendees = currentAttendees; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getGenderSpec() { return genderSpec; }
    public void setGenderSpec(int genderSpec) { this.genderSpec = genderSpec; }

    public String getAdminID() { return adminID; }
    public void setAdminID(String adminID) { this.adminID = adminID; }
}