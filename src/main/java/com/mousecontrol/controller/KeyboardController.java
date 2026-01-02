package com.mousecontrol.controller;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

/**
 * KeyboardController
 * Allows the application to simulate typing and key presses on the desktop
 * using java.awt.Robot. For characters that Robot cannot type directly,
 * falls back to clipboard-based paste (Ctrl+V / Cmd+V).
 */
public class KeyboardController {

    private final Robot robot;
    private final boolean isMac;

    public KeyboardController() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Failed to create Robot for KeyboardController", e);
        }
        String os = System.getProperty("os.name").toLowerCase();
        this.isMac = os.contains("mac") || os.contains("darwin");
    }

    /**
     * Type the given string. Attempts to map ASCII characters to KeyEvent VK_ codes.
     * For unsupported characters, falls back to clipboard paste.
     */
    public void typeString(String text) {
        if (text == null || text.isEmpty()) return;

        // Try to type character-by-character; if any unsupported char found, fallback to paste
        boolean fallback = false;
        for (char c : text.toCharArray()) {
            if (!typeChar(c)) {
                fallback = true;
                break;
            }
        }

        if (fallback) {
            pasteFromClipboard(text);
        }
    }

    /**
     * Attempt to type a single ASCII character using Robot key events.
     * Returns true if typed, false if unsupported (caller may fallback to clipboard).
     */
    private boolean typeChar(char c) {
        try {
            boolean upper = Character.isUpperCase(c);
            int key = 0;

            if (Character.isLetter(c)) {
                char base = Character.toLowerCase(c);
                key = KeyEvent.getExtendedKeyCodeForChar(base);
                if (key == KeyEvent.VK_UNDEFINED) return false;
                if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
                robot.keyPress(key);
                robot.keyRelease(key);
                if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
                return true;
            }

            if (Character.isDigit(c)) {
                key = KeyEvent.getExtendedKeyCodeForChar(c);
                if (key == KeyEvent.VK_UNDEFINED) return false;
                robot.keyPress(key);
                robot.keyRelease(key);
                return true;
            }

            switch (c) {
                case ' ':
                    robot.keyPress(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_SPACE);
                    return true;
                case '\n':
                case '\r':
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    return true;
                case '.':
                    robot.keyPress(KeyEvent.VK_PERIOD);
                    robot.keyRelease(KeyEvent.VK_PERIOD);
                    return true;
                case ',':
                    robot.keyPress(KeyEvent.VK_COMMA);
                    robot.keyRelease(KeyEvent.VK_COMMA);
                    return true;
                case '-':
                    robot.keyPress(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_MINUS);
                    return true;
                case '_':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_MINUS);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case ':':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_SEMICOLON);
                    robot.keyRelease(KeyEvent.VK_SEMICOLON);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case ';':
                    robot.keyPress(KeyEvent.VK_SEMICOLON);
                    robot.keyRelease(KeyEvent.VK_SEMICOLON);
                    return true;
                case '/':
                    robot.keyPress(KeyEvent.VK_SLASH);
                    robot.keyRelease(KeyEvent.VK_SLASH);
                    return true;
                case '\\':
                    robot.keyPress(KeyEvent.VK_BACK_SLASH);
                    robot.keyRelease(KeyEvent.VK_BACK_SLASH);
                    return true;
                case '!':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_1);
                    robot.keyRelease(KeyEvent.VK_1);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case '?':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_SLASH);
                    robot.keyRelease(KeyEvent.VK_SLASH);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case '"':
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_QUOTE);
                    robot.keyRelease(KeyEvent.VK_QUOTE);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case '\'':
                    robot.keyPress(KeyEvent.VK_QUOTE);
                    robot.keyRelease(KeyEvent.VK_QUOTE);
                    return true;
                case '(': // shift+9
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_9);
                    robot.keyRelease(KeyEvent.VK_9);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                case ')': // shift+0
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_0);
                    robot.keyRelease(KeyEvent.VK_0);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    return true;
                default:
                    return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Paste given text via clipboard (Ctrl+V or Cmd+V depending on OS)
     */
    private void pasteFromClipboard(String text) {
        try {
            StringSelection ss = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);

            if (isMac) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
        } catch (Exception e) {
            System.err.println("Clipboard paste failed: " + e.getMessage());
        }
    }

    /**
     * Tap a platform-specific key code (KeyEvent.VK_*) once.
     * Caller must supply a valid KeyEvent VK code.
     */
    public void tapKey(int keyCode) {
        try {
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } catch (Exception e) {
            System.err.println("tapKey failed: " + e.getMessage());
        }
    }

    /**
     * Press and hold a key
     */
    public void pressKey(int keyCode) {
        try { robot.keyPress(keyCode); } catch (Exception e) { System.err.println("pressKey failed: " + e.getMessage()); }
    }

    /**
     * Release a previously pressed key
     */
    public void releaseKey(int keyCode) {
        try { robot.keyRelease(keyCode); } catch (Exception e) { System.err.println("releaseKey failed: " + e.getMessage()); }
    }
}

