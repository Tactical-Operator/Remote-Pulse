package com.example.remotepulse;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    /*
     * Input field where the user enters the Spring Boot server address.
     *
     * Example:
     * 192.168.31.78:8080
     */
    private EditText serverAddressInput;

    /*
     * Button used to initiate the server connection.
     */
    private Button connectButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display so the layout can extend behind system bars.
        EdgeToEdge.enable(this);

        // Load the main screen layout.
        setContentView(R.layout.activity_main);


        /*
         * Handle Android system bar insets.
         *
         * This prevents the app's content from being obscured
         * by the status bar or navigation bar.
         */
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars()
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
         * Connect the Java variables to their corresponding
         * views defined in activity_main.xml.
         */
        serverAddressInput =
                findViewById(R.id.serverAddressInput);

        connectButton =
                findViewById(R.id.connectButton);


        /*
         * Handle the Connect button click.
         */
        connectButton.setOnClickListener(
                view -> {

                    // Read the server address entered by the user.
                    String serverAddress =
                            serverAddressInput
                                    .getText()
                                    .toString()
                                    .trim();


                    /*
                     * Validate the input.
                     * Do not attempt a connection if the field is empty.
                     */
                    if (serverAddress.isEmpty()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Enter server address",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    /*
                     * Perform network operations on a background thread.
                     *
                     * Android does not allow network requests on the
                     * main UI thread because they could block the interface.
                     */
                    new Thread(() -> {

                        try {

                            /*
                             * Construct the API URL using the server address.
                             *
                             * The /api/tiles endpoint is used to verify
                             * that the Remote Pulse backend is reachable.
                             */
                            String url =
                                    "http://"
                                            + serverAddress
                                            + "/api/tiles";

                            // Open an HTTP connection to the Spring Boot server.
                            java.net.HttpURLConnection connection =
                                    (java.net.HttpURLConnection)
                                            new java.net.URL(url)
                                                    .openConnection();

                            // Configure the HTTP GET request.
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

                            // Send the request and retrieve the HTTP response code.
                            int responseCode =
                                    connection.getResponseCode();

                            // Release the HTTP connection.
                            connection.disconnect();


                            /*
                             * Switch back to the main UI thread before
                             * displaying messages or opening another Activity.
                             */
                            runOnUiThread(() -> {

                                // HTTP 200 means the server responded successfully.
                                if (responseCode == 200) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Connected to Remote Pulse",
                                            Toast.LENGTH_SHORT
                                    ).show();


                                    /*
                                     * Open TileActivity after successfully
                                     * connecting to the backend.
                                     */
                                    Intent intent =
                                            new Intent(
                                                    MainActivity.this,
                                                    TileActivity.class
                                            );

                                    /*
                                     * Pass the server address to TileActivity.
                                     *
                                     * TileActivity uses this address for
                                     * fetching tiles, loading icons and
                                     * sending launch requests.
                                     */
                                    intent.putExtra(
                                            "SERVER_ADDRESS",
                                            serverAddress
                                    );

                                    startActivity(intent);
                                }
                                else {

                                    // Display the HTTP status returned by the server.
                                    Toast.makeText(
                                            MainActivity.this,
                                            "Server returned: "
                                                    + responseCode,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });


                        } catch (Exception e) {

                            /*
                             * Handle connection failures, such as:
                             * - Incorrect server address
                             * - Server not running
                             * - Network connectivity problems
                             * - Connection timeout
                             *
                             * Display the error message on the UI thread.
                             */
                            runOnUiThread(() ->

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Unable to connect",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }

                    }).start();
                }
        );
    }
}