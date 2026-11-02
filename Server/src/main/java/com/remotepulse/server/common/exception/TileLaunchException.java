package com.remotepulse.server.common.exception;

/**
 * Exception thrown when a Remote Pulse tile cannot be launched.
 *
 * This exception provides a specific error type for failures
 * occurring during application, website, or system-button execution.
 */
public class TileLaunchException extends RuntimeException {

    /**
     * Creates a launch exception with a descriptive message.
     *
     * @param message description of the launch failure
     */
    public TileLaunchException(String message) {
        super(message);
    }

    /**
     * Creates a launch exception with a message and its original cause.
     *
     * @param message description of the launch failure
     * @param cause original exception that caused the failure
     */
    public TileLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}