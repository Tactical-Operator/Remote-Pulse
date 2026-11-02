
package com.remotepulse.server.tile.service;

import com.remotepulse.server.common.exception.TileNotFoundException;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service layer responsible for managing tile configurations
 * in the Remote Pulse application.
 *
 * This class acts as an intermediary between the TileController
 * and TileRepository. It contains the business logic required
 * to retrieve, save, update, and clear tile configurations.
 *
 * Responsibilities:
 * - Retrieve all tiles or an individual tile.
 * - Find tiles using their database ID or slot number.
 * - Save new tile configurations.
 * - Update the configurable properties of existing tiles.
 * - Clear tile configurations without deleting their slots.
 * - Delete associated uploaded icon files when a tile is cleared.
 *
 * Database operations are performed through TileRepository.
 *
 * @author Remote Pulse
 */
@Service
public class TileService {

    // Logger used to record important events and errors.
    private static final Logger logger =
            LoggerFactory.getLogger(TileService.class);

    // Repository responsible for database operations on Tile entities.
    private final TileRepository tileRepository;

    /**
     * Constructs the TileService and injects its required repository.
     *
     * Spring automatically provides the TileRepository implementation
     * through constructor-based dependency injection.
     *
     * @param tileRepository repository used to access tile records
     */
    public TileService(TileRepository tileRepository) {
        this.tileRepository = tileRepository;
    }

    /**
     * Retrieves all tile records stored in the database.
     *
     * This method is used when the frontend or Android client
     * needs to retrieve the complete tile configuration.
     *
     * The transaction is marked read-only because this operation
     * does not modify any database records.
     *
     * @return a list containing all stored tiles
     */
    @Transactional(readOnly = true)
    public List<Tile> getAllTiles() {
        return tileRepository.findAll();
    }

    /**
     * Retrieves a tile using its unique database ID.
     *
     * The repository searches for the requested ID. If no matching
     * tile exists, a TileNotFoundException is thrown.
     *
     * @param id unique database ID of the requested tile
     * @return the matching Tile entity
     * @throws TileNotFoundException if the tile does not exist
     */
    @Transactional(readOnly = true)
    public Tile getTile(Long id) {

        return tileRepository.findById(id)
                .orElseThrow(() ->
                        new TileNotFoundException(
                                "Tile not found: " + id));
    }

    /**
     * Retrieves a tile using its position in the Remote Pulse layout.
     *
     * Slot numbers represent the positions of tiles in the interface.
     * This lookup is particularly useful for the Android client,
     * which can identify a tile by its slot number.
     *
     * @param slotNumber layout position of the requested tile
     * @return the matching Tile entity
     * @throws TileNotFoundException if the slot does not exist
     */
    @Transactional(readOnly = true)
    public Tile getTileBySlotNumber(Integer slotNumber) {

        return tileRepository.findBySlotNumber(slotNumber)
                .orElseThrow(() ->
                        new TileNotFoundException(
                                "Tile slot not found: " + slotNumber));
    }

    /**
     * Saves a tile entity in the database.
     *
     * Spring Data JPA determines whether the operation should
     * insert a new record or update an existing record.
     *
     * @param tile tile entity to be saved
     * @return the saved Tile entity
     */
    @Transactional
    public Tile saveTile(Tile tile) {
        return tileRepository.save(tile);
    }

    /**
     * Updates the configurable properties of an existing tile.
     *
     * The existing database record is retrieved first. Its
     * configurable properties are then replaced with the values
     * supplied in the updatedTile object.
     *
     * The database ID and slot number are intentionally preserved
     * so that the tile retains its original layout position.
     *
     * @param id unique database ID of the tile to update
     * @param updatedTile object containing the new tile configuration
     * @return the updated and saved Tile entity
     * @throws TileNotFoundException if the requested tile does not exist
     */
    @Transactional
    public Tile updateTile(Long id, Tile updatedTile) {

        // Retrieve the existing database record.
        Tile existingTile = getTile(id);

        // Update only the configurable properties.
        existingTile.setName(updatedTile.getName());
        existingTile.setType(updatedTile.getType());
        existingTile.setTarget(updatedTile.getTarget());
        existingTile.setIcon(updatedTile.getIcon());

        // Persist the updated entity.
        return tileRepository.save(existingTile);
    }

    /**
     * Clears the configuration of a tile without deleting its database
     * record or changing its slot number.
     *
     * The associated uploaded icon file is deleted if a valid icon
     * filename is configured and the file exists.
     *
     * Afterward, the name, type, target, and icon properties are
     * set to null, leaving the slot available for future configuration.
     *
     * @param slotNumber layout position of the tile to clear
     * @return the tile entity with its configuration cleared
     * @throws TileNotFoundException if the requested slot does not exist
     */
    @Transactional
    public Tile clearTile(Integer slotNumber) {

        // Find the tile using its layout slot number.
        Tile tile = getTileBySlotNumber(slotNumber);

        // Delete the associated uploaded icon, if configured.
        deleteIconFile(tile.getIcon());

        // Clear configurable properties while preserving the slot.
        tile.setName(null);
        tile.setType(null);
        tile.setTarget(null);
        tile.setIcon(null);

        // Save the cleared configuration.
        return tileRepository.save(tile);
    }

    /**
     * Deletes an uploaded tile icon from the application's data/icons
     * directory.
     *
     * The supplied value must be a filename, not an arbitrary file path.
     * The normalized resolved path is checked to ensure it remains
     * inside the permitted icon directory.
     *
     * Missing files are ignored. File deletion failures are logged
     * rather than silently ignored.
     *
     * Note: File-system operations are not part of the database
     * transaction. A successful file deletion cannot be rolled back
     * automatically if a subsequent database operation fails.
     *
     * @param iconName filename of the icon to delete
     */
    private void deleteIconFile(String iconName) {

        // Nothing to delete if the icon is not configured.
        if (iconName == null || iconName.isBlank()) {
            return;
        }

        // Reject absolute paths and values containing directory paths.
        Path fileName = Paths.get(iconName);

        if (fileName.isAbsolute()
                || fileName.getNameCount() != 1
                || !fileName.getFileName().toString().equals(iconName)) {

            logger.warn("Invalid icon filename: {}", iconName);
            return;
        }

        // Resolve the application's icon storage directory.
        Path iconDirectory = Paths.get("data", "icons")
                .toAbsolutePath()
                .normalize();

        // Build and normalize the complete icon file path.
        Path iconPath = iconDirectory.resolve(fileName).normalize();

        // Ensure the resolved path remains inside the icon directory.
        if (!iconPath.startsWith(iconDirectory)) {
            logger.warn("Icon path outside allowed directory: {}", iconName);
            return;
        }

        try {

            // Delete the file only if it exists.
            boolean deleted = Files.deleteIfExists(iconPath);

            if (deleted) {
                logger.info("Deleted tile icon: {}", iconName);
            }

        } catch (IOException exception) {

            // Record file deletion failures for troubleshooting.
            logger.error(
                    "Failed to delete tile icon: {}",
                    iconName,
                    exception);
        }
    }
}
