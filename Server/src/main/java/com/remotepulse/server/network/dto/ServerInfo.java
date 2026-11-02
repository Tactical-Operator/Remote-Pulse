package com.remotepulse.server.network.dto;

/**
 * Represents information about the running Remote Pulse server.
 *
 * <p>Stores the server's IP address, port, and combined network
 * address so the frontend or Android client can identify the server.</p>
 */
public class ServerInfo {

    /**
     * Local network IP address of the server.
     */
    private String ip;

    /**
     * Port on which the Spring Boot server is running.
     */
    private int port;

    /**
     * Combined server address in the format IP:PORT.
     */
    private String address;

    /**
     * No-argument constructor used to create the object without
     * initializing its fields immediately.
     */
    public ServerInfo() {
    }

    /**
     * Creates server information using the IP address and port.
     * Also constructs the combined address in IP:PORT format.
     *
     * @param ip   the server's IP address
     * @param port the server's port number
     */
    public ServerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.address = ip + ":" + port;
    }

    /**
     * Returns the server's IP address.
     *
     * @return the server's IP address
     */
    public String getIp() {
        return ip;
    }

    /**
     * Updates the server's IP address.
     *
     * @param ip the server's IP address
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Returns the server's port number.
     *
     * @return the server's port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Updates the server's port number.
     *
     * @param port the server's port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the combined server address.
     *
     * @return the server address in IP:PORT format
     */
    public String getAddress() {
        return address;
    }

    /**
     * Updates the combined server address.
     *
     * @param address the combined server address
     */
    public void setAddress(String address) {
        this.address = address;
    }
}