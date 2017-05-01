package com.example.android.oilsinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;


/**
 * Created by Mark on 4/18/2017.
 */

public class OilsProvider extends ContentProvider{

    /** Tag for the log messages */
    public static final String LOG_TAG = OilsProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the oils table */
    private static final int OILS = 100;

    /** URI matcher code for the content URI for a single bottle of oil in the oils table */
    private static final int OIL_ID = 101;

    // UriMatcher object to match a content URI to a corresponding code.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(OilsContract.CONTENT_AUTHORITY, OilsContract.PATH_OILS, OILS);
        sUriMatcher.addURI(OilsContract.CONTENT_AUTHORITY, OilsContract.PATH_OILS + "/#", OIL_ID);
    }

    // database helper object
    private OilsDbHelper mDbHelper;

    // Initialize the provider and the database helper object.
    @Override
    public boolean onCreate() {
        mDbHelper = new OilsDbHelper(getContext());
        return true;
    }

    // Perform the query for the given URI.
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        // This cursor will hold the result of the query
        Cursor cursor;
        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case OILS:
                // Query oils table directly with the given projection, selection, selection args,
                // and sort order. The cursor may contain multiple rows of the oils table.
                cursor = database.query(OilsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case OIL_ID:
                // Extract out the ID for a particular oil(s) from the URI.
                selection = OilsEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                // Perform query on the oils table to return a Cursor containing a
                // particular row(s) of the table.
                cursor = database.query(OilsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        // Set notification URI on the Cursor,
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        //Return the cursor
        return cursor;
    }

    // Insert new data into the provider with the given ContentValues.
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case OILS:
                return insertOil(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    // Insert an oil into the database with given content values.
    // Return the new content URI for that specific row in the database.
    private Uri insertOil(Uri uri, ContentValues values) {
        String oilImage = values.getAsString(OilsEntry.COLUMN_OIL_IMAGE);
        if(oilImage == null || oilImage.equals("")) {
            throw new IllegalArgumentException("Valid image required");
        }
        // Check the name is not null
        String name = values.getAsString(OilsEntry.COLUMN_OIL_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Oil requires a name");
        }
        // Check the bottle size is valid
        Integer bottleSize = values.getAsInteger(OilsEntry.COLUMN_OIL_SIZE);
        if (bottleSize == null || !OilsEntry.isValidBottleSize(bottleSize)) {
            throw new IllegalArgumentException("Oil requires valid bottle size");
        }
        // Check the quantity is valid
        Integer quantity = values.getAsInteger(OilsEntry.COLUMN_OIL_QTY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Oil requires valid quantity");
        }
        // Verify price entered is a valid float
        double price = values.getAsDouble(OilsEntry.COLUMN_OIL_PRICE);
        if (price < 0) {
            //if (price == null || !OilsEntry.isValidPrice(price)) {
            throw new IllegalArgumentException("Oil requires valid price");
        }
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Insert the new oil with the given values
        long id = database.insert(OilsEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        //Notify all listeners the data has changed for the oil content URI
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    // Updates the data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case OILS:
                return updateOil(uri, contentValues, selection, selectionArgs);
            case OIL_ID:
                // Extract the ID from the URI, so we know which row to update.
                selection = OilsEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateOil(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    // Update oils in the database with the given content values. Apply the changes to the rows
    // specified in the selection and selection arguments (which could be 0 or 1 or more oils).
    // Return the number of rows that were successfully updated.
    private int updateOil(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link OilEntry#COLUMN_OIL_NAME} key is present & name value is not null.
        if (values.containsKey(OilsEntry.COLUMN_OIL_NAME)) {
            String name = values.getAsString(OilsEntry.COLUMN_OIL_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Oil requires a name");
            }
        }
        // If the {@link OilsEntry#COLUMN_OIL_SIZE} key is present & bottle size is valid
        if (values.containsKey(OilsEntry.COLUMN_OIL_SIZE)) {
            Integer bottleSize = values.getAsInteger(OilsEntry.COLUMN_OIL_SIZE);
            if (bottleSize == null || !OilsEntry.isValidBottleSize(bottleSize)) {
                throw new IllegalArgumentException("Oil requires valid bottle size");
            }
        }
        // If the {@link OilsEntry#COLUMN_OIL_QTY} key is present & quantity value is valid.
        if (values.containsKey(OilsEntry.COLUMN_OIL_QTY)) {
            // Check that the quantity is valid
            Integer quantity = values.getAsInteger(OilsEntry.COLUMN_OIL_QTY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Oil requires valid quantity");
            }
        }
        // If the {@link OilsEntry#COLUMN_OIL_PRICE} key is present & price value is valid.
        if (values.containsKey(OilsEntry.COLUMN_OIL_PRICE)) {
            // Verify price entered is a valid float
            Integer price = values.getAsInteger(OilsEntry.COLUMN_OIL_PRICE);
            if (price != null && price < 0) {
                //if (price == null || !OilsEntry.isValidPrice(price)) {
                throw new IllegalArgumentException("Oil requires valid price");
            }
        }
        // Get writable database to update data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(OilsEntry.TABLE_NAME, values, selection, selectionArgs);
        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated !=0){
            //Notify all listeners the data has changed for the oil content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    // Delete the data at the given selection and selection arguments.
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Track the number of rows that were deleted
        int rowsDeleted;
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case OILS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(OilsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case OIL_ID:
                // Delete a single row given by the ID in the URI
                selection = OilsEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(OilsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0){
            //Notify all listeners the data has changed for the pet content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of rows deleted
        return rowsDeleted;
    }

    // Returns the MIME type of data for the content URI.
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case OILS:
                return OilsEntry.CONTENT_LIST_TYPE;
            case OIL_ID:
                return OilsEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
