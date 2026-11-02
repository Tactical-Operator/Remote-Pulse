
package com.remotepulse.server.application.dto;

/**
 * Represents an application discovered on the Windows system.
 *
 * Stores an application's display name and unique application ID.
 * Remote Pulse uses this information to identify installed
 * applications and configure them as tiles that can be launched
 * from the client.
 */
public class InstalledApp {

    /**
     * Application name displayed to the user.
     */
    private String name;

    /**
     * Unique identifier used to launch the application.
     */
    private String appId;

    /**
     * No-argument constructor used when creating the object
     * without providing values immediately.
     */
    public InstalledApp() {
    }

    /**
     * Creates an InstalledApp object with its name and application ID.
     *
     * @param name the application's display name
     * @param appId the application's unique identifier
     */
    public InstalledApp(String name, String appId) {
        this.name = name;
        this.appId = appId;
    }

    /**
     * Returns the application's display name.
     *
     * @return the application display name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the application's display name.
     *
     * @param name the application display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the application's unique identifier.
     *
     * @return the application ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Updates the application's unique identifier.
     *
     * @param appId the application's unique identifier
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }
}
