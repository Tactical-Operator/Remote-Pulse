package com.remotepulse.server.tile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Service responsible for managing Server-Sent Events (SSE)
 * connections between the Remote Pulse server and its Android client.
 *
 * Maintains the currently active SSE connection and sends notifications
 * whenever tile configurations are changed.
 *
 * Current implementation supports one active SSE client at a time.
 *
 * Supported SSE events:
 * - connected: Confirms that the SSE connection has been established.
 * - tiles-changed: Notifies Android that tile configurations have changed.
 *
 * @author Remote Pulse
 */
@Service
public class TileUpdateService {

    // Logger used to record SSE connection events and errors.
    private static final Logger logger =
            LoggerFactory.getLogger(TileUpdateService.class);

    /**
     * Holds the currently active SSE connection.
     *
     * Only one emitter is maintained because the current Remote Pulse
     * implementation supports one active Android client at a time.
     *
     * Access to this field is synchronized through the service methods
     * to prevent concurrent connection-management operations.
     */
    private SseEmitter emitter;

    /**
     * Establishes an SSE subscription for the Android client.
     *
     * If another connection is already registered, it is completed
     * before the new connection becomes active.
     *
     * Registers callbacks to handle connection completion, timeout,
     * and errors. An initial event is sent to confirm the connection.
     *
     * @return the newly created SSE emitter
     */
    public synchronized SseEmitter subscribe() {

        // Complete the previous connection if one exists.
        if (emitter != null) {

            try {
                emitter.complete();

            } catch (Exception exception) {

                // The previous connection may already be closed.
                logger.debug(
                        "Unable to complete previous SSE connection.",
                        exception
                );
            }
        }

        // A timeout of 0L means no timeout is configured.
        SseEmitter newEmitter = new SseEmitter(0L);

        // Register the new connection as the active emitter.
        emitter = newEmitter;

        logger.info("Android SSE client connected.");

        /*
         * Remove the connection when it completes.
         *
         * clearEmitter() checks whether this emitter is still active.
         * This prevents an older connection's callback from clearing
         * a newer connection that has replaced it.
         */
        newEmitter.onCompletion(() -> {

            clearEmitter(newEmitter);

            logger.info("Android SSE connection completed.");
        });

        // Handle connection timeout and release the active reference.
        newEmitter.onTimeout(() -> {

            clearEmitter(newEmitter);

            logger.warn("Android SSE connection timed out.");
        });

        // Handle connection errors and release the active reference.
        newEmitter.onError(error -> {

            clearEmitter(newEmitter);

            logger.error(
                    "Android SSE connection error: {}",
                    error.getMessage(),
                    error
            );
        });

        /*
         * Send an initial event so the Android client can confirm
         * that the SSE connection has been established.
         */
        try {

            newEmitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data("CONNECTED")
            );

        } catch (IOException exception) {

            // Remove the connection if the initial event cannot be sent.
            clearEmitter(newEmitter);

            // Mark the emitter as failed.
            newEmitter.completeWithError(exception);

            logger.error(
                    "Failed to send initial SSE connection event.",
                    exception
            );
        }

        return newEmitter;
    }

    /**
     * Removes an emitter only if it is still the active connection.
     *
     * This check is important when an older SSE connection completes
     * after a newer connection has already replaced it.
     *
     * @param emitterToRemove emitter whose reference should be cleared
     */
    private synchronized void clearEmitter(SseEmitter emitterToRemove) {

        // Avoid clearing a newer connection accidentally.
        if (emitter == emitterToRemove) {

            emitter = null;

            logger.debug("Active SSE emitter reference cleared.");
        }
    }

    /**
     * Notifies the connected Android client that tile configurations
     * have changed.
     *
     * Sends an SSE event named "tiles-changed" with the payload
     * "TILES_CHANGED".
     *
     * If no client is connected, the method returns without sending
     * an event.
     *
     * If sending fails, the failed connection is removed and completed
     * with an error.
     */
    public synchronized void tilesChanged() {

        logger.debug("Tiles changed event triggered.");

        // No active connection means there is no client to notify.
        if (emitter == null) {

            logger.debug("No active SSE client to notify.");

            return;
        }

        try {

            // Send the tile-change notification to Android.
            emitter.send(
                    SseEmitter.event()
                            .name("tiles-changed")
                            .data("TILES_CHANGED")
            );

            logger.info("Tiles-changed event sent to Android.");

        } catch (IOException exception) {

            // Store the failed emitter before clearing the active reference.
            SseEmitter failedEmitter = emitter;

            emitter = null;

            logger.error(
                    "Failed to send tiles-changed SSE event.",
                    exception
            );

            // Complete the failed connection with the original error.
            try {

                failedEmitter.completeWithError(exception);

            } catch (Exception completionException) {

                logger.debug(
                        "Unable to complete failed SSE connection.",
                        completionException
                );
            }
        }
    }
}