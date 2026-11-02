
package com.remotepulse.server.application.service;

import com.remotepulse.server.common.exception.TileNotFoundException;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles saving custom executable application configurations.
 *
 * Validates the executable path, updates the tile details,
 * extracts the executable's icon using a PowerShell script,
 * and saves the updated tile configuration.
 */
@Service
public class CustomAppSaveService {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomAppSaveService.class);

    private final TileRepository tileRepository;

    /**
     * Creates the service with the required tile repository.
     *
     * @param tileRepository repository used to retrieve and save tiles
     */
    public CustomAppSaveService(TileRepository tileRepository) {
        this.tileRepository = tileRepository;
    }

    /**
     * Configures a tile to launch a custom Windows executable.
     *
     * Validates the executable, finds the requested tile slot,
     * extracts the executable icon, and saves the configuration.
     *
     * @param slotNumber the Remote Pulse slot number
     * @param name the display name of the application
     * @param path the full path to the executable
     * @return the saved tile configuration
     * @throws IllegalArgumentException if the executable path is invalid
     * @throws TileNotFoundException if the requested tile slot does not exist
     * @throws IllegalStateException if icon extraction fails
     */
    public Tile saveCustomApp(
            Integer slotNumber,
            String name,
            String path) {

        // Ensure an executable path has been provided.
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "EXE path cannot be empty.");
        }

        // Allow only paths ending with the .exe extension.
        if (!path.toLowerCase().endsWith(".exe")) {
            throw new IllegalArgumentException(
                    "Only .exe files are allowed.");
        }

        // Verify that the executable file exists and is a regular file.
        File exeFile = new File(path);

        if (!exeFile.isFile()) {
            throw new IllegalArgumentException(
                    "EXE file does not exist: " + path);
        }

        // Find the existing tile using its slot number.
        Tile tile = tileRepository
                .findBySlotNumber(slotNumber)
                .orElseThrow(() ->
                        new TileNotFoundException(
                                "Tile slot not found: " + slotNumber));

        // Update the tile with the custom application details.
        tile.setName(name);
        tile.setType("CUSTOM_APP");
        tile.setTarget(path);

        // Create the directory used to store extracted icons.
        Path iconDirectory = Path.of("data", "icons")
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(iconDirectory);

            // Build the destination path for this tile's icon.
            String iconFileName = "slot-" + slotNumber + ".png";

            Path outputPath = iconDirectory.resolve(iconFileName);

            // Extract the executable's icon using PowerShell.
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    "extract-exe-icon.ps1",
                    path,
                    outputPath.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            // Capture the script output for error reporting.
            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Wait for extraction to finish and check its exit code.
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "EXE icon extraction failed:\n" + output);
            }

            // Verify that the extracted icon is a regular file.
            if (!Files.isRegularFile(outputPath)) {
                throw new IllegalStateException(
                        "Icon extraction completed, "
                                + "but icon file was not created.");
            }

            // Store the extracted icon filename in the tile.
            tile.setIcon(iconFileName);

            // Log the location of the extracted icon.
            logger.info(
                    "Custom App icon created: {}",
                    outputPath.toAbsolutePath()
            );

        } catch (IOException exception) {

            // Handle file-system and process-starting failures.
            throw new IllegalStateException(
                    "Failed to extract Custom App icon",
                    exception
            );

        } catch (InterruptedException exception) {

            // Restore the interrupted status of the current thread.
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Custom App icon extraction was interrupted",
                    exception
            );
        }

        // Save the updated tile configuration to the database.
        return tileRepository.save(tile);
    }
}
