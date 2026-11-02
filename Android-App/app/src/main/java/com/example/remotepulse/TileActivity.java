package com.example.remotepulse;

import android.os.Bundle;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


/*
 * Main tile screen of the Remote Pulse Android application.
 *
 * Responsibilities:
 *
 * 1. Display the 18 Remote Pulse tiles in a 3 x 6 grid.
 * 2. Fetch tile configurations from the Spring Boot backend.
 * 3. Load tile icons from the backend.
 * 4. Detect the physical orientation of the phone.
 * 5. Rotate tile content without rotating the grid.
 * 6. Listen for backend tile updates using Server-Sent Events (SSE).
 * 7. Send launch requests when a tile is tapped.
 */
public class TileActivity extends AppCompatActivity {

    // GridLayout containing all 18 tile containers.
    private GridLayout tileGrid;


    /*
     * Root view of the Activity.
     *
     * Used to calculate the actual available screen dimensions
     * after accounting for system bar insets.
     */
    private View mainView;


    /*
     * Detects the physical orientation of the phone.
     *
     * The Activity and grid remain in portrait orientation.
     * Only the content inside individual tiles rotates.
     */
    private OrientationEventListener orientationEventListener;


    // Stores the current rotation applied to tile content.
    private float currentContentRotation = 0f;


    /*
     * Spring Boot server address received from MainActivity.
     *
     * Example: 192.168.31.78:8080
     */
    private String serverAddress;


    /*
     * Background thread responsible for listening to
     * Server-Sent Events from the Spring Boot backend.
     */
    private Thread sseThread;


    /*
     * Active HTTP connection used by the SSE listener.
     * Stored so it can be disconnected when the Activity is destroyed.
     */
    private HttpURLConnection sseConnection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display.
        EdgeToEdge.enable(this);


