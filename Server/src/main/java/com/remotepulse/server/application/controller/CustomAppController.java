
package com.remotepulse.server.application.controller;

import com.remotepulse.server.application.dto.CustomAppRequest;
import com.remotepulse.server.application.service.CustomAppSaveService;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.service.TileUpdateService;

import org.springframework.web.bind.annotation.*;

/**
 * Handles REST API requests for configuring custom applications.
 *
 * Delegates application validation and tile configuration saving
 * to CustomAppSaveService and notifies connected Android clients
 * when the tile configuration changes.
 */
@RestController
@RequestMapping("/api/custom-app")
public class CustomAppController {

    private final CustomAppSaveService customAppSaveService;
    private final TileUpdateService tileUpdateService;

    /**
     * Injects the services required to save custom applications
     * and notify connected Android clients.
     *
     * @param customAppSaveService handles custom application configuration
     * @param tileUpdateService sends tile-change notifications
     */
    public CustomAppController(
            CustomAppSaveService customAppSaveService,
            TileUpdateService tileUpdateService) {

        this.customAppSaveService = customAppSaveService;
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Saves a custom application configuration for a tile.
     *
     * POST /api/custom-app/save
     *
     * The request contains the slot number, application name,
     * and executable path.
     *
     * @param request the custom application configuration
     * @return the saved tile
     */
    @PostMapping("/save")
    public Tile saveCustomApp(
            @RequestBody CustomAppRequest request) {

        // Delegate validation and saving to the service layer.
        Tile savedTile = customAppSaveService.saveCustomApp(
                request.getSlotNumber(),
                request.getName(),
                request.getPath()
        );

        // Notify connected Android clients that the tile changed.
        tileUpdateService.tilesChanged();

        return savedTile;
    }
}
