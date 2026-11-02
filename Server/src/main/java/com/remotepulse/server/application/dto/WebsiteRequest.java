package com.remotepulse.server.application.dto;

/**
 * Represents the request data received when configuring a website tile.
 *
 * <p>Used by the controller to receive the selected tile slot, display name,
 * and website URL from the frontend before saving the tile configuration.</p>
 */
public class WebsiteRequest {

    /**
     * Slot number where the website will be configured.
     */
    private Integer slotNumber;

    /**
     * Display name shown on the Remote Pulse tile.
     */
    private String name;

    /**
     * URL of the website that the tile will open.
     */
    private String url;

    /**
     * No-argument constructor used to create the request object.
     */
    public WebsiteRequest() {
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
     * Returns the website's display name.
     *
     * @return the website's display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the website's display name.
     *
     * @param name the website's display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the website URL.
     *
     * @return the website URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the website URL.
     *
     * @param url the website URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
}