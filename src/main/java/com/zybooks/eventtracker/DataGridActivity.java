package com.zybooks.eventtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DataGridActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 123;
    private TableLayout eventTable;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        try {
            dbHelper = new DatabaseHelper(this);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        eventTable = findViewById(R.id.eventTable);

        // Initialize Buttons
        Button addEventButton = findViewById(R.id.addEventButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Set onClick listener for the add event button
        addEventButton.setOnClickListener(v -> showAddEventDialog());

        // Set onClick listener for the logout button
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(DataGridActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Load events from the database
        loadEvents();

        // Request SMS permission
        requestSmsPermission();
    }
    // Displays a dialog window for adding a new event
    private void showAddEventDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        // Create the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);

        // Initialize the dialog elements
        EditText titleEditText = dialogView.findViewById(R.id.eventTitleEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.eventDescriptionEditText);
        EditText dateEditText = dialogView.findViewById(R.id.eventDateEditText);
        EditText timeEditText = dialogView.findViewById(R.id.eventTimeEditText);
        CheckBox notificationCheckBox = dialogView.findViewById(R.id.notificationCheckBox);
        Button addEventButton = dialogView.findViewById(R.id.addEventButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Create and show the dialog
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        // Set onClick listener for the add event button in the dialog
        addEventButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String date = dateEditText.getText().toString().trim();
            String time = timeEditText.getText().toString().trim();
            boolean enableNotifications = notificationCheckBox.isChecked();

            // Validate the input
            if (title.isEmpty() || date.isEmpty()) {
                Toast.makeText(DataGridActivity.this, "Please fill in title and date.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate date format
            if (isValidDate(date)) {
                Toast.makeText(DataGridActivity.this, "Invalid date format. Use MM-DD-YYYY.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add the event to the database
            long eventId = dbHelper.addEvent(title, description, date, time, enableNotifications);
            if (eventId != -1) {
                Toast.makeText(DataGridActivity.this, "Event Added", Toast.LENGTH_SHORT).show();
                loadEvents();
                if (enableNotifications) {
                    requestSmsPermission();
                }
            } else {
                Toast.makeText(DataGridActivity.this, "Error Adding Event", Toast.LENGTH_SHORT).show();
            }

            // Dismiss the dialog
            alertDialog.dismiss();
        });

        // Set onClick listener for the cancel button in the dialog
        cancelButton.setOnClickListener(v -> alertDialog.dismiss());
    }

    // Method to validate the date format
    private boolean isValidDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);
            return false;
        } catch (ParseException e) {
            return true;
        }
    }

    // Method to load events from the database
    private void loadEvents() {
        // Clear the table before loading new data
        eventTable.removeAllViews();

        // Add the header row first
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        String[] headers = {"Title", "Description", "Date", "Time"};
        float[] columnWeights = {1.4f, 2.0f, 1.2f, 1.5f};

        for (int i = 0; i < headers.length; i++) {
            TextView headerTextView = new TextView(this);
            headerTextView.setText(headers[i]);
            headerTextView.setPadding(5, 10, 5, 10);
            TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, columnWeights[i]);
            headerTextView.setLayoutParams(params);
            headerTextView.setTypeface(Typeface.DEFAULT_BOLD);
            headerRow.addView(headerTextView);
        }
        // Adjust the Time header to better align with the time entries
        TextView timeHeader = (TextView) headerRow.getChildAt(3);
        timeHeader.setPadding(40, 10, 5, 10);
        eventTable.addView(headerRow);

        try (Cursor cursor = dbHelper.getAllEvents()) {
            Log.d("DataGridActivity", "Loaded " + cursor.getCount() + " events.");
            // Iterate through each event in the cursor
            while (cursor.moveToNext()) {
                // Retrieve event details from the cursor
                int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(dbHelper.getColumnEventId()));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(dbHelper.getColumnTitle()));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(dbHelper.getColumnDescription()));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(dbHelper.getColumnDate()));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(dbHelper.getColumnTime()));
                boolean notificationsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(dbHelper.getColumnNotificationsEnabled())) == 1;

                // Add the event row to the table layout
                addEventRow(eventId, title, description, date, time, notificationsEnabled);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("DataGridActivity", "Error loading events", e);
        }
    }

    // Method to dynamically add a row to the table
    private void addEventRow(int eventId, String title, String description, String date, String time, Boolean notificationsEnabled) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        // Make entries clickable
        row.setClickable(true);
        // Set the background selector
        row.setBackgroundResource(R.drawable.row_selector);

        float[] columnWeights = {1.3f, 1.4f, 1.6f, 1.2f};

        TextView titleTextView = createColumnTextView(title, columnWeights[0]);
        row.addView(titleTextView);

        TextView descriptionTextView = createColumnTextView(description, columnWeights[1]);
        row.addView(descriptionTextView);

        TextView dateTextView = createColumnTextView(date, columnWeights[2]);
        row.addView(dateTextView);

        TextView timeTextView = createColumnTextView(time, columnWeights[3]);
        row.addView(timeTextView);
        eventTable.addView(row);

        // Make row clickable to show details
        row.setOnClickListener(v -> showEventDetailsDialog(eventId, title, description, date, time, notificationsEnabled));
    }

    // Creates a TextView for a column in the event table
    private TextView createColumnTextView(String text, float weight) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(5, 10, 5, 10);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight);
        textView.setLayoutParams(params);
        textView.setSingleLine(true);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return textView;
    }

    // Request SMS permission
    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // Handle permission request response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Displays a dialog window for editing an existing event
    @SuppressLint("SetTextI18n")
    private void showEditEventDialog(int eventId, String title, String description, String date, String time, boolean notificationsEnabled) {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        // Create the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);

        // Initialize the dialog elements
        EditText titleEditText = dialogView.findViewById(R.id.eventTitleEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.eventDescriptionEditText);
        EditText dateEditText = dialogView.findViewById(R.id.eventDateEditText);
        EditText timeEditText = dialogView.findViewById(R.id.eventTimeEditText);
        CheckBox notificationCheckBox = dialogView.findViewById(R.id.notificationCheckBox);
        Button addEventButton = dialogView.findViewById(R.id.addEventButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set existing values
        titleEditText.setText(title);
        descriptionEditText.setText(description);
        dateEditText.setText(date);
        timeEditText.setText(time);
        notificationCheckBox.setChecked(notificationsEnabled);
        addEventButton.setText("Update Event");

        // Create and show the dialog
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        // Set onClick listener for the add/update event button in the dialog
        addEventButton.setOnClickListener(v -> {
            String newTitle = titleEditText.getText().toString().trim();
            String newDescription = descriptionEditText.getText().toString().trim();
            String newDate = dateEditText.getText().toString().trim();
            String newTime = timeEditText.getText().toString().trim();
            boolean enableNotifications = notificationCheckBox.isChecked();

            // Validate the input
            if (newTitle.isEmpty() || newDescription.isEmpty() || newDate.isEmpty() || newTime.isEmpty()) {
                Toast.makeText(DataGridActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate date format
            if (isValidDate(newDate)) {
                Toast.makeText(DataGridActivity.this, "Invalid date format. Use MM-DD-YYYY.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the event in the database
            dbHelper.updateEvent(eventId, newTitle, newDescription, newDate, newTime, enableNotifications);
            Toast.makeText(DataGridActivity.this, "Event Updated", Toast.LENGTH_SHORT).show();
            loadEvents();
            if (enableNotifications) {
                requestSmsPermission();
            }

            // Dismiss the dialog
            alertDialog.dismiss();
        });

        // Set onClick listener for the cancel button in the dialog
        cancelButton.setOnClickListener(v -> alertDialog.dismiss());
    }

    // Displays a dialog window showing the details of an event
    private void showEventDetailsDialog(int eventId, String title, String description, String date, String time, boolean notificationsEnabled) {
        // Create the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Event Details");

        // Set the dialog message
        String message = "Title: " + title +
                "\n\nDescription: " + description +
                "\n\nDate: " + date +
                "\n\nTime: " + time +
                "\n\nNotifications: " + (notificationsEnabled ? "Enabled" : "Disabled");
        dialogBuilder.setMessage(message);

        // Add Delete and Edit buttons
        dialogBuilder.setNegativeButton("Edit", (dialog, which) -> {
            showEditEventDialog(eventId, title, description, date, time, notificationsEnabled);
        });

        dialogBuilder.setPositiveButton("Delete", (dialog, which) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event?")
                    .setPositiveButton("Yes", (confirmDialog, confirmWhich) -> {
                        try {
                            dbHelper.deleteEvent(eventId);
                            loadEvents();
                            Toast.makeText(DataGridActivity.this, "Event Deleted", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(DataGridActivity.this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("DataGridActivity", "Error deleting event", e);
                        }
                        confirmDialog.dismiss();
                    })
                    .setNegativeButton("No", (confirmDialog, confirmWhich) -> confirmDialog.dismiss())
                    .show();
        });

        dialogBuilder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());

        // Create and show the dialog
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        // Make the Delete button red
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.delete_red));
    }
}
