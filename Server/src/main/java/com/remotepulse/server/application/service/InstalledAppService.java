
package com.remotepulse.server.application.service;

import com.remotepulse.server.application.dto.InstalledApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers installed Windows applications for Remote Pulse.
 *
 * Uses the Windows PowerShell Get-StartApps command to retrieve
 * application names and their corresponding application IDs.
 *
 * The discovered applications are converted into InstalledApp
 * objects and returned to the application layer.
 */
@Service
public class InstalledAppService {

    private static final Logger logger =
            LoggerFactory.getLogger(InstalledAppService.class);

    /**
     * Retrieves installed applications registered in the Windows Start menu.
     *
     * Executes PowerShell, reads the application names and IDs,
     * and converts the output into InstalledApp objects.
     *
     * @return a list of discovered applications; an empty list if discovery fails
     */
    public List<InstalledApp> getInstalledApps() {

        // Store the applications discovered by PowerShell.
        List<InstalledApp> apps = new ArrayList<>();

        Process process = null;

        try {
            // Configure PowerShell to return UTF-8 output.
            String command =
                    "[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding; " +
                    "Get-StartApps | ForEach-Object { $_.Name + [char]9 + $_.AppID }";

            // Build the PowerShell process.
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-Command",
                    command
            );

            // Merge standard error into standard output to avoid
            // blocking if PowerShell produces error messages.
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            // Read PowerShell output using UTF-8 encoding.
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8
                    ))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    // Ignore empty output lines.
                    if (line.isBlank()) {
                        continue;
                    }

                    // Split each line into application name and App ID.
                    String[] parts = line.split("\t", 2);

                    if (parts.length == 2) {

                        String name = parts[0].trim();
                        String appId = parts[1].trim();

                        // Add valid application entries to the result.
                        if (!name.isBlank() && !appId.isBlank()) {
                            apps.add(new InstalledApp(name, appId));
                        }

                    } else {
                        // PowerShell error messages may also appear in the output.
                        logger.warn("PowerShell discovery output: {}", line);
                    }
                }
            }

            // Wait for PowerShell to finish execution.
            int exitCode = process.waitFor();

            // Check whether the PowerShell command completed successfully.
            if (exitCode != 0) {
                logger.error(
                        "Installed application discovery failed. PowerShell exit code: {}",
                        exitCode
                );
            } else {
                logger.info(
                        "Discovered {} installed applications.",
                        apps.size()
                );
            }

        } catch (IOException exception) {

            // Handle errors while starting or reading the PowerShell process.
            logger.error(
                    "Failed to execute PowerShell for application discovery.",
                    exception
            );

        } catch (InterruptedException exception) {

            // Restore the thread's interrupted status.
            Thread.currentThread().interrupt();

            logger.error(
                    "Installed application discovery was interrupted.",
                    exception
            );

        } finally {

            // Ensure the process is stopped if execution did not finish normally.
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        // Return all applications successfully discovered.
        return apps;
    }
}
