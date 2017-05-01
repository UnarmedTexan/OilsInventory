package com.example.android.oilsinventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;


/**
 * Created by Mark on 4/18/2017.
 */

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Identifier for the oil data loader
    private static final int EXISTING_OIL_LOADER = 0;

    // Content URI for the existing oil (null if it's a new oil)
    private Uri mCurrentOilUri;

    // EditText field to enter the name of the essential oil
    private EditText mNameEditText;

    // TextView field to display the quantity of bottles
    private TextView mQuantityTextView;

    // Button pressed to update inventory quantity
    private Button updateQty;

    // EditText field to enter inventory received
    private EditText receivedQty = (EditText) findViewById(R.id.edit_quantity_received);

    // EditText field to enter inventory sold
    private EditText soldQty = (EditText) findViewById(R.id.edit_quantity_sold);

    // Global variable for tracking increment and decrement of oil bottle quantity.
    private int oilQuantity = 0;

    // EditText field to enter the price of the essential oil
    private EditText mPriceEditText;

    // ImageView to display an image of the particular essential oil bottle
    private ImageView mOilImageView;

    // Spinner to select the size of the bottle
    private Spinner mSizeSpinner;

    /**
     * Size of bottle. The possible valid values are (in OilContract.java file):
     * {@link OilsEntry#FIFTEEN_ML} or {@link OilsEntry#FIVE_ML},
     */
    private int mSize = OilsEntry.FIFTEEN_ML;

    // Boolean flag to keep track of whether or not an oil has been edited (true) or not (false)
    private boolean mOilHasChanged = false;

    // OnTouchListener to listen for any user touches on a View, implying that they are modifying
    // the view, so we change the mOilHasChanged boolean to true.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mOilHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentOilUri = intent.getData();

        if (mCurrentOilUri == null) {
            setTitle(R.string.editor_activity_title_new_oil);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();

        } else {
            setTitle(getString(R.string.editor_activity_title_edit_oil));

            getLoaderManager().initLoader(EXISTING_OIL_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_oil_name);
        updateQty = (Button) findViewById(R.id.update_quantity);
        receivedQty = (EditText) findViewById(R.id.edit_quantity_received);
        soldQty = (EditText) findViewById(R.id.edit_quantity_sold);
        mPriceEditText = (EditText) findViewById(R.id.edit_oil_price);
        mSizeSpinner = (Spinner) findViewById(R.id.spinner_size);

        // OnTouchListeners for each input field
        mNameEditText.setOnTouchListener(mTouchListener);
        updateQty.setOnTouchListener(mTouchListener);
        receivedQty.setOnTouchListener(mTouchListener);
        soldQty.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSizeSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();
    }

    // Method is called when the Update button is clicked.
    public void updateQty(View v) {

        mQuantityTextView = (TextView) findViewById(R.id.oil_quantity);

        // Quantity displayed in the editor TextView for Inventory Quantity
        int quantity = Integer.parseInt(mQuantityTextView.getText().toString().trim());

        int mSold;
        int mReceived;

        // Retrieve values entered in the EditText for quantity sold and received
        String quantitySold = soldQty.getText().toString().trim();
        String quantityReceived = receivedQty.getText().toString().trim();
        if (quantitySold.isEmpty()) {
            mSold = 0;
        } else {
            mSold = Integer.parseInt(soldQty.getText().toString().trim());
        }

        if (quantityReceived.isEmpty()) {
            mReceived = 0;
        } else {
            mReceived = Integer.parseInt(receivedQty.getText().toString().trim());
        }

        // Verify inventory quantity is enough to support sold quantity
        if (mSold > mReceived + quantity) {
            Toast.makeText(EditorActivity.this, getString(R.string.not_enough_inventory),
                    Toast.LENGTH_LONG).show();
        } else {
            quantity = quantity + mReceived - mSold;
            mQuantityTextView.setText(quantity);
            // Notify user of a low inventory
            if (quantity < 5) {
                Toast.makeText(EditorActivity.this, getString(R.string.low_inventory),
                        Toast.LENGTH_LONG).show();
            }
        }
        oilQuantity = quantity;
        displayQuantity(oilQuantity);
    }

    // This method displays the given quantity value on the screen.
    private void displayQuantity(int quantityOfOil) {
        mQuantityTextView = (TextView) findViewById(R.id.oil_quantity);
        mQuantityTextView.setText("" + quantityOfOil);
    }

    // Setup the dropdown spinner that allows the user to select the oil bottle size.
    private void setupSpinner() {
        // Create adapter for spinner
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_size_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mSizeSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.size_fifteen))) {
                        mSize = OilsEntry.FIFTEEN_ML;
                    } else {
                        mSize = OilsEntry.FIVE_ML;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSize = OilsEntry.FIFTEEN_ML;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (mCurrentOilUri == null) {
            return null;
        }
        String[] projection = {
                OilsEntry._ID,
                OilsEntry.COLUMN_OIL_IMAGE,//
                OilsEntry.COLUMN_OIL_NAME,
                OilsEntry.COLUMN_OIL_SIZE,
                OilsEntry.COLUMN_OIL_QTY,
                OilsEntry.COLUMN_OIL_PRICE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentOilUri,         // Query the content URI for the current oil
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int imageColumnIndex = cursor.getColumnIndex(OilsEntry.COLUMN_OIL_IMAGE);
            int nameColumnIndex = cursor.getColumnIndex(OilsEntry.COLUMN_OIL_NAME);
            int sizeColumnIndex = cursor.getColumnIndex(OilsEntry.COLUMN_OIL_SIZE);
            int priceColumnIndex = cursor.getColumnIndex(OilsEntry.COLUMN_OIL_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(OilsEntry.COLUMN_OIL_QTY);

            // Extract out the value from the Cursor for the given column index
            String image = cursor.getString(imageColumnIndex);
            String name = cursor.getString(nameColumnIndex);
            int size = cursor.getInt(sizeColumnIndex);
            float price = cursor.getFloat(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Float.toString(price));
            mQuantityTextView.setText(Integer.toString(quantity));

            // size is a dropdown spinner
            switch (size) {
                case OilsEntry.FIFTEEN_ML:
                    mSizeSpinner.setSelection(1);
                    break;
                case OilsEntry.FIVE_ML:
                    mSizeSpinner.setSelection(2);
                    break;
                default:
                    mSizeSpinner.setSelection(1);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        //mOilImageView.setImageDrawable("");
        mNameEditText.setText("");
        mSizeSpinner.setSelection(0); // Select "15ml" bottle
        mPriceEditText.setText("0");
        mQuantityTextView.setText("1");
    }

    // Save user input from activity_editor and save new oil in database.
    private void saveOil() {
        // Read from input fields & trim
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        // Verify if this is supposed to be a new oil and EditText fields are empty
        if (mCurrentOilUri == null && TextUtils.isEmpty(nameString)
                && TextUtils.isEmpty(priceString)) {
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(OilsEntry.COLUMN_OIL_IMAGE, R.drawable.no_image_available);
        values.put(OilsEntry.COLUMN_OIL_NAME, nameString);
        values.put(OilsEntry.COLUMN_OIL_SIZE, mSize);
        values.put(OilsEntry.COLUMN_OIL_QTY, oilQuantity);

        // Parse string into float, but only if there's input from the user. Use 0 by default.
        float price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Float.parseFloat(priceString);
        }
        values.put(OilsEntry.COLUMN_OIL_PRICE, price);

        // Determine if this is a new or existing oil by checking if mCurrentOilUri is null
        if (mCurrentOilUri == null) {
            // Insert a new oil into the provider and return the content URI for this new oil.
            Uri newUri = getContentResolver().insert(OilsEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_oil_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_oil_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // For if this is an EXISTING oil. Update the oil with content URI: mCurrentOilUri
            // and pass in the new ContentValues.
            int rowsAffected = getContentResolver().update(mCurrentOilUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_oil_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_oil_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to delete an oil from the database.
    private void deleteOil() {
        // Verify the oil exists, prior to attempting to delete.
        if (mCurrentOilUri != null) {
            // Call ContentResolver to delete oil at the given content URI.
            int rowsDeleted = getContentResolver().delete(mCurrentOilUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // Display toast msg to notify the deletion failed.
                Toast.makeText(this, getString(R.string.editor_delete_oil_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Display toast msg to notify the deletion was successful.
                Toast.makeText(this, getString(R.string.editor_delete_oil_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    // This method is called after invalidateOptionsMenu(), so the menu can be updated
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new oil, hide the "Delete" menu item.
        if (mCurrentOilUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save oil to database
                saveOil();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Show a dialog that notifies the user they are deleting an oil
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the oil hasn't changed, continue with navigating up to {@link MainActivity}.
                if (!mOilHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                // Dialog to warn user of unsaved changes
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is called when user presses the back button and there have been no changes.
    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mOilHasChanged) {
            super.onBackPressed();
            return;
        }
        // Dialog to warn user of unsaved changes
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show a dialog that notifies the user they have unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // When user clicks the "Keep editing" button, dismiss & continue editing the oil.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the oil.
                deleteOil();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // When user clicks the "Cancel" button, dismiss & continue editing the oil.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