        /*
         * ==========================================
         * FULLSCREEN MODE
         * ==========================================
         *
         * Hide Android system bars to provide a fullscreen
         * experience while using the Remote Pulse tile grid.
         *
         * The bars can temporarily reappear when the user
         * swipes from the screen edges.
         */
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );


        windowInsetsController.hide(
                WindowInsetsCompat.Type.systemBars()
        );


        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );


        // Load the tile screen layout.
        setContentView(
                R.layout.activity_tile
        );


        /*
         * ==========================================
         * ROOT VIEW
         * ==========================================
         *
         * Used for measuring the usable screen area
         * when dynamically creating the tile grid.
         */
        mainView =
                findViewById(
                        R.id.main
                );


        /*
         * ==========================================
         * SYSTEM BAR INSETS
         * ==========================================
         *
         * Apply the system bar dimensions as padding
         * so content does not overlap system UI areas.
         */
        ViewCompat.setOnApplyWindowInsetsListener(
                mainView,
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );


                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );


                    return insets;
                }
        );


        /*
         * ==========================================
         * TILE GRID
         * ==========================================
         */
        tileGrid =
                findViewById(
                        R.id.tileGrid
                );


        /*
         * ==========================================
         * PHYSICAL ORIENTATION DETECTION
         * ==========================================
         *
         * Detects how the user physically holds the phone.
         *
         * The Activity itself does not rotate.
         * The grid remains 3 x 6.
         * Only the icon or placeholder inside each tile rotates.
         */
        orientationEventListener =
                new OrientationEventListener(this) {

                    @Override
                    public void onOrientationChanged(
                            int orientation
                    ) {

                        // Ignore orientation readings that Android cannot determine.
                        if (
                                orientation
                                        == ORIENTATION_UNKNOWN
                        ) {

                            return;
                        }


                        /*
                         * Landscape direction 1.
                         * Rotate tile content by -90 degrees.
                         */
                        if (
                                orientation >= 60
                                        && orientation <= 120
                        ) {

                            rotateTileContents(
                                    -90f
                            );


                            /*
                             * Landscape direction 2.
                             * Rotate tile content by 90 degrees.
                             */
                        } else if (
                                orientation >= 240
                                        && orientation <= 300
                        ) {

                            rotateTileContents(
                                    90f
                            );


                            /*
                             * Portrait orientation.
                             * Restore the original content rotation.
                             */
                        } else if (
                                orientation >= 330
                                        || orientation <= 30
                        ) {

                            rotateTileContents(
                                    0f
                            );
                        }
                    }
                };


        // Enable the orientation listener if the device supports it.
        if (
                orientationEventListener
                        .canDetectOrientation()
        ) {

            orientationEventListener.enable();
        }


        /*
         * ==========================================
         * GET SERVER ADDRESS
         * ==========================================
         *
         * MainActivity passes the address entered by the user
         * through an Intent when the connection succeeds.
         */
        serverAddress =
                getIntent()
                        .getStringExtra(
                                "SERVER_ADDRESS"
                        );


        /*
         * Fetch the current tile configuration from the backend.
         */
        loadTiles();


        /*
         * Start listening for tile configuration changes.
         */
        listenForTileUpdates();
    }


    /*
     * ==========================================
     * ROTATE TILE CONTENT ONLY
     * ==========================================
     *
     * Applies rotation to the first child inside every
     * FrameLayout tile container.
     *
     * The tile containers themselves are never rotated,
     * so the overall grid arrangement remains unchanged.
     */
    private void rotateTileContents(
            float rotation
    ) {

        // Avoid applying the same rotation repeatedly.
        if (
                currentContentRotation
                        == rotation
        ) {

            return;
        }


        // Remember the latest applied rotation.
        currentContentRotation =
                rotation;


        // Iterate through every child in the tile grid.
        for (
                int i = 0;
                i < tileGrid.getChildCount();
                i++
        ) {

            View tileView =
                    tileGrid.getChildAt(
                            i
                    );


            // Only process FrameLayout tile containers.
            if (!(tileView instanceof FrameLayout)) {

                continue;
            }


            FrameLayout tileContainer =
                    (FrameLayout) tileView;


            /*
             * Each tile contains either:
             * - A TextView for an empty slot.
             * - An ImageView for a configured tile.
             */
            if (
                    tileContainer.getChildCount()
                            > 0
            ) {

                View content =
                        tileContainer.getChildAt(
                                0
                        );


                // Rotate the content without rotating its container.
                content.setRotation(
                        rotation
                );
            }
        }
    }


    /*
     * ==========================================
     * LOAD TILES FROM SPRING BOOT
     * ==========================================
     *
     * Sends an HTTP GET request to:
     *
     * http://SERVER_ADDRESS/api/tiles
     *
     * The backend returns the configured tiles as JSON.
     * Gson converts that JSON response into a List<Tile>.
     */
    private void loadTiles() {

        // Network operations run on a background thread.
        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            try {

                // Construct the backend API URL.
                String urlString =
                        "http://"
                                + serverAddress
                                + "/api/tiles";


                URL url =
                        new URL(
                                urlString
                        );


                // Open the HTTP connection.
                connection =
                        (HttpURLConnection)
                                url.openConnection();


                // Configure the request method.
                connection.setRequestMethod(
                        "GET"
                );


                // Maximum time allowed to establish the connection.
                connection.setConnectTimeout(
                        3000
                );


                // Maximum time allowed to receive the response.
                connection.setReadTimeout(
                        3000
                );


                // Send the request and obtain the HTTP response code.
                int responseCode =
                        connection.getResponseCode();


                // Stop processing if the backend did not return HTTP 200.
                if (responseCode != 200) {

                    throw new RuntimeException(
                            "Server returned "
                                    + responseCode
                    );
                }


                /*
                 * Read the JSON response from the server.
                 *
                 * BufferedReader reads the response line by line.
                 * StringBuilder combines the lines into one JSON string.
                 */
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );


                StringBuilder json =
                        new StringBuilder();


                String line;


                while (
                        (line = reader.readLine())
                                != null
                ) {

                    json.append(
                            line
                    );
                }


                reader.close();


                /*
                 * Define the expected JSON structure.
                 *
                 * The response represents a list of Tile objects,
                 * rather than a single Tile.
                 */
                Type tileListType =
                        new TypeToken<List<Tile>>() {
                        }.getType();


                /*
                 * Gson converts the JSON string into Java objects.
                 * Each element in the resulting list represents one tile.
                 */
                List<Tile> tiles =
                        new Gson().fromJson(
                                json.toString(),
                                tileListType
                        );


                /*
                 * Return to the UI thread to dynamically
                 * create and display the tiles.
                 */
                runOnUiThread(
                        () -> displayTiles(
                                tiles
                        )
                );


            } catch (Exception e) {

                // Print technical details to the Android log.
                e.printStackTrace();


                // Display an error message to the user.
                runOnUiThread(
                        () -> Toast.makeText(
                                TileActivity.this,
                                "Failed to load tiles",
                                Toast.LENGTH_SHORT
                        ).show()
                );


            } finally {

                // Release the HTTP connection.
                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    /*
     * ==========================================
     * SERVER-SENT EVENTS (SSE) LISTENER
     * ==========================================
     *
     * Connects to:
     *
     * http://SERVER_ADDRESS/api/tiles/events
     *
     * SSE allows the backend to notify the Android app
     * when tile configurations change.
     *
     * When the Android app receives:
     *
     * data:TILES_CHANGED
     *
     * it fetches the latest tile configuration again.
     */
    private void listenForTileUpdates() {

        sseThread =
                new Thread(() -> {

                    /*
                     * Keep listening until the thread is interrupted,
                     * normally when the Activity is destroyed.
                     */
                    while (
                            !Thread.currentThread()
                                    .isInterrupted()
                    ) {

                        HttpURLConnection connection =
                                null;


                        try {

                            // Construct the SSE endpoint URL.
                            String urlString =
                                    "http://"
                                            + serverAddress
                                            + "/api/tiles/events";


                            URL url =
                                    new URL(
                                            urlString
                                    );


                            // Open and store the SSE connection.
                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();


                            sseConnection =
                                    connection;


                            // SSE uses an HTTP GET request.
                            connection.setRequestMethod(
                                    "GET"
                            );


                            // Tell the backend that the client expects an event stream.
                            connection.setRequestProperty(
                                    "Accept",
                                    "text/event-stream"
                            );


                            // Maximum time allowed to establish the connection.
                            connection.setConnectTimeout(
                                    3000
                            );


                            /*
                             * Disable the read timeout.
                             *
                             * Unlike a normal HTTP request, an SSE connection
                             * is designed to remain open while waiting for events.
                             */
                            connection.setReadTimeout(
                                    0
                            );


                            int responseCode =
                                    connection.getResponseCode();


                            // Ensure that the SSE endpoint connected successfully.
                            if (responseCode != 200) {

                                throw new RuntimeException(
                                        "SSE connection failed: "
                                                + responseCode
                                );
                            }


                            // Read incoming SSE messages from the server.
                            BufferedReader reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    connection.getInputStream()
                                            )
                                    );


                            String line;


                            while (
                                    (line = reader.readLine())
                                            != null
                            ) {

                                /*
                                 * Check whether the backend has reported
                                 * that tile configurations have changed.
                                 */
                                if (
                                        line.equals(
                                                "data:TILES_CHANGED"
                                        )
                                ) {

                                    // Fetch the updated tile configuration.
                                    loadTiles();
                                }
                            }


                            reader.close();


                        } catch (Exception e) {

                            // Log connection errors before attempting to reconnect.
                            e.printStackTrace();


                        } finally {

                            // Release the current SSE connection.
                            if (connection != null) {

                                connection.disconnect();
                            }
                        }


                        /*
                         * Wait briefly before attempting to reconnect.
                         * This prevents continuous rapid reconnection attempts.
                         */
                        try {

                            Thread.sleep(
                                    1000
                            );


                        } catch (InterruptedException e) {

                            // Restore the interrupted status and stop the loop.
                            Thread.currentThread()
                                    .interrupt();

                            break;
                        }
                    }
                });


        // Start the background SSE listener.
        sseThread.start();
    }


    /*
     * ==========================================
     * DISPLAY TILES
     * ==========================================
     *
     * Dynamically creates the 18 tiles using the data
     * received from the Spring Boot backend.
     *
     * The grid always follows this arrangement:
     *
     *  1   2   3
     *  4   5   6
     *  7   8   9
     * 10  11  12
     * 13  14  15
     * 16  17  18
     *
     * The grid itself does not change orientation.
     */
    private void displayTiles(
            List<Tile> tiles
    ) {

        /*
         * Post the operation to the root view.
         *
         * This ensures Android has completed the layout pass
         * and calculated the actual dimensions of mainView.
         */
        mainView.post(() -> {

            // Remove previously displayed tiles before rebuilding the grid.
            tileGrid.removeAllViews();


            /*
             * Configure the fixed 3-column, 6-row grid.
             */
            tileGrid.setColumnCount(
                    3
            );


            tileGrid.setRowCount(
                    6
            );


            // Margin around each tile, measured in pixels.
            int margin =
                    5;


            /*
             * ==========================================
             * CALCULATE AVAILABLE SCREEN SIZE
             * ==========================================
             *
             * Obtain the usable width and height of the root view.
             * Subtract its padding so that system inset areas
             * are not included in the tile calculations.
             */
            int availableWidth =
                    mainView.getWidth()
                            - mainView.getPaddingLeft()
                            - mainView.getPaddingRight();


            int availableHeight =
                    mainView.getHeight()
                            - mainView.getPaddingTop()
                            - mainView.getPaddingBottom();


            // Do not create tiles if the view has no usable dimensions.
            if (
                    availableWidth <= 0
                            || availableHeight <= 0
            ) {

                return;
            }


            /*
             * ==========================================
             * CALCULATE TILE DIMENSIONS
             * ==========================================
             *
             * Calculate the width required for three columns
             * and the height required for six rows.
             *
             * Margins are deducted from the available dimensions
             * before calculating the size of each tile.
             */
            int tileWidth =
                    (
                            availableWidth
                                    - (margin * 2 * 3)
                    ) / 3;


            int tileHeight =
                    (
                            availableHeight
                                    - (margin * 2 * 6)
                    ) / 6;


            /*
             * ==========================================
             * CREATE ALL 18 TILE CONTAINERS
             * ==========================================
             *
             * Every slot gets a FrameLayout.
             * The FrameLayout acts as the container for
             * either an empty-slot placeholder or a tile icon.
             */
            for (
                    int slot = 1;
                    slot <= 18;
                    slot++
            ) {

                Tile currentTile =
                        null;


                /*
                 * Find the Tile object corresponding to
                 * the current grid slot.
                 *
                 * Slots without a matching Tile object
                 * are displayed as empty slots.
                 */
                for (Tile tile : tiles) {

                    if (
                            tile.getSlotNumber() != null
                                    && tile.getSlotNumber() == slot
                    ) {

                        currentTile =
                                tile;

                        break;
                    }
                }


                /*
                 * ==========================================
                 * TILE CONTAINER
                 * ==========================================
                 *
                 * Each tile uses a FrameLayout so its
                 * icon or placeholder can fill the container.
                 */
                FrameLayout tileContainer =
                        new FrameLayout(
                                this
                        );


                // Apply the shared tile background drawable.
                tileContainer.setBackgroundResource(
                        R.drawable.tile_background
                );


                // Configure the tile's position and dimensions in the grid.
                GridLayout.LayoutParams gridParams =
                        new GridLayout.LayoutParams();


                gridParams.width =
                        tileWidth;

                gridParams.height =
                        tileHeight;


                // Apply spacing around the tile.
                gridParams.setMargins(
                        margin,
                        margin,
                        margin,
                        margin
                );


                tileContainer.setLayoutParams(
                        gridParams
                );


                /*
                 * ==========================================
                 * EMPTY TILE
                 * ==========================================
                 *
                 * Display a question mark when a slot has
                 * no configured tile or no icon.
                 */
                if (
                        currentTile == null
                                || currentTile.getIcon() == null
                ) {

                    TextView emptyView =
                            new TextView(
                                    this
                            );


                    emptyView.setText(
                            "?"
                    );


                    emptyView.setTextColor(
                            0xFFFFFFFF
                    );


                    emptyView.setTextSize(
                            24
                    );


                    // Centre the question mark inside the tile.
                    emptyView.setGravity(
                            Gravity.CENTER
                    );


                    emptyView.setBackgroundColor(
                            0xFF202020
                    );


                    // Make the placeholder occupy the entire tile.
                    FrameLayout.LayoutParams emptyParams =
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                            );


                    emptyView.setLayoutParams(
                            emptyParams
                    );


                    // Apply the current physical orientation.
                    emptyView.setRotation(
                            currentContentRotation
                    );


                    // Add the placeholder to its tile container.
                    tileContainer.addView(
                            emptyView
                    );
                }


                /*
                 * ==========================================
                 * CONFIGURED TILE
                 * ==========================================
                 *
                 * If a tile and its icon are available,
                 * create an ImageView and load the icon from
                 * the Spring Boot server using Glide.
                 */
                else {

                    ImageView iconView =
                            new ImageView(
                                    this
                            );


                    // Fit the icon inside the available image area.
                    iconView.setScaleType(
                            ImageView.ScaleType.FIT_CENTER
                    );


                    // Configure the icon to fill the tile container.
                    FrameLayout.LayoutParams iconParams =
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                            );


                    // Keep a small margin around the icon.
                    iconParams.setMargins(
                            4,
                            4,
                            4,
                            4
                    );


                    iconView.setLayoutParams(
                            iconParams
                    );


                    // Get the icon path provided by the backend.
                    String iconPath =
                            currentTile.getIcon();


                    String iconUrl;


                    /*
                     * Built-in system icons.
                     *
                     * These are served from the system-icons directory
                     * exposed by the Spring Boot backend.
                     */
                    if (
                            iconPath.startsWith(
                                    "system-icons/"
                            )
                    ) {

                        iconUrl =
                                "http://"
                                        + serverAddress
                                        + "/"
                                        + iconPath;

                    }


                    /*
                     * Custom or runtime icons.
                     *
                     * These are served through the backend's
                     * /icons/ resource path.
                     */
                    else {

                        iconUrl =
                                "http://"
                                        + serverAddress
                                        + "/icons/"
                                        + iconPath;
                    }


                    /*
                     * ==========================================
                     * LOAD ICON USING GLIDE
                     * ==========================================
                     *
                     * A timestamp is added to the URL and used
                     * as a Glide signature to avoid displaying
                     * an outdated cached icon after an update.
                     */
                    long version =
                            System.currentTimeMillis();


                    Glide.with(
                                    this
                            )
                            .load(
                                    iconUrl
                                            + "?v="
                                            + version
                            )
                            .signature(
                                    new ObjectKey(
                                            version
                                    )
                            )
                            .diskCacheStrategy(
                                    DiskCacheStrategy.NONE
                            )
                            .skipMemoryCache(
                                    true
                            )
                            .into(
                                    iconView
                            );


                    // Apply the current physical orientation to the icon.
                    iconView.setRotation(
                            currentContentRotation
                    );


                    // Add the icon to its tile container.
                    tileContainer.addView(
                            iconView
                    );
                }


                /*
                 * ==========================================
                 * TILE CLICK HANDLER
                 * ==========================================
                 *
                 * Capture the current slot number so that
                 * tapping this tile launches the corresponding
                 * slot on the Windows computer.
                 */
                final int slotNumber =
                        slot;


                tileContainer.setOnClickListener(
                        view ->
                                launchSlot(
                                        slotNumber
                                )
                );


                // Add the completed tile container to the grid.
                tileGrid.addView(
                        tileContainer
                );
            }
        });
    }


    /*
     * ==========================================
     * LAUNCH TILE SLOT
     * ==========================================
     *
     * Sends an HTTP POST request to:
     *
     * /api/tiles/slot/{slotNumber}/launch
     *
     * The Spring Boot backend identifies the requested
     * slot and performs the configured action on the PC.
     */
    private void launchSlot(
            int slotNumber
    ) {

        // Perform the network request outside the UI thread.
        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            try {

                // Construct the launch API URL for the selected slot.
                String urlString =
                        "http://"
                                + serverAddress
                                + "/api/tiles/slot/"
                                + slotNumber
                                + "/launch";


                URL url =
                        new URL(
                                urlString
                        );


                // Open the HTTP connection.
                connection =
                        (HttpURLConnection)
                                url.openConnection();


                // Launch requests use the POST method.
                connection.setRequestMethod(
                        "POST"
                );


                // Maximum time allowed to establish the connection.
                connection.setConnectTimeout(
                        3000
                );


                // Maximum time allowed to receive the response.
                connection.setReadTimeout(
                        3000
                );


                // Send the request and retrieve the HTTP response code.
                int responseCode =
                        connection.getResponseCode();


                // Treat any response other than HTTP 200 as a failed launch.
                if (responseCode != 200) {

                    throw new RuntimeException(
                            "Launch failed: "
                                    + responseCode
                    );
                }


            } catch (Exception e) {

                // Log technical details for debugging.
                e.printStackTrace();


                // Inform the user that the selected slot could not be launched.
                runOnUiThread(
                        () -> Toast.makeText(
                                TileActivity.this,
                                "Failed to launch slot "
                                        + slotNumber,
                                Toast.LENGTH_SHORT
                        ).show()
                );


            } finally {

                // Release the HTTP connection.
                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    /*
     * ==========================================
     * ACTIVITY RESUMED
     * ==========================================
     *
     * Called when the Activity becomes active again.
     * Re-enable physical orientation detection.
     */
    @Override
    protected void onResume() {

        super.onResume();


        if (
                orientationEventListener != null
                        && orientationEventListener
                        .canDetectOrientation()
        ) {

            orientationEventListener.enable();
        }
    }


    /*
     * ==========================================
     * ACTIVITY PAUSED
     * ==========================================
     *
     * Called when the Activity is no longer in the foreground.
     * Disable orientation detection to avoid unnecessary
     * sensor processing while the screen is inactive.
     */
    @Override
    protected void onPause() {

        if (
                orientationEventListener != null
        ) {

            orientationEventListener.disable();
        }


        super.onPause();
    }


    /*
     * ==========================================
     * ACTIVITY DESTROYED
     * ==========================================
     *
     * Release resources when the Activity is destroyed.
     *
     * 1. Disable orientation detection.
     * 2. Disconnect the active SSE connection.
     * 3. Interrupt the SSE background thread.
     */
    @Override
    protected void onDestroy() {

        if (
                orientationEventListener != null
        ) {

            orientationEventListener.disable();
        }


        // Close the active SSE connection, if one exists.
        if (sseConnection != null) {

            sseConnection.disconnect();

            sseConnection =
                    null;
        }


        // Stop the background SSE listener thread.
        if (sseThread != null) {

            sseThread.interrupt();

            sseThread =
                    null;
        }


        super.onDestroy();
    }
}