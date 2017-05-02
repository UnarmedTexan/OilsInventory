package com.example.android.oilsinventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Mark on 4/18/2017.
 */

public class OilsContract {

    // Provide empty constructor to prevent someone from accidentally instantiating
    // the contract class.
    private OilsContract() {}

    // The string to use for the content authority
    public static final String CONTENT_AUTHORITY = "com.example.android.oilsinventory";

    // Base URI used to contact the content provider
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //Path used to appended base content URI for looking at oil data
    public static final String PATH_OILS = "oils";

    // Inner class to define constant values for the oils database table.
    // Each entry in the table represents a single essential oil.
    public static final class OilsEntry implements BaseColumns {

        // The MIME type of the {@link #CONTENT_URI} for a list of essential oils.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_OILS;

        // The MIME type of the {@link #CONTENT_URI} for a single oil.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_OILS;

        // The content URI to access the oil data in the provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_OILS);

        // Name of database table for oils
        public final static String TABLE_NAME = "oils";

        /**
         * Unique ID number for the pet (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Image of the oil bottle
         *
         * Type: TEXT
         */
        public final static String COLUMN_OIL_IMAGE = "image";

        /**
         * Name of the essential oil.
         *
         * Type: TEXT
         */
        public final static String COLUMN_OIL_NAME ="name";

        /**
         * Size of the oil bottle.
         *
         * The only possible values are {@link #FIFTEEN_ML} or {@link #FIVE_ML}
         *
         * Type: INTEGER
         */
        public final static String COLUMN_OIL_SIZE = "size";

        /**
         * Number of bottles for a particular type of oil.
         *
         * Type: INTEGER
         */
        public final static String COLUMN_OIL_QTY = "qty";

        /**
         * Price of the oil bottle.
         *
         * Type: TEXT
         */
        public final static String COLUMN_OIL_PRICE = "price";

        // Possible values for the size of the oil bottle.
        public static final int FIFTEEN_ML = 0;
        public static final int FIVE_ML = 1;

        // Returns whether or not the given bottle size is {@link #FIFTEEN_ML} or {@link #FIVE_ML}.
        public static boolean isValidBottleSize(int size) {
            if (size == FIFTEEN_ML || size == FIVE_ML ) {
                return true;
            }
            return false;
        }

        // Returns whether or not the price for a given oil is a float.
        public static boolean isValidPrice(float price){
            try {
                price = Float.parseFloat(COLUMN_OIL_PRICE);
            } catch (NumberFormatException e)
            {
                return false;
            }
            return true;
        }
    }
}
