package com.remotepulse.server.system.service;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KeyboardShortcutService {

    private static final Logger logger =
            LoggerFactory.getLogger(KeyboardShortcutService.class);

    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    private static final int KEYEVENTF_SCANCODE = 0x0008;

    private static final int MAPVK_VK_TO_VSC = 0;

    /*
     * Additional Windows API mapping.
     * User32 from JNA does not expose MapVirtualKey directly.
     */
    private interface User32Extra extends StdCallLibrary {

        User32Extra INSTANCE = Native.load(
                "user32",
                User32Extra.class,
                W32APIOptions.DEFAULT_OPTIONS
        );

        int MapVirtualKey(int virtualKey, int mapType);
    }

    private static final Map<String, Integer> KEY_CODES = new HashMap<>();

    private static final Map<String, Integer> MODIFIER_CODES = Map.of(
            "CTRL", 0x11,
            "ALT", 0x12,
            "SHIFT", 0x10
    );

    private static final Set<Integer> EXTENDED_KEYS = Set.of(
            0x21, 0x22, 0x23, 0x24,
            0x25, 0x26, 0x27, 0x28,
            0x2D, 0x2E
    );

    static {

        // Letters A-Z
        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_CODES.put(String.valueOf(c), (int) c);
        }

        // Numbers 0-9
        for (char c = '0'; c <= '9'; c++) {
            KEY_CODES.put(String.valueOf(c), (int) c);
        }

        // Function keys F1-F12
        for (int i = 1; i <= 12; i++) {
            KEY_CODES.put("F" + i, 0x70 + i - 1);
        }

        // Special keys
        KEY_CODES.put("SPACE", 0x20);
        KEY_CODES.put("ENTER", 0x0D);
        KEY_CODES.put("TAB", 0x09);
        KEY_CODES.put("ESC", 0x1B);
        KEY_CODES.put("BACKSPACE", 0x08);
        KEY_CODES.put("DELETE", 0x2E);
        KEY_CODES.put("INSERT", 0x2D);
        KEY_CODES.put("HOME", 0x24);
        KEY_CODES.put("END", 0x23);
        KEY_CODES.put("PAGEUP", 0x21);
        KEY_CODES.put("PAGEDOWN", 0x22);
        KEY_CODES.put("UP", 0x26);
        KEY_CODES.put("DOWN", 0x28);
        KEY_CODES.put("LEFT", 0x25);
        KEY_CODES.put("RIGHT", 0x27);
    }

    /**
     * Executes a keyboard shortcut on Windows.
     */
    public void execute(String shortcut) {

        if (!System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("windows")) {

            throw new IllegalStateException(
                    "Keyboard shortcuts are supported only on Windows."
            );
        }

        ParsedShortcut parsed = parseShortcut(shortcut);

        List<Integer> events = new ArrayList<>();

        // Key-down events: modifiers first, then main key.
        events.addAll(parsed.modifiers());
        events.add(parsed.mainKey());

        // Key-up events: main key first, then modifiers in reverse order.
        for (int i = events.size() - 1; i >= 0; i--) {
            events.add(events.get(i));
        }

        sendShortcutEvents(
                events,
                parsed.modifiers().size() + 1
        );

        logger.info(
                "Keyboard shortcut sent: {}",
                shortcut
        );
    }

    /**
     * Parses and validates the shortcut.
     */
    private ParsedShortcut parseShortcut(String shortcut) {

        if (shortcut == null || shortcut.isBlank()) {
            throw new IllegalArgumentException(
                    "Keyboard shortcut cannot be empty."
            );
        }

        String[] parts = shortcut
                .trim()
                .toUpperCase(Locale.ROOT)
                .split("\\+", -1);

        if (parts.length < 2 || parts.length > 4) {
            throw new IllegalArgumentException(
                    "Shortcut must contain one main key and one to three modifiers."
            );
        }

        Set<String> uniqueParts = new HashSet<>();
        List<Integer> modifiers = new ArrayList<>();

        Integer mainKey = null;

        for (String part : parts) {

            String key = part.trim();

            if (key.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid shortcut format."
                );
            }

            if (!uniqueParts.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate key: " + key
                );
            }

            if (MODIFIER_CODES.containsKey(key)) {

                modifiers.add(MODIFIER_CODES.get(key));

            } else {

                if (mainKey != null) {
                    throw new IllegalArgumentException(
                            "Only one main key is allowed."
                    );
                }

                mainKey = KEY_CODES.get(key);

                if (mainKey == null) {
                    throw new IllegalArgumentException(
                            "Unsupported key: " + key
                    );
                }
            }
        }

        if (modifiers.isEmpty() || mainKey == null) {
            throw new IllegalArgumentException(
                    "Shortcut requires at least one modifier and one main key."
            );
        }

        validateCombination(uniqueParts);

        return new ParsedShortcut(modifiers, mainKey);
    }

    /**
     * Blocks selected Windows system shortcuts.
     */
    private void validateCombination(Set<String> keys) {

        if (keys.containsAll(Set.of("ALT", "F4"))) {
            throw new IllegalArgumentException(
                    "ALT+F4 is not allowed."
            );
        }

        if (keys.containsAll(Set.of("CTRL", "SHIFT", "ESC"))) {
            throw new IllegalArgumentException(
                    "CTRL+SHIFT+ESC is not allowed."
            );
        }

        if (keys.containsAll(Set.of("CTRL", "ALT", "DELETE"))) {
            throw new IllegalArgumentException(
                    "CTRL+ALT+DELETE is not allowed."
            );
        }

        if (keys.containsAll(Set.of("ALT", "TAB"))) {
            throw new IllegalArgumentException(
                    "ALT+TAB is not allowed."
            );
        }
    }

    /**
     * Sends shortcut events in separate stages.
     */
    private void sendShortcutEvents(
            List<Integer> keyCodes,
            int keyDownCount) {

        List<Integer> modifiers =
                keyCodes.subList(0, keyDownCount - 1);

        int mainKey = keyCodes.get(keyDownCount - 1);

        boolean modifiersPressed = false;

        try {

            // Stage 1: Press modifier keys.
            sendInputBatch(modifiers, false);
            modifiersPressed = true;

            pauseBetweenEvents();

            // Stage 2: Press main key.
            sendInputBatch(List.of(mainKey), false);

            pauseBetweenEvents();

            // Stage 3: Release main key.
            sendInputBatch(List.of(mainKey), true);

            pauseBetweenEvents();

        } finally {

            // Stage 4: Release modifier keys in reverse order.
            if (modifiersPressed) {

                List<Integer> reversedModifiers =
                        new ArrayList<>(modifiers);

                Collections.reverse(reversedModifiers);

                sendInputBatch(reversedModifiers, true);
            }
        }
    }

    /**
     * Sends keyboard events using Windows scan codes.
     */
    private void sendInputBatch(
            List<Integer> keyCodes,
            boolean keyUp) {

        int eventCount = keyCodes.size();

        WinUser.INPUT[] inputs =
                (WinUser.INPUT[]) new WinUser.INPUT().toArray(eventCount);

        for (int i = 0; i < eventCount; i++) {

            int virtualKey = keyCodes.get(i);

            int scanCode = User32Extra.INSTANCE.MapVirtualKey(
                    virtualKey,
                    MAPVK_VK_TO_VSC
            );

            if (scanCode == 0) {
                throw new IllegalArgumentException(
                        "Could not map virtual key: " + virtualKey
                );
            }

            WinUser.INPUT input = inputs[i];

            input.type = new WinDef.DWORD(
                    WinUser.INPUT.INPUT_KEYBOARD
            );

            WinUser.KEYBDINPUT keyboardInput =
                    new WinUser.KEYBDINPUT();

            // Scan-code input requires wVk = 0.
            keyboardInput.wVk = new WinDef.WORD(0);
            keyboardInput.wScan = new WinDef.WORD(scanCode);

            int flags = KEYEVENTF_SCANCODE;

            if (keyUp) {
                flags |= KEYEVENTF_KEYUP;
            }

            if (EXTENDED_KEYS.contains(virtualKey)) {
                flags |= KEYEVENTF_EXTENDEDKEY;
            }

            keyboardInput.dwFlags = new WinDef.DWORD(flags);
            keyboardInput.time = new WinDef.DWORD(0);

            keyboardInput.dwExtraInfo =
                    new BaseTSD.ULONG_PTR(0);

            input.input.setType(WinUser.KEYBDINPUT.class);
            input.input.ki = keyboardInput;

            input.write();
        }

        WinDef.DWORD sent = User32.INSTANCE.SendInput(
                new WinDef.DWORD(eventCount),
                inputs,
                inputs[0].size()
        );

        if (sent.intValue() != eventCount) {

            int errorCode = Native.getLastError();

            logger.error(
                    "Scan-code SendInput failed. Expected: {}, Sent: {}, Error: {}",
                    eventCount,
                    sent.intValue(),
                    errorCode
            );

            throw new IllegalStateException(
                    "Windows accepted only " + sent.intValue()
                            + " of " + eventCount
                            + " keyboard events. Error code: " + errorCode
            );
        }

        logger.debug(
                "Windows accepted {} scan-code keyboard events.",
                eventCount
        );
    }

    /**
     * Adds a short delay between input stages.
     */
    private void pauseBetweenEvents() {

        try {
            Thread.sleep(30);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Keyboard shortcut execution was interrupted.",
                    e
            );
        }
    }

    private record ParsedShortcut(
            List<Integer> modifiers,
            int mainKey
    ) {
    }
}