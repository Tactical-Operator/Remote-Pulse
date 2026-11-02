package com.remotepulse.server.system.controller;

import com.remotepulse.server.system.service.SystemButtonSaveService;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.service.TileUpdateService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for configuring system-button tiles.
 *
 * <p>Receives the selected tile slot and system action,
 * delegates the save operation to the service layer,
 * and notifies connected Android clients of the change.</p>
 */
@RestController
@RequestMapping("/api/system-button")
public class SystemButtonController {

    private final SystemButtonSaveService systemButtonSaveService;
    private final TileUpdateService tileUpdateService;

    /**
     * Creates the controller with its required services.
     *
     * @param systemButtonSaveService service for saving system-button configurations
     * @param tileUpdateService service for notifying connected clients
     */
    public SystemButtonController(
            SystemButtonSaveService systemButtonSaveService,
            TileUpdateService tileUpdateService) {

        this.systemButtonSaveService = systemButtonSaveService;
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Saves a system-button configuration for the specified tile slot.
     *
     * @param request request containing the slot number and system action
     * @return the saved tile configuration
     */
    @PostMapping("/save")
    public Tile saveSystemButton(
            @RequestBody Map<String, Object> request) {

        // Extract and convert the tile slot number from the request.
        Integer slotNumber = Integer.valueOf(
                request.get("slotNumber").toString()
        );

        // Extract the selected system action.
        String action = request.get("action").toString();

        // Save the system-button configuration through the service layer.
        Tile savedTile = systemButtonSaveService.saveSystemButton(
                slotNumber,
                action
        );

        // Notify connected Android clients that the tile configuration changed.
        tileUpdateService.tilesChanged();

        return savedTile;
    }
}
