package com.remotepulse.server.network.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Service responsible for discovering the local network IP address
 * of the machine running the Remote Pulse server.
 *
 * <p>Uses Windows PowerShell to identify the IPv4 address associated
 * with a network configuration that has a default gateway.</p>
 */
@Service
public class ServerNetworkService {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerNetworkService.class);

    /**
     * Finds the local IPv4 address using Windows PowerShell.
     *
     * @return the local IPv4 address, or "Unknown" if it cannot be determined
     */
    public String findLocalIpAddress() {

        Process process = null;

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "powershell.exe",
                            "-NoProfile",
                            "-Command",
                            "(Get-NetIPConfiguration | " +
                            "Where-Object { " +
                            "$_.IPv4DefaultGateway -ne $null " +
                            "} | " +
                            "Select-Object -First 1 " +
                            "-ExpandProperty IPv4Address).IPv4Address"
                    );

            process = processBuilder.start();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream(),
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String ipAddress = reader.readLine();

                int exitCode = process.waitFor();

                if (exitCode == 0 &&
                        ipAddress != null &&
                        !ipAddress.isBlank()) {

                    return ipAddress.trim();
                }

                logger.warn(
                        "Could not determine the local IP address. PowerShell exit code: {}",
                        exitCode
                );
            }

        } catch (IOException e) {

            logger.error("Failed to execute PowerShell for IP discovery.", e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.error("IP discovery was interrupted.", e);

        } finally {

            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        return "Unknown";
    }
}