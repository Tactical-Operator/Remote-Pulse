
package com.remotepulse.server.application.controller;

import com.remotepulse.server.application.dto.InstalledApp;
import com.remotepulse.server.application.dto.InstalledAppRequest;
import com.remotepulse.server.application.service.InstalledAppSaveService;
import com.remotepulse.server.application.service.InstalledAppService;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.service.TileUpdateService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles REST API requests for discovering and configuring
 * installed Windows applications in Remote Pulse.
 *
 * Delegates application discovery and tile configuration saving
 * to the appropriate service classes.
 */
@RestController
public class InstalledAppController {

    private final InstalledAppService installedAppService;
    private final InstalledAppSaveService installedAppSaveService;
    private final TileUpdateService tileUpdateService;

    /**
     * Injects the services required to discover applications,
     * save tile configurations, and notify Android clients.
     *
     * @param installedAppService discovers installed applications
     * @param installedAppSaveService saves selected applications
     * @param tileUpdateService sends tile-change notifications
     */
    public InstalledAppController(
            InstalledAppService installedAppService,
            InstalledAppSaveService installedAppSaveService,
            TileUpdateService tileUpdateService) {

        this.installedAppService = installedAppService;
        this.installedAppSaveService = installedAppSaveService;
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Retrieves the installed applications available to Remote Pulse.
     *
     * GET /api/installed-apps
     *
     * @return list of discovered applications
     */
    @GetMapping("/api/installed-apps")
    public List<InstalledApp> getInstalledApps() {
        return installedAppService.getInstalledApps();
    }

    /**
     * Saves a selected installed application to a tile.
     *
     * POST /api/installed-apps/select
     *
     * The request supplies the slot number, application name,
     * and application ID.
     *
     * @param request details of the selected application
     * @return the saved tile
     */
    @PostMapping("/api/installed-apps/select")
    public Tile selectInstalledApp(
            @RequestBody InstalledAppRequest request) {

        // Delegate tile configuration saving to the service layer.
        Tile savedTile = installedAppSaveService.saveInstalledApp(
                request.getSlotNumber(),
                request.getName(),
                request.getAppId()
        );

        // Notify Android that a tile configuration has changed.
        tileUpdateService.tilesChanged();

        return savedTile;
    }
}
