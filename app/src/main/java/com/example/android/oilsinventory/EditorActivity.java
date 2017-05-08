package com.example.android.oilsinventory;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Created by Mark on 4/18/2017.
 */

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Identifier for the oil data loader
    private static final int EXISTING_OIL_LOADER = 0;

    // Content URI for the existing oil (null if it's a new oil)
    private Uri mCurrentOilUri;

    // EditText field to enter the name of the essential oil
    private EditText mNameEditText;

    // Set image string to null if no image Uri is found.
    private String nameString;
    private String imageString;
    private String quantityString;
    private String priceString;


    // EditText field to enter inventory received
    private EditText initialQty;

    // TextView field to display the quantity of bottles
    private TextView mQuantityTextView;

    // Button pressed to update inventory quantity
    private Button updateQty;

    // EditText field to enter inventory received
    private EditText receivedQty;

    // EditText field to enter inventory sold
    private EditText soldQty;

    // EditText field to enter the price of the essential oil
    private EditText mPriceEditText;

    // Spinner to select the size of the bottle
    private Spinner mSizeSpinner;

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    private boolean isGalleryPicture = false;
    private Bitmap mBitmap;

    // Button used to access camera
    private Button mButtonTakePicture;

    // Button used to access gallery
    private Button mButtonSelectPicture;

    // ImageView to display an image of the particular essential oil bottle
    private ImageView mImageView;
    private static final String FILE_PROVIDER_AUTHORITY =
            "com.example.android.oilsinventory.fileprovider";
    private Uri mImageUri;
    private static final String STATE_URI = "STATE_URI";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";

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
            View updateQuantity = findViewById(R.id.update_inventory);
            updateQuantity.setVisibility(View.INVISIBLE);
            invalidateOptionsMenu();

        } else {
            setTitle(getString(R.string.editor_activity_title_edit_oil));
            View initialQuantity = findViewById(R.id.initial_inventory);
            initialQuantity.setVisibility(View.INVISIBLE);
            getLoaderManager().initLoader(EXISTING_OIL_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_oil_name);
        initialQty = (EditText) findViewById(R.id.enter_initial_quantity);
        updateQty = (Button) findViewById(R.id.update_quantity);
        receivedQty = (EditText) findViewById(R.id.edit_quantity_received);
        soldQty = (EditText) findViewById(R.id.edit_quantity_sold);
        mPriceEditText = (EditText) findViewById(R.id.edit_oil_price);
        mSizeSpinner = (Spinner) findViewById(R.id.spinner_size);
        mButtonTakePicture = (Button) findViewById(R.id.take_image);
        mButtonSelectPicture = (Button) findViewById(R.id.add_image);

        // OnTouchListeners for each input field
        mNameEditText.setOnTouchListener(mTouchListener);
        initialQty.setOnTouchListener(mTouchListener);
        updateQty.setOnTouchListener(mTouchListener);
        receivedQty.setOnTouchListener(mTouchListener);
        soldQty.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSizeSpinner.setOnTouchListener(mTouchListener);
        mButtonTakePicture.setOnTouchListener(mTouchListener);
        mButtonSelectPicture.setOnTouchListener(mTouchListener);

        setupSpinner();

        // Image items
        mImageView = (ImageView) findViewById(R.id.oil_image);
        mButtonTakePicture.setEnabled(false);
        requestPermissions();
    }

    // Method is called when the Update button is clicked.
    public void updateQty(View v) {

        mQuantityTextView = (TextView) findViewById(R.id.oil_quantity);

        // Quantity displayed in the editor TextView for Inventory Quantity
        int quantity = Integer.parseInt(mQuantityTextView.getText().toString().trim());

        // Initialize to hold sold and received inventory quantities
        int mSold;
        int mReceived;

        // Retrieve values entered in the EditText for quantity sold and received
        String quantitySold = soldQty.getText().toString().trim();
        String quantityReceived = receivedQty.getText().toString().trim();

        // Verify if no sold EditText input then set to "0", otherwise convert user entry
        if (quantitySold.isEmpty()) {
            mSold = 0;
        } else {
            mSold = Integer.parseInt(soldQty.getText().toString().trim());
        }

        // Verify if no received EditText input then set to "0", otherwise convert user entry
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
            mQuantityTextView.setText("" + quantity);
            // Notify user of a low inventory
            if (quantity < 5) {
                Toast.makeText(EditorActivity.this, getString(R.string.low_inventory),
                        Toast.LENGTH_LONG).show();
            }
        }
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
                OilsEntry.COLUMN_OIL_IMAGE,
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

        mQuantityTextView = (TextView) findViewById(R.id.oil_quantity);

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
            mImageUri = Uri.parse(cursor.getString(imageColumnIndex));
            String name = cursor.getString(nameColumnIndex);
            int size = cursor.getInt(sizeColumnIndex);
            float price = cursor.getFloat(priceColumnIndex);
            String quantity = cursor.getString(quantityColumnIndex);


            // Get the image from the Uri
            Bitmap bitmap = getBitmapFromUri(mImageUri);

            // Update the views on the screen with the values from the database
            mImageView.setImageBitmap(bitmap);
            mNameEditText.setText(name);
            mPriceEditText.setText(Float.toString(price));
            mQuantityTextView.setText(quantity);

            // size is a dropdown spinner
            switch (size) {
                case OilsEntry.FIFTEEN_ML:
                    mSizeSpinner.setSelection(0);
                    break;
                case OilsEntry.FIVE_ML:
                    mSizeSpinner.setSelection(1);
                    break;
                default:
                    mSizeSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mImageView.setImageResource(R.drawable.no_image_available);
        mNameEditText.setText("");
        mSizeSpinner.setSelection(0); // Select "15ml" bottle
        mPriceEditText.setText("0");
        mQuantityTextView.setText("1");
    }

    // Save user input from activity_editor and save new oil in database.
    private void saveOil() {

        // Read from Name and Price input fields & trim
        nameString = mNameEditText.getText().toString().trim();
        priceString = mPriceEditText.getText().toString().trim();

        // Set image string to null if no image Uri is found.
        if (mImageUri != null) {
            imageString = mImageUri.toString();
        } else {
            imageString = null;
        }

        if (mQuantityTextView != null) {
            // Quantity from edit oil
            quantityString = mQuantityTextView.getText().toString().trim();
        } else {
            // Quantity from initial oil
            quantityString = initialQty.getText().toString().trim();
        }

        // Create a ContentValues object where column names are the keys,
        // and oil attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(OilsEntry.COLUMN_OIL_NAME, nameString);
        values.put(OilsEntry.COLUMN_OIL_SIZE, mSize);
        values.put(OilsEntry.COLUMN_OIL_QTY, quantityString);
        values.put(OilsEntry.COLUMN_OIL_IMAGE, imageString);

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

    private boolean entryComplete() {

        // Set image string to null if no image Uri is found.
        if (mImageUri != null) {
            imageString = mImageUri.toString();
        } else {
            imageString = "";
        }

        if (mQuantityTextView != null) {
            // Quantity from edit oil
            quantityString = mQuantityTextView.getText().toString().trim();
        } else {
            // Quantity from initial oil
            quantityString = initialQty.getText().toString().trim();
        }
        nameString = mNameEditText.getText().toString().trim();
        priceString = mPriceEditText.getText().toString().trim();
        if (imageString.equals("") || nameString.equals("") || priceString.equals("")
                || quantityString.equals("")) {
            return false;
        } else {
            return true;
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
                if (!entryComplete()) {
                    // not all Editable fields have been modified.
                    Toast.makeText(this, getString(R.string.oil_not_added) + "\n"
                                    + getString(R.string.oil_not_added_part2),
                            Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    saveOil();
                    // Exit activity
                    finish();
                    return true;
                }
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

    // create intent to use email for ordering inventory of a particular oil
    public void orderOils(View view) {
        String oilName = mNameEditText.getText().toString().trim();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + getText(R.string.order_email)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order " + oilName);
        if (emailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(emailIntent.createChooser(emailIntent, "Send Email"));
        }
    }

    // The following code provided by forum mentor for the purpose of accessing camera and gallery
    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);
            }
        } else {
            mButtonTakePicture.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mButtonTakePicture.setEnabled(true);
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void selectPicture(View view) {
        Intent intent;
        Log.e(LOG_TAG, "While is set and the ifs are worked through.");

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // Show only images, no videos or anything else
        Log.e(LOG_TAG, "Check write to external permissions");

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void takePicture(View view) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File f = createImageFile();

            Log.d(LOG_TAG, "File: " + f.getAbsolutePath());

            mImageUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, f);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);

            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = getPackageManager()
                        .queryIntentActivities(takePictureIntent,
                                PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, mImageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mImageUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mImageUri.toString());

                mBitmap = getBitmapFromUri(mImageUri);
                mImageView.setImageBitmap(mBitmap);
                mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                isGalleryPicture = true;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            mBitmap = getBitmapFromUri(mImageUri);
            mImageView.setImageBitmap(mBitmap);
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            isGalleryPicture = false;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }
        }
        return storageDir;
    }

    // If hardware is rotated, ensure image is not lost before prior to saving
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mImageUri != null)
            outState.putString(STATE_URI, mImageUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_URI) &&
                !savedInstanceState.getString(STATE_URI).equals("")) {
            mImageUri = Uri.parse(savedInstanceState.getString(STATE_URI));

            ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mImageView.setImageBitmap(getBitmapFromUri(mImageUri));
                }
            });
        }
    }
}
