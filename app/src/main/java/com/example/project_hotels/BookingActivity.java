package com.example.project_hotels;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private TextView hotelNameTextView, hotelAddressTextView, hotelRatingTextView;
    private ImageView hotelImageView;
    private EditText guestNameEditText, phoneNumberEditText;
    private TextView checkInDateTextView, checkOutDateTextView;
    private Spinner numGuestsSpinner;
    private Button bookButton;
    private String selectedCheckInDate = "";
    private String selectedCheckOutDate = "";
    private int selectedGuests = 1;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.booking);

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        String hotelName = intent.getStringExtra("hotel_name");
        String hotelAddress = intent.getStringExtra("hotel_location");
        String hotelRating = intent.getStringExtra("hotel_rating");
        int hotelImage = intent.getIntExtra("hotel_image", R.drawable.room6);
        int price = intent.getIntExtra("price", 1000);
        String hotel_price = intent.getStringExtra("hotel_price");

        int hotelId = getHotelIdFromName(hotelName); // Get ID based on hotel name
        int roomNumber = hotelId; // Assign room number equal to hotelId

        hotelNameTextView = findViewById(R.id.hotelName);
        hotelAddressTextView = findViewById(R.id.hotelAddress);
        hotelRatingTextView = findViewById(R.id.hotelRating);
        hotelImageView = findViewById(R.id.hotelImage);
        guestNameEditText = findViewById(R.id.guestName);
        phoneNumberEditText = findViewById(R.id.phoneNumber);
        checkInDateTextView = findViewById(R.id.checkInDate);
        checkOutDateTextView = findViewById(R.id.checkOutDate);
        numGuestsSpinner = findViewById(R.id.numGuests);
        bookButton = findViewById(R.id.bookButton);

        hotelNameTextView.setText(hotelName);
        hotelAddressTextView.setText(hotelAddress);
        hotelRatingTextView.setText(hotelRating != null ? "⭐ " + hotelRating : "⭐ 4.5");
        hotelImageView.setImageResource(hotelImage);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.guest_numbers, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numGuestsSpinner.setAdapter(adapter);
        numGuestsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGuests = Integer.parseInt(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGuests = 1;
            }
        });

        checkInDateTextView.setOnClickListener(v -> showDatePickerDialog(true));
        checkOutDateTextView.setOnClickListener(v -> showDatePickerDialog(false));

        bookButton.setOnClickListener(v -> confirmBooking(
                hotelName, hotelAddress, hotelImage, price, hotelId, roomNumber
        ));
    }

    private void showDatePickerDialog(boolean isCheckIn) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    if (isCheckIn) {
                        selectedCheckInDate = selectedDate;
                        checkInDateTextView.setText(selectedDate);
                    } else {
                        selectedCheckOutDate = selectedDate;
                        checkOutDateTextView.setText(selectedDate);
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void confirmBooking(String hotelName, String hotelAddress, int hotelImage, int hotelPrice, int hotelId, int roomNumber) {
        String guestName = guestNameEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        if (guestName.isEmpty() || phoneNumber.isEmpty() || selectedCheckInDate.isEmpty() || selectedCheckOutDate.isEmpty()) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date checkInDate = dateFormat.parse(selectedCheckInDate);
            Date checkOutDate = dateFormat.parse(selectedCheckOutDate);
            if (checkOutDate.before(checkInDate)) {
                Toast.makeText(this, "Check-Out must be after Check-In", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = 1; // Change this if you implement user auth later

            boolean isAvailable = dbHelper.isRoomAvailable(hotelId, roomNumber, selectedCheckInDate, selectedCheckOutDate);
            if (!isAvailable) {
                Toast.makeText(this, "Room is not available for selected dates.", Toast.LENGTH_SHORT).show();
                return;
            }

            long insertResult = dbHelper.insertRoomBooking(userId, hotelId, roomNumber, selectedCheckInDate, selectedCheckOutDate);
            if (insertResult != -1) {
                Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(BookingActivity.this, ConfirmationActivity.class);
                intent.putExtra("hotel_name", hotelName);
                intent.putExtra("hotel_location", hotelAddress);
                intent.putExtra("hotel_image", hotelImage);
                intent.putExtra("price", hotelPrice);
                intent.putExtra("check_in", selectedCheckInDate);
                intent.putExtra("check_out", selectedCheckOutDate);
                intent.putExtra("num_guests", selectedGuests);
                intent.putExtra("hotel_id", hotelId);
                intent.putExtra("room_number", roomNumber);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Booking Failed! Try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid date selection", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ New method to assign hotelId based on hotel name
    private int getHotelIdFromName(String hotelName) {
        if (hotelName.equals("Hotel Royal Orchid")) {
            return 1;
        } else if (hotelName.equals("The Grand Horizon")) {
            return 2;
        } else if (hotelName.equals("Hotel Sapphire Bay")) {
            return 3;
        } else if (hotelName.equals("Orange City Inn")) {
            return 4;
        } else {
            return 5;
        }
    }
}
