package com.remotepulse.server.tile.controller;

import java.util.List;

import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.service.TileLaunchService;
import com.remotepulse.server.tile.service.TileService;
import com.remotepulse.server.tile.service.TileUpdateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for handling Remote Pulse tile operations.
 *
 * Exposes API endpoints for retrieving, updating, launching, and clearing
 * tile configurations.
 *
 * Delegates business logic to the appropriate service classes and
 * notifies connected Android clients whenever tile configurations change.
 *
 * Base URL: /api/tiles
 *
 * @author Remote Pulse
 */
@RestController
@RequestMapping("/api/tiles")
public class TileController {

    // Logger used to record tile-related API operations.
    private static final Logger logger =
            LoggerFactory.getLogger(TileController.class);

    // Service responsible for tile database operations.
    private final TileService tileService;

    // Service responsible for launching configured tiles.
    private final TileLaunchService tileLaunchService;

    // Service responsible for notifying Android clients about tile changes.
    private final TileUpdateService tileUpdateService;

    /**
     * Constructs the TileController and injects its required services.
     *
     * Spring automatically provides these service instances through
     * constructor-based dependency injection.
     *
     * @param tileService handles tile retrieval, updating, and clearing
     * @param tileLaunchService executes configured tile actions
     * @param tileUpdateService manages Android tile-change notifications
     */
    public TileController(
            TileService tileService,
            TileLaunchService tileLaunchService,
            TileUpdateService tileUpdateService) {

        this.tileService = tileService;
        this.tileLaunchService = tileLaunchService;
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Retrieves all tile records stored in the database.
     *
     * GET /api/tiles
     *
     * Used by the frontend and Android client to obtain tile configurations.
     *
     * @return HTTP 200 containing the list of tiles
     */
    @GetMapping
    public ResponseEntity<List<Tile>> getAllTiles() {

        List<Tile> tiles = tileService.getAllTiles();

        return ResponseEntity.ok(tiles);
    }

    /**
     * Retrieves a tile using its unique database ID.
     *
     * GET /api/tiles/{id}
     *
     * @param id unique database ID of the requested tile
     * @return HTTP 200 containing the requested tile
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tile> getTile(@PathVariable Long id) {

        Tile tile = tileService.getTile(id);

        return ResponseEntity.ok(tile);
    }

    /**
     * Updates the configuration of an existing tile.
     *
     * PUT /api/tiles/{id}
     *
     * After a successful update, connected Android clients are notified
     * through the TileUpdateService.
     *
     * @param id unique database ID of the tile to update
     * @param tile updated tile data supplied in the request body
     * @return HTTP 200 containing the updated tile
     */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Tile> updateTile(
            @PathVariable Long id,
            @RequestBody Tile tile) {

        // Update the tile configuration in the database.
        Tile updatedTile = tileService.updateTile(id, tile);

        // Notify Android that tile configurations have changed.
        tileUpdateService.tilesChanged();

        logger.info("Updated tile with ID: {}", id);

        return ResponseEntity.ok(updatedTile);
    }

    /**
     * Launches a tile using its unique database ID.
     *
     * POST /api/tiles/{id}/launch
     *
     * Retrieves the tile and delegates its execution to TileLaunchService.
     *
     * @param id unique database ID of the tile to launch
     * @return HTTP 200 containing a launch success message
     */
    @PostMapping("/{id}/launch")
    public ResponseEntity<String> launchTile(@PathVariable Long id) {

        // Retrieve the requested tile.
        Tile tile = tileService.getTile(id);

        // Delegate the launch operation to the launch service.
        tileLaunchService.launch(tile);

        String response =
                "Tile '" + tile.getName() + "' Launched Successfully";

        logger.info("Launched tile with ID: {}", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Launches a tile using its Remote Pulse layout slot number.
     *
     * POST /api/tiles/slot/{slotNumber}/launch
     *
     * This endpoint is primarily used by the Android application,
     * which identifies tiles using their slot numbers.
     *
     * @param slotNumber position of the tile in the Remote Pulse layout
     * @return HTTP 200 containing the slot number and tile name
     */
    @PostMapping("/slot/{slotNumber}/launch")
    public ResponseEntity<String> launchTileBySlot(
            @PathVariable Integer slotNumber) {

        // Retrieve the tile associated with the requested slot.
        Tile tile = tileService.getTileBySlotNumber(slotNumber);

        // Execute the action configured for this tile.
        tileLaunchService.launch(tile);

        String response =
                "Slot "
                        + slotNumber
                        + " ('"
                        + tile.getName()
                        + "') launched successfully";

        logger.info("Launched tile from slot: {}", slotNumber);

        return ResponseEntity.ok(response);
    }

    /**
     * Clears a tile's configuration while preserving its database record
     * and layout slot number.
     *
     * DELETE /api/tiles/slot/{slotNumber}/configuration
     *
     * After clearing the configuration, connected Android clients
     * are notified about the change.
     *
     * @param slotNumber layout slot whose configuration should be cleared
     * @return HTTP 200 containing the cleared tile
     */
    @DeleteMapping("/slot/{slotNumber}/configuration")
    public ResponseEntity<Tile> clearTile(
            @PathVariable Integer slotNumber) {

        // Clear the tile configuration while preserving its slot.
        Tile clearedTile = tileService.clearTile(slotNumber);

        // Notify Android that the tile configuration has changed.
        tileUpdateService.tilesChanged();

        logger.info("Cleared tile configuration for slot: {}", slotNumber);

        return ResponseEntity.ok(clearedTile);
    }
}