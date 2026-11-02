
package com.remotepulse.server.application.service;

import com.remotepulse.server.common.exception.TileNotFoundException;
import com.remotepulse.server.icon.service.IconExtractionService;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

// import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles saving installed application configurations for Remote Pulse tiles.
 *
 * Updates the tile details, attempts to extract the application's icon,
 * and saves the configuration to the database.
 *
 * Icon extraction is optional. If extraction fails, the application
 * configuration is still saved without an updated icon reference.
 */
@Service
public class InstalledAppSaveService {

    private static final Logger logger =
            LoggerFactory.getLogger(InstalledAppSaveService.class);

    private final TileRepository tileRepository;
    private final IconExtractionService iconExtractionService;

    /**
     * Creates the service with its required dependencies.
     *
     * @param tileRepository provides database access to tile configurations
     * @param iconExtractionService extracts icons from installed applications
     */
    public InstalledAppSaveService(
            TileRepository tileRepository,
            IconExtractionService iconExtractionService) {

        this.tileRepository = tileRepository;
        this.iconExtractionService = iconExtractionService;
    }

    /**
     * Configures an existing tile to launch an installed Windows application.
     *
     * Finds the tile using its slot number, updates its application details,
     * attempts to extract its icon, and saves the updated tile.
     *
     * If icon extraction fails, the tile is still saved.
     *
     * @param slotNumber the Remote Pulse slot number
     * @param name the application's display name
     * @param appId the identifier used to launch the application
     * @return the saved tile configuration
     * @throws TileNotFoundException if the requested tile slot does not exist
     */
    public Tile saveInstalledApp(
            Integer slotNumber,
            String name,
            String appId) {

        // Find the existing tile using its slot number.
        Tile tile = tileRepository
                .findBySlotNumber(slotNumber)
                .orElseThrow(() ->
                        new TileNotFoundException(
                                "Tile slot not found: " + slotNumber
                        )
                );

        // Store the installed application's configuration.
        tile.setName(name);
        tile.setType("INSTALLED_APP");
        tile.setTarget(appId);

        try {
            // Resolve the directory used to store uploaded and extracted icons.
            Path iconDirectory = Path.of("data", "icons")
                    .toAbsolutePath()
                    .normalize();

            // Create the icon directory if it does not already exist.
            Files.createDirectories(iconDirectory);

            // Build the output path for this slot's application icon.
            String iconFileName = "slot-" + slotNumber + ".png";

            Path iconPath = iconDirectory.resolve(iconFileName);

            // Extract the application's icon and save it to the output path.
            iconExtractionService.extractInstalledAppIcon(
                    appId,
                    iconPath.toString()
            );

            // Store the extracted icon filename in the tile.
            tile.setIcon(iconFileName);

            logger.info(
                    "Extracted icon for installed application '{}' in slot {}.",
                    name,
                    slotNumber
            );

        } catch (Exception exception) {

            // Continue saving the tile even if icon extraction fails.
            logger.warn(
                    "Could not extract icon for installed application '{}'. " +
                    "The tile will be saved without an updated icon.",
                    name,
                    exception
            );
        }

        // Save the updated tile configuration to the database.
        return tileRepository.save(tile);
    }
}
