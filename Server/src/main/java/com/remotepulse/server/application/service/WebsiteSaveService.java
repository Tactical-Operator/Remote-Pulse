package com.remotepulse.server.application.service;

import com.remotepulse.server.common.exception.TileNotFoundException;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles saving website configurations for Remote Pulse tiles.
 *
 * <p>Validates the website URL, updates the tile details,
 * attempts to extract the website icon, and saves the tile.</p>
 */
@Service
public class WebsiteSaveService {

    private static final Logger logger =
            LoggerFactory.getLogger(WebsiteSaveService.class);

    private final TileRepository tileRepository;

    /**
     * Creates the service with its required repository.
     *
     * @param tileRepository repository for accessing and saving tile records
     */
    public WebsiteSaveService(TileRepository tileRepository) {
        this.tileRepository = tileRepository;
    }

    /**
     * Configures a tile to open the specified website.
     *
     * @param slotNumber the Remote Pulse slot number
     * @param name the display name of the website
     * @param url the website URL
     * @return the saved tile configuration
     * @throws IllegalArgumentException if the website URL is empty or
     *                                   does not start with HTTP or HTTPS
     * @throws TileNotFoundException if the specified tile slot does not exist
     */
    public Tile saveWebsite(
            Integer slotNumber,
            String name,
            String url) {

        // Ensure a website URL has been provided.
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "Website URL cannot be empty."
            );
        }

        // Accept only URLs beginning with HTTP or HTTPS.
        if (!url.startsWith("http://") &&
                !url.startsWith("https://")) {

            throw new IllegalArgumentException(
                    "Website URL must start with http:// or https://"
            );
        }

        // Find the existing tile using its slot number.
        Tile tile = tileRepository
                .findBySlotNumber(slotNumber)
                .orElseThrow(() -> new TileNotFoundException(
                        "Tile slot not found: " + slotNumber
                ));

        // Update the tile with the website configuration.
        tile.setName(name);
        tile.setType("WEBSITE");
        tile.setTarget(url);

        try {
            // Create the directory used to store custom tile icons.
            Path iconDirectory = Path.of("data", "icons");

            Files.createDirectories(iconDirectory);

            // Build the output path for this slot's icon.
            String outputPath = Path.of(
                    "data",
                    "icons",
                    "slot-" + slotNumber + ".png"
            )
                    .toAbsolutePath()
                    .normalize()
                    .toString();

            // Run the PowerShell script that extracts the website icon.
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    "extract-web-icon.ps1",
                    url,
                    outputPath
            )
                    .inheritIO()
                    .start();

            // Wait until the icon extraction process finishes.
            int exitCode = process.waitFor();

            // Treat an unsuccessful extraction as an error.
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Website icon extraction failed. Exit code: " + exitCode
                );
            }

            // Store the extracted icon filename in the tile.
            tile.setIcon("slot-" + slotNumber + ".png");

        } catch (InterruptedException e) {

            // Restore the interrupted status of the current thread.
            Thread.currentThread().interrupt();

            logger.warn(
                    "Website icon extraction was interrupted for: {}",
                    name,
                    e
            );

        } catch (IOException | RuntimeException e) {

            // Icon extraction errors do not prevent saving the tile.
            logger.warn(
                    "Could not extract website icon for: {}",
                    name,
                    e
            );
        }

        // Save the updated tile configuration to the database.
        return tileRepository.save(tile);
    }
}