package com.mousecontrol.controller;


import java.awt.*;
import java.awt.event.InputEvent;

public class MouseController {

    private final Robot robot;
    private boolean useXdotool = false;  // Flag to use xdotool on Linux
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_LINUX = OS_NAME.contains("linux");

    public MouseController() {
            try {
                    this.robot = new Robot();
                    this.robot.setAutoDelay(0);
                    System.out.println("✓ Robot initialized successfully");

                    // Check if we're on Linux and xdotool is available
                    if (IS_LINUX) {
                        useXdotool = checkXdotoolAvailable();
                        if (useXdotool) {
                            System.out.println("✓ xdotool found - using xdotool for cursor movement");
                        } else {
                            System.err.println("⚠ xdotool not found - trying Robot (may not work without sudo)");
                            System.err.println("  Install xdotool: sudo apt-get install xdotool");
                        }
                    }

                    // Test if cursor control works
                    Point testPos = MouseInfo.getPointerInfo().getLocation();
                    System.out.println("Current cursor position: " + testPos.x + ", " + testPos.y);

                    // Try to move cursor 1 pixel and back to test
                    moveToAbsolute(testPos.x + 1, testPos.y);
                    Thread.sleep(10);
                    Point afterMove = MouseInfo.getPointerInfo().getLocation();
                    moveToAbsolute(testPos.x, testPos.y); // Move back

                    if (afterMove.x == testPos.x + 1) {
                        System.out.println("✓ Cursor movement working successfully!");
                    } else {
                        System.err.println("⚠ WARNING: Cursor movement not working!");
                        System.err.println("  Expected: " + (testPos.x + 1) + ", Got: " + afterMove.x);
                        if (IS_LINUX && !useXdotool) {
                            System.err.println("  Try installing xdotool: sudo apt-get install xdotool");
                        }
                    }
            }
            catch (AWTException awtException ) {
                throw new RuntimeException("Failed to create Robot instance", awtException);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Robot initialization was interrupted", e);
            }
    }

    public MouseController(Robot robot) {
        this.robot = robot;
        if (IS_LINUX) {
            useXdotool = checkXdotoolAvailable();
        }
    }

    /**
     * Check if xdotool is available on the system
     */
    private static boolean checkXdotoolAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "xdotool");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Move cursor to absolute position using xdotool (Linux) or Robot (other systems)
     */
    private void moveToAbsolute(int x, int y) {
        if (useXdotool) {
            try {
                ProcessBuilder pb = new ProcessBuilder("xdotool", "mousemove", String.valueOf(x), String.valueOf(y));
                Process process = pb.start();
                process.waitFor();
                // Don't check exit code, sometimes it still works even if it returns non-zero
            } catch (Exception e) {
                System.err.println("Error using xdotool: " + e.getMessage());
                // Fallback to Robot
                robot.mouseMove(x, y);
            }
        } else {
            robot.mouseMove(x, y);
        }
    }

    /**
     * Move cursor relative to current position
     */


    public void moveBy(int dx, int dy) {
        // Get current mouse position
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            System.err.println("ERROR: Could not get pointer info");
            return;
        }

        Point currentPos = pointerInfo.getLocation();

        // Get screen bounds to clamp cursor
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Calculate total screen bounds across all monitors
        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            minX = Math.min(minX, bounds.x);
            minY = Math.min(minY, bounds.y);
            maxX = Math.max(maxX, bounds.x + bounds.width);
            maxY = Math.max(maxY, bounds.y + bounds.height);
        }

        int newX = currentPos.x + dx;
        int newY = currentPos.y + dy;

        // Clamp to screen bounds
        newX = Math.max(minX, Math.min(newX, maxX - 1));
        newY = Math.max(minY, Math.min(newY, maxY - 1));

        System.out.printf("Moving cursor: (%d, %d) -> (%d, %d) [delta: %d, %d]%n",
                currentPos.x, currentPos.y, newX, newY, dx, dy);

        moveToAbsolute(newX, newY);

        // Small delay to ensure the move is registered
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // VERIFY: Check if cursor actually moved
        PointerInfo verifyInfo = MouseInfo.getPointerInfo();
        if (verifyInfo != null) {
            Point actualPos = verifyInfo.getLocation();
            if (actualPos.x != newX || actualPos.y != newY) {
                System.err.println("⚠ CURSOR DID NOT MOVE!");
                System.err.println("  Tried to move to: (" + newX + ", " + newY + ")");
                System.err.println("  Actual position: (" + actualPos.x + ", " + actualPos.y + ")");
                if (IS_LINUX && !useXdotool) {
                    System.err.println("  → Try installing xdotool: sudo apt-get install xdotool");
                }
            } else {
                System.out.println("✓ Cursor moved successfully to: (" + actualPos.x + ", " + actualPos.y + ")");
            }
        }
    }


    public void leftClick() {
        System.out.println("LEFT CLICK executed");
        try {
            if (useXdotool) {
                ProcessBuilder pb = new ProcessBuilder("xdotool", "click", "1");
                Process process = pb.start();
                process.waitFor();
            } else {
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
        } catch (Exception e) {
            System.err.println("Left click error: " + e.getMessage());
        }
    }

    public void rightClick() {
        System.out.println("RIGHT CLICK executed");
        try {
            if (useXdotool) {
                ProcessBuilder pb = new ProcessBuilder("xdotool", "click", "3");
                Process process = pb.start();
                process.waitFor();
            } else {
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            }
        } catch (Exception e) {
            System.err.println("Right click error: " + e.getMessage());
        }
    }

}
