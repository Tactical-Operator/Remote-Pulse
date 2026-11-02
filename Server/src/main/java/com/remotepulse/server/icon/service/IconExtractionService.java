
package com.remotepulse.server.icon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts icons from installed Windows applications.
 *
 * Executes the extract-icon.ps1 PowerShell script using the
 * application's ID and destination path.
 *
 * Captures the script output, checks its exit code, and verifies
 * that the expected PNG icon file was created successfully.
 */
@Service
public class IconExtractionService {

    private static final Logger logger =
            LoggerFactory.getLogger(IconExtractionService.class);

    /**
     * Extracts an installed application's icon to the specified path.
     *
     * Creates the destination directory if necessary, executes the
     * PowerShell extraction script, and verifies the resulting PNG file.
     *
     * @param appId the identifier of the installed application
     * @param outputPath the destination path for the extracted PNG icon
     * @throws IllegalArgumentException if the application ID or output path is invalid
     * @throws IllegalStateException if PowerShell extraction fails or the PNG is not created
     */
    public void extractInstalledAppIcon(
            String appId,
            String outputPath) {

        // Validate the required arguments.
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException(
                    "Application ID cannot be empty."
            );
        }

        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Icon output path cannot be empty."
            );
        }

        Process process = null;

        try {
            // Represent the destination icon file.
            Path outputFile = Path.of(outputPath)
                    .toAbsolutePath()
                    .normalize();

            // Create the parent directory if it does not exist.
            Path parentDirectory = outputFile.getParent();

            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            logger.info(
                    "Starting icon extraction for installed application: {}",
                    appId
            );

            // Path to the existing PowerShell icon extraction script.
            String scriptPath = "extract-icon.ps1";

            // Execute the script with the application ID and output path.
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    scriptPath,
                    appId,
                    outputFile.toString()
            );

            // Combine standard output and error output into one stream.
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            // Read the combined PowerShell output.
            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Wait for PowerShell to finish and retrieve its exit code.
            int exitCode = process.waitFor();

            // Report an error if the extraction script failed.
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "PowerShell icon extraction failed. Exit code: "
                                + exitCode + "\n" + output
                );
            }

            // Verify that the expected PNG file was actually created.
            if (!Files.isRegularFile(outputFile)) {
                throw new IllegalStateException(
                        "PowerShell completed successfully, but PNG was not created:\n"
                                + output
                );
            }

            // Log the script output when available.
            if (!output.isBlank()) {
                logger.debug("PowerShell icon extraction output:\n{}", output);
            }

            logger.info(
                    "Icon successfully created: {}",
                    outputFile
            );

        } catch (IOException exception) {

            // Handle errors while starting PowerShell or accessing the icon file.
            throw new IllegalStateException(
                    "Failed to execute icon extraction for application: " + appId,
                    exception
            );

        } catch (InterruptedException exception) {

            // Restore the thread's interrupted status.
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Icon extraction was interrupted for application: " + appId,
                    exception
            );

        } finally {

            // Stop PowerShell if it is still running after an unsuccessful operation.
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
