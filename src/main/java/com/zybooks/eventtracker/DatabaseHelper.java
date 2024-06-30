package com.zybooks.eventtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DATABASE_NAME = "event_tracker.db";
    private static final int DATABASE_VERSION = 4;

    // Table for user credentials
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Table for event details
    private static final String TABLE_EVENTS = "events";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_NOTIFICATIONS_ENABLED = "notifications_enabled";

    // SQL statement to create the users table
    private static final String TABLE_CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT);";

    // SQL statement to create the events table
    private static final String TABLE_CREATE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
                    COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_NOTIFICATIONS_ENABLED + " INTEGER DEFAULT 0);";

    public DatabaseHelper(Context context) {
        // DatabaseHelper Constructor
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("DatabaseHelper", "DatabaseHelper constructor called");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the users and events tables
        Log.d("DatabaseHelper", "Creating users table: " + TABLE_CREATE_USERS);
        db.execSQL(TABLE_CREATE_USERS);
        Log.d("DatabaseHelper", "Creating events table: " + TABLE_CREATE_EVENTS);
        db.execSQL(TABLE_CREATE_EVENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the old tables if they exist and create new ones
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }
    // Method to add or update an event
    public long addEvent(String title, String description, String date, String time, boolean enableNotifications) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_DATE, convertDateToSortableFormat(date)); // Convert date to YYYY-MM-DD format
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_NOTIFICATIONS_ENABLED, enableNotifications ? 1 : 0);

        return db.insert(TABLE_EVENTS, null, values);
    }
    // Method to update an event
    public void updateEvent(int eventId, String title, String description, String date, String time, boolean enableNotifications) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_DATE, convertDateToSortableFormat(date));
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_NOTIFICATIONS_ENABLED, enableNotifications ? 1 : 0);

        db.update(TABLE_EVENTS, values, COLUMN_EVENT_ID + " = ?", new String[]{String.valueOf(eventId)});
    }

    // Method to delete an event
    public void deleteEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, COLUMN_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
    }

    // Method to get all events
    public Cursor getAllEvents() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_EVENTS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_DATE
        );
    }

    // Convert date from MM-DD-YYYY to YYYY-MM-DD for proper sorting
    private String convertDateToSortableFormat(String date) {
        try {
            SimpleDateFormat sdfSource = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
            SimpleDateFormat sdfTarget = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return sdfTarget.format(sdfSource.parse(date));
        } catch (ParseException e) {
            return date; // If parsing fails, return the original date
        }
    }

    // Getter methods for column names
    public String getColumnEventId() {
        return COLUMN_EVENT_ID;
    }

    public String getColumnTitle() {
        return COLUMN_TITLE;
    }

    public String getColumnDescription() {
        return COLUMN_DESCRIPTION;
    }

    public String getColumnDate() {
        return COLUMN_DATE;
    }

    public String getColumnTime() {
        return COLUMN_TIME;
    }

    public String getColumnNotificationsEnabled() {
        return COLUMN_NOTIFICATIONS_ENABLED;
    }
}
