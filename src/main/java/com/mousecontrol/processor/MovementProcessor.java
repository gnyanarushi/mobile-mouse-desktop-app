package com.mousecontrol.processor;

import com.mousecontrol.controller.MouseController;
import com.mousecontrol.models.MotionData;

import java.awt.*;

/**
 * MovementProcessor
 *
 * Converts MotionData (gyro values + click flags) into relative pixel movements
 * and issues commands to MouseController.
 *
 * Responsibilities:
 *  - sensitivity scaling (pixels per gyro unit)
 *  - smoothing (exponential)
 *  - dead-zone filtering
 *  - calibration offsets
 */
public class MovementProcessor {

    private final MouseController mouse;
    private UICallback uiCallback;  // Optional UI callback

    // Tunable parameters (defaults are conservative)
    private double sensitivity = 10.0;    // pixels per gyro unit
    private double smoothing = 0.20;      // 0..1, higher => smoother but more lag
    private double deadZone = 0.02;       // ignore gyro values with absolute value below this
    private double calibX = 0.0;          // calibration offset for gyroX
    private double calibY = 0.0;          // calibration offset for gyroY
    private boolean invertY = true;       // invert Y axis (common expectation)

    // internal smoothing state
    private double lastDx = 0.0;
    private double lastDy = 0.0;

    public MovementProcessor(MouseController mouse) {
        this.mouse = mouse;
    }

    // ===== UI Callback Interface =====
    public interface UICallback {
        void onGyroUpdate(double gyroX, double gyroY);
        void onCursorMove(int moveX, int moveY);
        void onCursorPosition(int x, int y);
    }

    public void setUICallback(UICallback callback) {
        this.uiCallback = callback;
    }

    // ===== setters for runtime tuning =====

    public void setSensitivity(double sensitivity) {
        if (sensitivity <= 0) throw new IllegalArgumentException("sensitivity must be > 0");
        this.sensitivity = sensitivity;
    }

    public void setSmoothing(double smoothing) {
        if (smoothing < 0 || smoothing > 1) throw new IllegalArgumentException("smoothing must be between 0 and 1");
        this.smoothing = smoothing;
    }

    public void setDeadZone(double deadZone) {
        if (deadZone < 0) throw new IllegalArgumentException("deadZone must be >= 0");
        this.deadZone = deadZone;
    }

    public void setCalibration(double offsetX, double offsetY) {
        this.calibX = offsetX;
        this.calibY = offsetY;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
    }

    // Reset smoothing state (useful after calibration or big jumps)
    public void resetSmoothingState() {
        lastDx = 0.0;
        lastDy = 0.0;
    }

    // ===== main entry point =====
    public void handle(MotionData data) {
        if (data == null) return;

        // 1) Apply calibration offsets
        double gx = data.gyroX - calibX;
        double gy = data.gyroY - calibY;

        // Notify UI of raw gyro values
        if (uiCallback != null) {
            uiCallback.onGyroUpdate(gx, gy);
        }

        // 2) Dead zone: treat small fluctuations as zero to reduce noise
        gx = Math.abs(gx) < deadZone ? 0.0 : gx;
        gy = Math.abs(gy) < deadZone ? 0.0 : gy;

        // 3) Scale by sensitivity
        double rawDx = gx * sensitivity;
        double rawDy = gy * sensitivity;

        // 4) Optionally invert Y so tilt up -> move up on screen
        if (invertY) {
            rawDy = -rawDy;
        }

        // 5) Exponential smoothing: new = old*(1 - a) + raw*a
        double dx = lastDx * (1.0 - smoothing) + rawDx * smoothing;
        double dy = lastDy * (1.0 - smoothing) + rawDy * smoothing;

        // Save for next smoothing step
        lastDx = dx;
        lastDy = dy;

        // 6) Round to integers (Robot API uses pixels)
        int moveX = (int) Math.round(dx);
        int moveY = (int) Math.round(dy);

        // DEBUG: Log processing details
        System.out.printf("Gyro: (%.3f, %.3f) -> Raw: (%.2f, %.2f) -> Smoothed: (%.2f, %.2f) -> Pixels: (%d, %d)%n",
                gx, gy, rawDx, rawDy, dx, dy, moveX, moveY);

        // Notify UI of cursor movement
        if (uiCallback != null && (moveX != 0 || moveY != 0)) {
            uiCallback.onCursorMove(moveX, moveY);
        }

        // 7) Perform movement if there is a non-zero delta
        if (moveX != 0 || moveY != 0) {
            mouse.moveBy(moveX, moveY);
            // Notify UI of new cursor position
            if (uiCallback != null) {
                PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                if (pointerInfo != null) {
                    java.awt.Point pos = pointerInfo.getLocation();
                    uiCallback.onCursorPosition((int)pos.getX(), (int)pos.getY());
                }
            }
        }

        // 8) Handle clicks
        if (data.leftClick) {
            mouse.leftClick();
        }
        if (data.rightClick) {
            mouse.rightClick();
        }
    }
}
