
package com.remotepulse.server.tile.repository;

import com.remotepulse.server.tile.entity.Tile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;



/**
 * Database access layer for Tile entities.
 *
 * Spring Data JPA automatically provides common database operations.
 */
public interface TileRepository extends JpaRepository<Tile, Long> {

    Optional<Tile> findBySlotNumber(Integer slotNumber);

//     This tells Spring Data JPA two things:
// - Tile — the entity this repository manages.
// - Long — the data type of the entity's primary key (id).
// By extending JpaRepository, you automatically get methods such as:
// Method	Purpose
// findAll()	Retrieve all tiles
// findById(id)	Find a tile by its database ID
// save(tile)	Insert or update a tile
// delete(tile)	Delete a tile
// deleteById(id)	Delete by database ID
// existsById(id)	Check whether a tile exists

}
