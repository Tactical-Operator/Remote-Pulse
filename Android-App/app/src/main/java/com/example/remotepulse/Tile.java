package com.example.remotepulse;

/*
 * Data model representing a single Remote Pulse tile.
 *
 * This class stores the tile information received from
 * the Spring Boot backend through the /api/tiles endpoint.
 *
 * Gson uses this class to convert JSON responses into
 * Java Tile objects.
 */
public class Tile {

    // Unique database ID assigned to the tile by the backend.
    private Long id;

    // Position of the tile in the Remote Pulse grid (1–18).
    private Integer slotNumber;

    // Display name of the tile.
    private String name;

    // Defines the tile category, such as CUSTOM_APP, WEBSITE,
    // INSTALLED_APP, SYSTEM_BUTTON or KEYBOARD_SHORTCUT.
    private String type;

    // Target associated with the tile.
    // Depending on its type, this may contain an application path,
    // website URL or another action identifier.
    private String target;

    // Icon filename or path used to display the tile's icon.
    private String icon;


    /*
     * No-argument constructor.
     *
     * Required by Gson when creating Tile objects from JSON.
     */
    public Tile() {
    }


    /*
     * Getter methods
     *
     * These methods allow other classes, such as TileActivity,
     * to access the tile's private fields.
     */

    public Long getId() {
        return id;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public String getIcon() {
        return icon;
    }


    /*
     * Setter methods
     *
     * These methods allow Gson or other classes to assign
     * values to the tile's private fields.
     */

    public void setId(Long id) {
        this.id = id;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}