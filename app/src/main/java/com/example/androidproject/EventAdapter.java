package com.example.androidproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events;

    // Simple constructor without click listener for now
    public EventAdapter(List<Event> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.eventTitle.setText(event.getEventName());
        holder.eventVenue.setText("Venue: " + event.getVenue());

        // Format date and time from startDateTime and endDateTime
        if (event.getStartDateTime() != null && event.getEndDateTime() != null) {
            String startDateTime = event.getStartDateTime();
            String endDateTime = event.getEndDateTime();

            // Display date from startDateTime
            holder.eventDate.setText("Date: " + startDateTime.split(",")[0]);

            // Display time range
            String startTime = startDateTime.split(",")[1].trim();
            String endTime = endDateTime.split(",")[1].trim();
            holder.eventTime.setText("Time: " + startTime + " - " + endTime);
        } else {
            holder.eventDate.setText("Date: Not set");
            holder.eventTime.setText("Time: Not set");
        }

        // Display attendance (default to 0 if not set)
        int currentAttendees = event.getCurrentAttendees();
        holder.eventAttendance.setText(currentAttendees + " / " + event.getPax());

        // For now, buttons will do nothing
        holder.btnViewList.setOnClickListener(v -> {
            // Will implement later
        });

        holder.btnEdit.setOnClickListener(v -> {
            // Will implement later
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle, eventVenue, eventDate, eventTime, eventAttendance;
        TextView btnViewList, btnEdit;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventVenue = itemView.findViewById(R.id.eventVenue);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventTime = itemView.findViewById(R.id.eventTime);
            eventAttendance = itemView.findViewById(R.id.eventAttendance);
            btnViewList = itemView.findViewById(R.id.btnViewList);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}