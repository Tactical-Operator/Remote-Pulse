
package com.remotepulse.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Configures additional static resource locations for Remote Pulse.
 *
 * Uploaded tile icons are stored outside the application resources
 * directory so that they remain available independently of the
 * application's packaged JAR.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registers a resource handler for uploaded tile icons.
     *
     * Maps requests under /icons/** to the external data/icons
     * directory relative to the application's working directory.
     *
     * @param registry registry used to configure Spring MVC resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Resolve the icon directory to an absolute file-system location.
        String iconLocation = Path.of(
                        "data",
                        "icons")
                .toAbsolutePath()
                .toUri()
                .toString();

        // Serve files from data/icons through the /icons/** URL pattern.
        registry
                .addResourceHandler("/icons/**")
                .addResourceLocations(iconLocation);
    }
}
