package com.mousecontrol.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Headless replacement for the original Swing-based DashboardUI.
 * Keeps the same public API used by the rest of the app but logs
 * updates to stdout so the application can run in the background.
 */
public class DashboardUI {

    private final StringBuilder logBuffer = new StringBuilder();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    // state (kept for potential debugging)
    private volatile double gyroX = 0.0;
    private volatile double gyroY = 0.0;
    private volatile int moveX = 0;
    private volatile int moveY = 0;
    private volatile int cursorX = 0;
    private volatile int cursorY = 0;
    private volatile String connectionStatus = "Waiting for connection...";

    public DashboardUI() {
        addLog("Headless dashboard initialized.");
    }

    public void updateGyroValues(double gyroX, double gyroY) {
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        addLog(String.format("Gyro updated: %+f, %+f", gyroX, gyroY));
    }

    public void updateCursorMovement(int moveX, int moveY) {
        this.moveX = moveX;
        this.moveY = moveY;
        addLog(String.format("Cursor moved: %+d, %+d", moveX, moveY));
    }

    public void updateCursorPosition(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
        addLog(String.format("Cursor position: %d, %d", x, y));
    }

    public void setConnectionStatus(String status) {
        this.connectionStatus = status;
        addLog("Connection status: " + status);
    }

    public synchronized void addLog(String message) {
        String ts = LocalDateTime.now().format(dtf);
        String line = ts + " - " + message;
        System.out.println(line);
        logBuffer.append(line).append('\n');
        // keep the buffer from growing unbounded
        if (logBuffer.length() > 32_000) {
            logBuffer.delete(0, logBuffer.length() / 2);
        }
    }

    // Optional getters for debugging or tests
    public double getGyroX() { return gyroX; }
    public double getGyroY() { return gyroY; }
    public int getMoveX() { return moveX; }
    public int getMoveY() { return moveY; }
    public int getCursorX() { return cursorX; }
    public int getCursorY() { return cursorY; }
    public String getConnectionStatus() { return connectionStatus; }

}
