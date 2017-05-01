package com.example.android.oilsinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;


/**
 * Created by Mark on 4/18/2017.
 */

/**
 * {@link OilCursorAdapter} is an adapter for a list view using a {@link Cursor} of oil data as it's
 * data source. This adapter creates list items for each row of oil data in the {@link Cursor}.
 */
public class OilCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link OilCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public OilCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the oil data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current oil can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find individual views needed to modify in the list item layout.
        TextView nameTextView = (TextView) view.findViewById(R.id.oil_name);
        TextView sizeTextView = (TextView) view.findViewById(R.id.bottle_size);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.oil_quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.oil_price);

        // Find the columns and read the oil attributes from the Cursor for the current oil
        final String idColumn = cursor.getString(cursor.getColumnIndex(OilsEntry._ID));
        final String oilName = cursor.getString(cursor.getColumnIndex(OilsEntry.COLUMN_OIL_NAME));
        final String oilQuantity = cursor.getString(cursor.getColumnIndex(OilsEntry.COLUMN_OIL_QTY));
        final String oilSize = cursor.getString(cursor.getColumnIndex(OilsEntry.COLUMN_OIL_SIZE));
        final String oilImage = cursor.getString(cursor.getColumnIndex(OilsEntry.COLUMN_OIL_IMAGE));
        final double oilPrice = cursor.getDouble(cursor.getColumnIndex(OilsEntry.COLUMN_OIL_PRICE));

        // Update the TextViews with the attributes for the current oil
        nameTextView.setText(oilName);
        sizeTextView.setText(oilSize);
        quantityTextView.setText(oilQuantity);
        priceTextView.setText(Double.toString(oilPrice));

        final Uri oilUri = ContentUris.withAppendedId(OilsEntry.CONTENT_URI,
                Long.parseLong(idColumn));

        Button sellOil = (Button) view.findViewById(R.id.sell_oil);
        sellOil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int inventoryOil;
                if (TextUtils.isEmpty(quantityTextView.getText().toString())){
                    inventoryOil = 0;

                }else {
                    inventoryOil = Integer.parseInt(quantityTextView.getText().toString());
                }

                if (inventoryOil > 0){
                    inventoryOil = inventoryOil - 1;
                    if (inventoryOil == 0){
                        Toast.makeText(view.getContext(),
                                view.getContext().getString(R.string.low_inventory),
                                Toast.LENGTH_SHORT).show();
                    }
                    // Create a ContentValues object where column names are the keys,
                    // and pet attributes from the editor are the values.
                    ContentValues values = new ContentValues();
                    values.put(OilsEntry.COLUMN_OIL_IMAGE, oilImage);
                    values.put(OilsEntry.COLUMN_OIL_NAME, oilName);
                    values.put(OilsEntry.COLUMN_OIL_SIZE, oilSize);
                    values.put(OilsEntry.COLUMN_OIL_QTY, oilQuantity);

                    int rowsUpdated = view.getContext().getContentResolver().update(oilUri,
                            values, null, null);

                    if(rowsUpdated == 0) {
                        Toast.makeText(view.getContext(), "Error occurred",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}
