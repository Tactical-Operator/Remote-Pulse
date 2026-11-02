package com.remotepulse.server.network.controller;

import com.remotepulse.server.network.dto.ServerInfo;
import com.remotepulse.server.network.service.ServerNetworkService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for providing Remote Pulse server information.
 *
 * <p>Exposes the server's local IP address and configured port
 * so clients can determine the address used to access the server.</p>
 */
@RestController
public class ServerInfoController {

    /**
     * Configured server port; defaults to 8080 if not specified.
     */
    @Value("${server.port:8080}")
    private int serverPort;

    private final ServerNetworkService serverNetworkService;

    /**
     * Creates the controller with its required network service.
     *
     * @param serverNetworkService service for discovering the local IP address
     */
    public ServerInfoController(ServerNetworkService serverNetworkService) {
        this.serverNetworkService = serverNetworkService;
    }

    /**
     * Retrieves the server's local IP address and port.
     *
     * @return server information containing the IP address and port
     */
    @GetMapping("/api/server-info")
    public ServerInfo getServerInfo() {

        // Determine the local IP address at request time.
        String ipAddress = serverNetworkService.findLocalIpAddress();

        // Return the IP address and configured port to the client.
        return new ServerInfo(ipAddress, serverPort);
    }
}