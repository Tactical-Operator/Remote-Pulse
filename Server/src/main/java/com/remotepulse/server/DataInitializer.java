
package com.remotepulse.server;

import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the default tile slots when the Remote Pulse
 * Spring Boot application starts.
 *
 * The initializer ensures that slots 1 through 18 exist
 * in the database without creating duplicate slots or
 * overwriting existing tile configurations.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(DataInitializer.class);

    private final TileRepository tileRepository;

    /**
     * Creates the DataInitializer with the required repository.
     *
     * @param tileRepository repository used to check and save tile slots
     */
    public DataInitializer(TileRepository tileRepository) {
        this.tileRepository = tileRepository;
    }

    /**
     * Runs automatically after the Spring application starts.
     *
     * Creates any missing tile slots between 1 and 18.
     *
     * @param args command-line arguments passed to the application
     */
    @Override
    public void run(String... args) {

        for (int slotNumber = 1; slotNumber <= 18; slotNumber++) {

            // Check whether this slot already exists in the database.
            if (tileRepository.findBySlotNumber(slotNumber).isEmpty()) {

                // Create a blank tile for the missing slot.
                Tile tile = new Tile();
                tile.setSlotNumber(slotNumber);

                tileRepository.save(tile);
            }
        }

        logger.info("Remote Pulse: 18 tile slots ready.");
    }
}
