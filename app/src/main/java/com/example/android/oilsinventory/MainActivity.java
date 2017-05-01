package com.example.android.oilsinventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.oilsinventory.data.OilsContract.OilsEntry;


public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // Identifier for the oils data loader
    private static final int OIL_LOADER = 0;

    // Adapter for the ListView
    OilCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView to be populated with oil data
        ListView oilListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, to be shown when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        oilListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of oil data in the Cursor.
        // If no oil data yet (until the loader finishes), pass in null for the Cursor.
        mCursorAdapter = new OilCursorAdapter(this, null);
        oilListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        oilListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent oilEditor = new Intent(MainActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific oil clicked on
                // For example, the URI would be "content://com.example.android.oils/oils/2"
                // if the oil with ID 2 was clicked on.
                Uri currentOilUri = ContentUris.withAppendedId(OilsEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                oilEditor.setData(currentOilUri);

                // Launch the {@link EditorActivity} to display the data for the current oil.
                startActivity(oilEditor);
            }
        });
        // Kick off the loader
        getLoaderManager().initLoader(OIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                OilsEntry._ID,
                OilsEntry.COLUMN_OIL_IMAGE,
                OilsEntry.COLUMN_OIL_NAME,
                OilsEntry.COLUMN_OIL_SIZE,
                OilsEntry.COLUMN_OIL_QTY,
                OilsEntry.COLUMN_OIL_PRICE };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                OilsEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link OilCursorAdapter} with this new cursor containing updated oil data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    // Helper method to insert hardcoded oil data into the database. For debugging purposes only.
    private void insertOils() {
        // Create a ContentValues object where column names are the keys,
        ContentValues values = new ContentValues();
        values.put(OilsEntry.COLUMN_OIL_IMAGE, R.drawable.no_image_available);
        values.put(OilsEntry.COLUMN_OIL_NAME, "Lavender");
        values.put(OilsEntry.COLUMN_OIL_SIZE, OilsEntry.FIFTEEN_ML);
        values.put(OilsEntry.COLUMN_OIL_QTY, 4);
        values.put(OilsEntry.COLUMN_OIL_PRICE, "$23.99");

        // Insert a new row for Lavender essential oil into the provider using the ContentResolver.
        // Use the {@link OilsEntry#CONTENT_URI} to indicate we want to insert into the oils
        // database table.
        // Receive the new content URI, allowing us to access Lavender oil data in the future.
        Uri newUri = getContentResolver().insert(OilsEntry.CONTENT_URI, values);
    }

    // Helper method to delete all oils in the database.
    private void deleteAllOils() {
        int rowsDeleted = getContentResolver().delete(OilsEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " rows deleted from oils database");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options to be added to the app bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertOils();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllOils();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
