package com.zybooks.eventtracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private EditText usernameEditText;
    private EditText passwordEditText;

    // Database helper object
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);

        // Set onClick listener for login button
        loginButton.setOnClickListener(v -> loginUser());

        // Set onClick listener for create account button
        createAccountButton.setOnClickListener(v -> registerUser());
    }

    // Method to handle user login
    private void loginUser() {
        // Get input from UI
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if input fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open database for reading
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query the database to check if the user exists
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?", new String[]{username, password});

        // Check if a match was found
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            // Successful login, navigate to DataGridActivity
            Intent intent = new Intent(MainActivity.this, DataGridActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Invalid login, show error message
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to handle user registration
    private void registerUser() {
        // Get input from UI
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if input fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open database for writing
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Query the database to check if the username already exists
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=?", new String[]{username});

        // Check if a match was found
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            // Username already exists, show error message
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
        } else {
            // Username does not exist, create new user
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("password", password);

            // Insert new user into database
            long newRowId = db.insert("users", null, values);

            // Check if the insert was successful
            if (newRowId != -1) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                // Successful registration, navigate to DataGridActivity
                Intent intent = new Intent(MainActivity.this, DataGridActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Error occurred during registration, show error message
                Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
