
package com.remotepulse.server.icon.controller;

import com.remotepulse.server.icon.dto.CustomIconRequest;
import com.remotepulse.server.icon.service.CustomIconService;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.service.TileUpdateService;

import org.springframework.web.bind.annotation.*;

/**
 * REST controller for saving custom tile icons.
 *
 * Receives custom icon data, delegates image processing and
 * storage to CustomIconService, and notifies connected clients
 * when the tile configuration changes.
 */
@RestController
@RequestMapping("/api/custom-icon")
public class CustomIconController {

    private final CustomIconService customIconService;
    private final TileUpdateService tileUpdateService;

    /**
     * Creates the controller with its required services.
     *
     * @param customIconService service for saving custom tile icons
     * @param tileUpdateService service for notifying connected clients
     */
    public CustomIconController(
            CustomIconService customIconService,
            TileUpdateService tileUpdateService) {

        this.customIconService = customIconService;
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Saves a custom icon for the specified tile slot.
     *
     * POST /api/custom-icon/save
     *
     * @param request contains the tile slot number and Base64-encoded image
     * @return the updated tile configuration
     */
    @PostMapping("/save")
    public Tile saveCustomIcon(
            @RequestBody CustomIconRequest request) {

        // Save the custom icon through the service layer.
        Tile savedTile = customIconService.saveCustomIcon(
                request.getSlotNumber(),
                request.getImageBase64()
        );

        // Notify connected Android clients that the tile configuration changed.
        tileUpdateService.tilesChanged();

        return savedTile;
    }
}
