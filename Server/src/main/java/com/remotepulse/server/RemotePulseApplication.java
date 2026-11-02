
package com.remotepulse.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Remote Pulse Spring Boot application.
 *
 * Starts the embedded web server and initializes the Spring
 * application context.
 *
 * Component scanning begins from com.remotepulse.server and
 * includes its subpackages.
 */
@SpringBootApplication
public class RemotePulseApplication {

    /**
     * Starts the Remote Pulse application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(RemotePulseApplication.class, args);
    }
}
