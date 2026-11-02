package com.remotepulse.server.tile.controller;

import com.remotepulse.server.tile.service.TileUpdateService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller responsible for exposing the Server-Sent Events (SSE)
 * endpoint used by the Remote Pulse Android client.
 *
 * Allows the Android client to establish a persistent HTTP connection
 * and receive notifications whenever tile configurations change.
 *
 * Endpoint:
 * GET /api/tiles/events
 *
 * The controller delegates SSE connection management and event delivery
 * to TileUpdateService.
 *
 * @author Remote Pulse
 */
@RestController
@RequestMapping("/api/tiles")
public class TileUpdateController {

    // Service responsible for managing SSE connections and notifications.
    private final TileUpdateService tileUpdateService;

    /**
     * Constructs the TileUpdateController and injects its required service.
     *
     * Spring automatically provides the TileUpdateService implementation
     * through constructor-based dependency injection.
     *
     * @param tileUpdateService service that manages SSE subscriptions
     *                          and tile-change notifications
     */
    public TileUpdateController(TileUpdateService tileUpdateService) {
        this.tileUpdateService = tileUpdateService;
    }

    /**
     * Establishes an SSE subscription for the Android client.
     *
     * GET /api/tiles/events
     *
     * Returns an SseEmitter that keeps the HTTP response open.
     * The server can then send events through this connection
     * whenever tile configurations are modified.
     *
     * The response uses the text/event-stream media type required
     * by Server-Sent Events.
     *
     * @return the SseEmitter associated with the Android client connection
     */
    @GetMapping(
            value = "/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribeToTileUpdates() {

        // Delegate connection creation to the service layer.
        return tileUpdateService.subscribe();
    }
}