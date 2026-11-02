package com.remotepulse.server.system.service;

import org.springframework.stereotype.Service;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Service responsible for executing Windows system-button actions
 * in the Remote Pulse application.
 *
 * Uses JNA (Java Native Access) to communicate with the native
 * Windows user32 library and simulate keyboard events.
 *
 * Supported actions:
 * - VOLUME_UP: Increases the system volume.
 * - VOLUME_DOWN: Decreases the system volume.
 * - MUTE_TOGGLE: Toggles system audio mute.
 * - MEDIA_PLAY_PAUSE: Toggles media playback.
 * - PRINT_SCREEN: Simulates the Print Screen key.
 * - FULLSCREEN: Simulates the F11 key.
 *
 * This service is used by TileLaunchService when a tile is
 * configured with the SYSTEM_BUTTON type.
 *
 * Note: This service requires a Windows operating system.
 *
 * @author Remote Pulse
 */
@Service
public class SystemButtonService {

    // Windows keyboard event flag used to indicate key release.
    private static final int KEYEVENTF_KEYUP = 0x0002;

    /**
     * JNA interface representing the native Windows user32 library.
     *
     * The keybd_event function simulates keyboard key presses
     * and releases at the operating-system level.
     */
    private interface User32 extends Library {

        // Load the native Windows user32 library.
        User32 INSTANCE = Native.load("user32", User32.class);

        /**
         * Simulates a keyboard event in Windows.
         *
         * @param virtualKeyCode virtual-key code of the key
         * @param scanCode hardware scan code of the key
         * @param flags event flags, such as key release
         * @param extraInfo additional information associated with the event
         */
        void keybd_event(
                byte virtualKeyCode,
                byte scanCode,
                int flags,
                int extraInfo);
    }

    /**
     * Executes the requested Windows system action.
     *
     * Identifies the action using its string identifier and
     * delegates execution to the corresponding private method.
     *
     * @param action identifier of the system action to execute
     * @throws IllegalArgumentException if the action is not supported
     */
    public void execute(String action) {

        // Increase the system volume.
        if ("VOLUME_UP".equals(action)) {
            volumeUp();
            return;
        }

        // Decrease the system volume.
        if ("VOLUME_DOWN".equals(action)) {
            volumeDown();
            return;
        }

        // Toggle system audio mute.
        if ("MUTE_TOGGLE".equals(action)) {
            muteToggle();
            return;
        }

        // Toggle media playback.
        if ("MEDIA_PLAY_PAUSE".equals(action)) {
            mediaPlayPause();
            return;
        }

        // Simulate the Print Screen key.
        if ("PRINT_SCREEN".equals(action)) {
            printScreen();
            return;
        }

        // Simulate F11 to toggle fullscreen.
        if ("FULLSCREEN".equals(action)) {
            fullscreen();
            return;
        }

        // Reject actions that are not supported.
        throw new IllegalArgumentException(
                "Unsupported system action: " + action);
    }

    /**
     * Simulates a press and release of a Windows keyboard key.
     *
     * First sends the key-down event and then sends the key-up event.
     *
     * @param virtualKeyCode Windows virtual-key code of the key
     */
    private void pressKey(byte virtualKeyCode) {

        // Simulate pressing the key.
        User32.INSTANCE.keybd_event(
                virtualKeyCode,
                (byte) 0,
                0,
                0);

        // Simulate releasing the key.
        User32.INSTANCE.keybd_event(
                virtualKeyCode,
                (byte) 0,
                KEYEVENTF_KEYUP,
                0);
    }

    /**
     * Increases the system volume.
     *
     * Uses the Windows VK_VOLUME_UP virtual-key code.
     */
    private void volumeUp() {
        pressKey((byte) 0xAF);
    }

    /**
     * Decreases the system volume.
     *
     * Uses the Windows VK_VOLUME_DOWN virtual-key code.
     */
    private void volumeDown() {
        pressKey((byte) 0xAE);
    }

    /**
     * Toggles the system audio mute state.
     *
     * Uses the Windows VK_VOLUME_MUTE virtual-key code.
     */
    private void muteToggle() {
        pressKey((byte) 0xAD);
    }

    /**
     * Toggles media playback between play and pause.
     *
     * Uses the Windows VK_MEDIA_PLAY_PAUSE virtual-key code.
     */
    private void mediaPlayPause() {
        pressKey((byte) 0xB3);
    }

    /**
     * Simulates the Print Screen key.
     *
     * Uses the Windows VK_SNAPSHOT virtual-key code.
     */
    private void printScreen() {
        pressKey((byte) 0x2C);
    }

    /**
     * Simulates the F11 key to toggle fullscreen mode
     * in applications that support this keyboard shortcut.
     */
    private void fullscreen() {
        pressKey((byte) 0x7A);
    }
}