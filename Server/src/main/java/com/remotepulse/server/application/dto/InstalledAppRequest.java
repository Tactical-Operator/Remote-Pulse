
package com.remotepulse.server.application.dto;

/**
 * Represents the request data received when configuring an
 * installed application as a Remote Pulse tile.
 *
 * Used by the controller to receive the selected tile slot,
 * application display name, and application ID from the frontend.
 */
public class InstalledAppRequest {

    /**
     * Slot number where the application will be configured.
     */
    private Integer slotNumber;

    /**
     * Application name displayed on the Remote Pulse tile.
     */
    private String name;

    /**
     * Unique identifier used to launch the installed application.
     */
    private String appId;

    /**
     * No-argument constructor used to create the request object
     * during JSON deserialization.
     */
    public InstalledAppRequest() {
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
     * Sets the tile slot number received from the frontend.
     *
     * @param slotNumber the tile slot number
     */
    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * Returns the application's display name.
     *
     * @return the application display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the application's display name.
     *
     * @param name the application display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the application's unique identifier.
     *
     * @return the application ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Sets the application's unique identifier.
     *
     * @param appId the application's unique identifier
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }
}
