
package com.remotepulse.server.config;

import com.remotepulse.server.network.service.ServerNetworkService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles startup tasks after the Remote Pulse application
 * has completed its initialization.
 *
 * Displays the configuration page URL and attempts to open
 * the page in a new Chrome window.
 */
@Component
public class ServerStartup {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerStartup.class);

    private final int serverPort;
    private final ServerNetworkService serverNetworkService;

    /**
     * Creates the startup component with its required dependencies.
     *
     * @param serverNetworkService service used to retrieve the local IP address
     * @param serverPort configured Spring Boot server port
     */
    public ServerStartup(
            ServerNetworkService serverNetworkService,
            @Value("${server.port:8080}") int serverPort) {

        this.serverNetworkService = serverNetworkService;
        this.serverPort = serverPort;
    }

    /**
     * Executes startup tasks after the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        String ipAddress = serverNetworkService.findLocalIpAddress();

        String url = "http://"
                + ipAddress
                + ":"
                + serverPort
                + "/index.html";

        // Display the startup banner directly in the terminal.
        System.out.println();

        System.out.println("========================================");
        System.out.println("          REMOTE PULSE SERVER");
        System.out.println("========================================");

        System.out.println();

        System.out.println("The configuration page is available at:");

        System.out.println();

        System.out.println(url);

        System.out.println();

        System.out.println("Opening configuration page in browser...");

        System.out.println();

        System.out.println("========================================");

        openBrowser(url);
    }

    /**
     * Attempts to open the configuration page in a new Chrome window.
     *
     * @param url configuration page URL
     */
    private void openBrowser(String url) {

        try {
            new ProcessBuilder(
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "--new-window",
                    "--window-size=516,914",
                    "--window-position=100,50",
                    url
            ).start();

        } catch (IOException exception) {

            logger.warn(
                    "Could not open Chrome automatically.",
                    exception
            );
        }
    }
}
