package com.example.project_hotels;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserDatabase.db";
    private static final int DATABASE_VERSION = 3;
    private Context context;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_ROOM_BOOKINGS = "room_bookings";
    public static final String TABLE_COUPONS = "coupons";

    private static final String CREATE_TABLE_USERS = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT, " +
            "email TEXT UNIQUE, " +
            "mobile TEXT, " +
            "password TEXT);";

    private static final String CREATE_TABLE_ROOM_BOOKINGS = "CREATE TABLE IF NOT EXISTS " + TABLE_ROOM_BOOKINGS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER, " +
            "hotel_id INTEGER, " +
            "room_number INTEGER, " +
            "check_in_date TEXT, " +
            "check_out_date TEXT, " +
            "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id));";
    private static final String CREATE_TABLE_COUPONS = "CREATE TABLE IF NOT EXISTS " + TABLE_COUPONS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "code TEXT UNIQUE);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_ROOM_BOOKINGS);
//        db.execSQL(CREATE_TABLE_COUPONS);
        db.execSQL("CREATE TABLE IF NOT EXISTS coupons (code TEXT PRIMARY KEY)");
        Log.d("DatabaseHelper", "Tables Created Successfully: " + TABLE_USERS + ", " + TABLE_ROOM_BOOKINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOM_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUPONS);
        onCreate(db);
    }

    public boolean isRoomAvailable(int hotelId, int roomNumber, String checkInDate, String checkOutDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        // This will only return conflicts for the same hotel and room
        String query = "SELECT * FROM " + TABLE_ROOM_BOOKINGS +
                " WHERE hotel_id = ? AND room_number = ? AND NOT (" +
                "check_out_date <= ? OR check_in_date >= ?)";

        Cursor cursor = db.rawQuery(query, new String[] {
                String.valueOf(hotelId),     // Specific hotel only
                String.valueOf(roomNumber),  // Specific room only (1 room per hotel, fixed)
                checkInDate,                 // New booking check-in
                checkOutDate                 // New booking check-out
        });

        boolean isAvailable = !cursor.moveToFirst();  // True if no conflict
        cursor.close();
        return isAvailable;
    }





    public int getAvailableRoomNumber(int hotelId, String checkInDate, String checkOutDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT DISTINCT room_number FROM " + TABLE_ROOM_BOOKINGS +
                " WHERE hotel_id = ? AND (" +
                "(check_in_date <= ? AND check_out_date >= ?) OR " +
                "(check_in_date <= ? AND check_out_date >= ?))";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(hotelId), checkInDate, checkInDate, checkOutDate, checkOutDate});
        List<Integer> bookedRooms = new ArrayList<>();
        while (cursor.moveToNext()) {
            bookedRooms.add(cursor.getInt(0));
        }
        cursor.close();

        for (int i = 1; i <= 10; i++) {
            if (!bookedRooms.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    public long insertRoomBooking(int userId, int hotelId, int roomNumber, String checkInDate, String checkOutDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("hotel_id", hotelId);
        values.put("room_number", roomNumber);
        values.put("check_in_date", checkInDate);
        values.put("check_out_date", checkOutDate);

        long result = db.insert(TABLE_ROOM_BOOKINGS, null, values);
        db.close();
        return result;
    }
    public void insertDummyCoupon() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("code", "DISCOUNT10");

        long result = db.insertWithOnConflict(TABLE_COUPONS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();

        // Show toast only if inserted (result ≠ -1)
        if (result != -1) {
            Toast.makeText(context, "Coupon inserted", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isCouponValid(String code) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COUPONS + " WHERE code = ?", new String[]{code});
        boolean isValid = cursor.moveToFirst();
        cursor.close();
        return isValid;
    }

    public int getHotelIdFromName(String hotelName) {
        if (hotelName.equals("Hotel Royal Orchid")) {
            return 1;
        } else if (hotelName.equals("The Grand Horizon")) {
            return 2;
        } else if (hotelName.equals("Hotel Sapphire Bay")) {
            return 3;
        } else if (hotelName.equals("Orange City Inn")) {
            return 4;
        } else {
            return 5; // Default or for any other hotel
        }
    }

}
