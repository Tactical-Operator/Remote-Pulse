
package com.remotepulse.server.icon.service;

import com.remotepulse.server.common.exception.TileNotFoundException;
import com.remotepulse.server.tile.entity.Tile;
import com.remotepulse.server.tile.repository.TileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Handles custom icon uploads for Remote Pulse tiles.
 *
 * Decodes uploaded Base64 image data, resizes the image while
 * preserving its aspect ratio, saves it as a PNG, and updates
 * the tile configuration in the database.
 *
 * Custom icons are stored in the data/icons directory.
 */
@Service
public class CustomIconService {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomIconService.class);

    private static final int ICON_SIZE = 256;

    private final TileRepository tileRepository;

    /**
     * Creates the service with its required repository dependency.
     *
     * @param tileRepository provides database access to tile configurations
     */
    public CustomIconService(TileRepository tileRepository) {
        this.tileRepository = tileRepository;
    }

    /**
     * Saves a custom icon for the specified tile.
     *
     * Decodes the uploaded image, resizes it to fit a transparent
     * 256 x 256 canvas, saves the resulting PNG, and updates the tile.
     *
     * @param slotNumber the Remote Pulse slot number
     * @param imageBase64 the uploaded image encoded as Base64
     * @return the updated tile configuration
     * @throws TileNotFoundException if the requested tile slot does not exist
     * @throws IllegalArgumentException if the uploaded image data is invalid
     * @throws IllegalStateException if image processing or file storage fails
     */
    public Tile saveCustomIcon(
            Integer slotNumber,
            String imageBase64) {

        // Find the existing tile using its slot number.
        Tile tile = tileRepository
                .findBySlotNumber(slotNumber)
                .orElseThrow(() ->
                        new TileNotFoundException(
                                "Tile slot not found: " + slotNumber
                        )
                );

        // Validate the uploaded Base64 data.
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "Image data cannot be empty."
            );
        }

        // Remove the data URL prefix if one is present.
        int commaIndex = imageBase64.indexOf(",");

        if (commaIndex >= 0) {
            imageBase64 = imageBase64.substring(commaIndex + 1);
        }

        try {
            // Decode the Base64 string into image bytes.
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            // Read the uploaded bytes as an image.
            BufferedImage originalImage;

            try (ByteArrayInputStream inputStream =
                         new ByteArrayInputStream(imageBytes)) {

                originalImage = ImageIO.read(inputStream);
            }

            // Reject image data that Java cannot read as an image.
            if (originalImage == null) {
                throw new IllegalArgumentException(
                        "Unsupported image format."
                );
            }

            // Create a standard 256 x 256 transparent icon canvas.
            BufferedImage icon = new BufferedImage(
                    ICON_SIZE,
                    ICON_SIZE,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D graphics = icon.createGraphics();

            try {
                // Clear the canvas to make its background transparent.
                graphics.setComposite(AlphaComposite.Clear);

                graphics.fillRect(
                        0,
                        0,
                        ICON_SIZE,
                        ICON_SIZE
                );

                graphics.setComposite(AlphaComposite.SrcOver);

                // Enable high-quality image rendering.
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );

                graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY
                );

                graphics.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                // Calculate the scale while preserving the image's aspect ratio.
                double scale = Math.min(
                        (double) ICON_SIZE / originalImage.getWidth(),
                        (double) ICON_SIZE / originalImage.getHeight()
                );

                int newWidth = Math.max(
                        1,
                        (int) (originalImage.getWidth() * scale)
                );

                int newHeight = Math.max(
                        1,
                        (int) (originalImage.getHeight() * scale)
                );

                // Calculate coordinates to center the resized image.
                int x = (ICON_SIZE - newWidth) / 2;
                int y = (ICON_SIZE - newHeight) / 2;

                // Draw the resized image onto the transparent canvas.
                graphics.drawImage(
                        originalImage,
                        x,
                        y,
                        newWidth,
                        newHeight,
                        null
                );

            } finally {
                // Release graphics resources after rendering.
                graphics.dispose();
            }

            // Resolve the directory used to store custom icons.
            Path iconsDirectory = Path.of("data", "icons")
                    .toAbsolutePath()
                    .normalize();

            // Create the directory if it does not exist.
            Files.createDirectories(iconsDirectory);

            // Generate the icon filename for this tile.
            String iconFileName = "slot-" + slotNumber + ".png";

            Path outputFile = iconsDirectory
                    .resolve(iconFileName)
                    .normalize();

            // Save the processed icon in PNG format.
            boolean written = ImageIO.write(
                    icon,
                    "png",
                    outputFile.toFile()
            );

            if (!written || !Files.isRegularFile(outputFile)) {
                throw new IOException(
                        "Could not create the custom icon PNG file."
                );
            }

            // Store the icon filename in the tile configuration.
            tile.setIcon(iconFileName);

            // Save the updated tile to the database.
            Tile savedTile = tileRepository.save(tile);

            logger.info(
                    "Custom icon saved for tile slot {}: {}",
                    slotNumber,
                    outputFile
            );

            return savedTile;

        } catch (IllegalArgumentException exception) {

            // Preserve validation and unsupported-image errors.
            throw exception;

        } catch (IOException exception) {

            // Report image storage or processing failures.
            throw new IllegalStateException(
                    "Failed to save custom icon for slot: " + slotNumber,
                    exception
            );
        }
    }
}
