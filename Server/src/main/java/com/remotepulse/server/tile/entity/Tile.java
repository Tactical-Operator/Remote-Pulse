
package com.remotepulse.server.tile.entity;

import jakarta.persistence.*;

/**
 * Represents a tile in the Remote Pulse application.
 *
 * This entity maps to the "tiles" table in the database and stores
 * each tile's position, display name, type, launch target, and icon.
 */
@Entity
@Table(name = "tiles")
public class Tile {

    // Unique database ID generated automatically.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Position of the tile in the Remote Pulse layout.
    @Column(name = "slot_number", nullable = false, unique = true)
    private Integer slotNumber;

    // Display name shown to the user.
    @Column
    private String name;

    // Identifies the tile type.
    @Column
    private String type;

    // Stores the executable path, URL, or action identifier.
    @Column(columnDefinition = "TEXT")
    private String target;

    // Stores the icon reference.
    @Column
    private String icon;

    // Required by JPA.
    public Tile() {
    }

    public Long getId() {
        return id;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
