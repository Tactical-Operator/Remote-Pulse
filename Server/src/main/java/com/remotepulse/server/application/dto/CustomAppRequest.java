
package com.remotepulse.server.application.dto;

/**
 * Represents the request data received when configuring
 * a custom application tile.
 *
 * Used by CustomAppController to receive the selected tile slot,
 * application display name, and executable file path from
 * the frontend.
 */
public class CustomAppRequest {

    /**
     * Slot number where the custom application will be configured.
     */
    private Integer slotNumber;

    /**
     * Display name to show on the tile in the Remote Pulse interface.
     */
    private String name;

    /**
     * Full filesystem path to the custom application's executable.
     */
    private String path;

    /**
     * No-argument constructor used during request deserialization.
     */
    public CustomAppRequest() {
    }

    /**
     * Returns the selected tile slot number.
     *
     * @return the tile slot number
     */
    public Integer getSlotNumber() {
        return slotNumber;
    }

    /**
     * Sets the tile slot number received in the request.
     *
     * @param slotNumber the tile slot number
     */
    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * Returns the custom application's display name.
     *
     * @return the application display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the custom application's display name.
     *
     * @param name the application display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the executable's filesystem path.
     *
     * @return the executable file path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the executable's filesystem path.
     *
     * @param path the executable file path
     */
    public void setPath(String path) {
        this.path = path;
    }
}
