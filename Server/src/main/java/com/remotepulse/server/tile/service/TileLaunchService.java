
package com.remotepulse.server.tile.service;

import com.remotepulse.server.common.exception.TileLaunchException;
import com.remotepulse.server.system.service.SystemButtonService;
import com.remotepulse.server.system.service.KeyboardShortcutService;
import com.remotepulse.server.tile.entity.Tile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TileLaunchService {

    private static final Logger logger =
            LoggerFactory.getLogger(TileLaunchService.class);

    private final SystemButtonService systemButtonService;
    private final KeyboardShortcutService keyboardShortcutService;

    public TileLaunchService(
            SystemButtonService systemButtonService,
            KeyboardShortcutService keyboardShortcutService) {

        this.systemButtonService = systemButtonService;
        this.keyboardShortcutService = keyboardShortcutService;
    }

    public void launch(Tile tile) {

        if (tile == null) {
            throw new TileLaunchException("Cannot launch a null tile.");
        }

        String tileType = tile.getType();
        String target = tile.getTarget();

        if (tileType == null || tileType.isBlank()) {
            throw new TileLaunchException(
                    "Cannot launch tile: tile type is missing."
            );
        }

        if (target == null || target.isBlank()) {
            throw new TileLaunchException(
                    "Cannot launch tile: target is missing."
            );
        }

        try {

            if ("CUSTOM_APP".equals(tileType)) {

                new ProcessBuilder(
                        "wt.exe",
                        "new-tab",
                        target
                ).start();

            } else if ("WEBSITE".equals(tileType)) {

                new ProcessBuilder(
                        "cmd",
                        "/c",
                        "start",
                        "",
                        target
                ).start();

            } else if ("INSTALLED_APP".equals(tileType)) {

                new ProcessBuilder(
                        "explorer.exe",
                        "shell:AppsFolder\\" + target
                ).start();

            } else if ("SYSTEM_BUTTON".equals(tileType)) {

                systemButtonService.execute(target);

            } else if ("KEYBOARD_SHORTCUT".equals(tileType)) {

                keyboardShortcutService.execute(target);

            } else {

                throw new TileLaunchException(
                        "Unknown tile type: " + tileType
                );
            }

            logger.info(
                    "Tile launch requested. Type: {}, Target: {}",
                    tileType,
                    target
            );

        } catch (TileLaunchException exception) {

            throw exception;

        } catch (Exception exception) {

            logger.error(
                    "Failed to launch tile. Type: {}, Target: {}",
                    tileType,
                    target,
                    exception
            );

            throw new TileLaunchException(
                    "Failed to launch tile of type: " + tileType,
                    exception
            );
        }
    }
}
