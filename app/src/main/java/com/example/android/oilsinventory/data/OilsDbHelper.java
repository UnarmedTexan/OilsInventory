package com.example.android.oilsinventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;

/**
 * Created by Mark on 4/18/2017.
 */

public class OilsDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = OilsDbHelper.class.getSimpleName();

    // Name of the oils database file
    private static final String DATABASE_NAME = "oils.db";

    // Database version.  Changing the schema requires the database version to be incremented.
    private static final int DATABASE_VERSION = 1;

    // String used to delete old table when updating database
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + OilsEntry.TABLE_NAME;

    /**
     * Constructs a new instance of {@link OilsDbHelper}.
     *
     * @param context of the app
     */
    public OilsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // This method is called when the database is being initially created.
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the oils table
        String SQL_CREATE_OILS_TABLE = "CREATE TABLE " + OilsEntry.TABLE_NAME + " ("
                + OilsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + OilsEntry.COLUMN_OIL_IMAGE + " TEXT NOT NULL, "
                + OilsEntry.COLUMN_OIL_NAME + " TEXT NOT NULL, "
                + OilsEntry.COLUMN_OIL_SIZE + " INTEGER NOT NULL, "
                + OilsEntry.COLUMN_OIL_QTY + " INTEGER NOT NULL, "
                + OilsEntry.COLUMN_OIL_PRICE + " FLOAT NOT NULL DEFAULT 0);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_OILS_TABLE);
    }

    // Method called when the database needs to be upgraded.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
