package com.mousecontrol.ui;

import javax.swing.*;
import java.awt.*;

public class DashboardUI extends JFrame {

    private final DashboardPanel panel;

    public DashboardUI() {
        setTitle("Mobile Mouse Controller - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        panel = new DashboardPanel();
        add(panel);

        setVisible(true);
    }

    public void updateGyroValues(double gyroX, double gyroY) {
        panel.setGyroValues(gyroX, gyroY);
    }

    public void updateCursorMovement(int moveX, int moveY) {
        panel.setCursorMovement(moveX, moveY);
    }

    public void updateCursorPosition(int x, int y) {
        panel.setCursorPosition(x, y);
    }

    public void setConnectionStatus(String status) {
        panel.setConnectionStatus(status);
    }

    public void addLog(String message) {
        panel.addLog(message);
    }

    // Inner class for the drawing panel
    private static class DashboardPanel extends JPanel {
        private double gyroX = 0;
        private double gyroY = 0;
        private int moveX = 0;
        private int moveY = 0;
        private int cursorX = 0;
        private int cursorY = 0;
        private String connectionStatus = "Waiting for connection...";
        private final StringBuilder logBuffer = new StringBuilder();

        public DashboardPanel() {
            setBackground(new Color(20, 20, 30));
            setForeground(Color.WHITE);
            setFont(new Font("Monospaced", Font.PLAIN, 12));
        }

        public synchronized void setGyroValues(double gx, double gy) {
            this.gyroX = gx;
            this.gyroY = gy;
            repaint();
        }

        public synchronized void setCursorMovement(int mx, int my) {
            this.moveX = mx;
            this.moveY = my;
            repaint();
        }

        public synchronized void setCursorPosition(int x, int y) {
            this.cursorX = x;
            this.cursorY = y;
            repaint();
        }

        public synchronized void setConnectionStatus(String status) {
            this.connectionStatus = status;
            repaint();
        }

        public synchronized void addLog(String message) {
            logBuffer.append(message).append("\n");
            // Keep only last 10 lines
            String[] lines = logBuffer.toString().split("\n");
            if (lines.length > 10) {
                StringBuilder newLog = new StringBuilder();
                for (int i = lines.length - 10; i < lines.length; i++) {
                    if (i >= 0) {
                        newLog.append(lines[i]).append("\n");
                    }
                }
                logBuffer.setLength(0);
                logBuffer.append(newLog);
            }
            repaint();
        }

        @Override
        protected synchronized void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);

            int y = 30;

            // Title
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("Mobile Mouse Controller", 20, y);
            y += 40;

            // Connection Status
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            if (connectionStatus.contains("Connected")) {
                g2d.setColor(new Color(100, 255, 100)); // Green
            } else {
                g2d.setColor(new Color(255, 100, 100)); // Red
            }
            g2d.drawString("Status: " + connectionStatus, 20, y);
            g2d.setColor(Color.WHITE);
            y += 35;

            // Gyro Values Section
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("GYRO VALUES:", 20, y);
            y += 25;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g2d.drawString(String.format("Gyro X: %+.4f", gyroX), 40, y);
            y += 25;
            g2d.drawString(String.format("Gyro Y: %+.4f", gyroY), 40, y);
            y += 35;

            // Draw Gyro Visual Indicator (compass-like)
            drawGyroIndicator(g2d, 250, 130, gyroX, gyroY);
            y = 200;

            // Cursor Movement Section
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("CURSOR MOVEMENT:", 20, y);
            y += 25;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g2d.drawString(String.format("Move X: %+d pixels", moveX), 40, y);
            y += 25;
            g2d.drawString(String.format("Move Y: %+d pixels", moveY), 40, y);
            y += 35;

            // Cursor Position Section with Large Arrow Indicator
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("CURSOR POSITION:", 20, y);
            y += 25;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g2d.drawString(String.format("X: %d, Y: %d", cursorX, cursorY), 40, y);
            y += 10;

            // Draw large cursor position indicator with arrow
            drawCursorIndicator(g2d, 250, y + 40, cursorX, cursorY);
            y += 80;

            // Log Section
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("LOG:", 20, y);
            y += 20;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2d.setColor(new Color(150, 255, 150));
            String[] logLines = logBuffer.toString().split("\n");
            for (String logLine : logLines) {
                if (y < getHeight() - 10) {
                    g2d.drawString(logLine, 30, y);
                    y += 18;
                }
            }
        }

        private void drawGyroIndicator(Graphics2D g2d, int centerX, int centerY, double gx, double gy) {
            int radius = 60;

            // Draw circle background
            g2d.setColor(new Color(50, 50, 70));
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Draw circle border
            g2d.setColor(new Color(150, 150, 200));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Draw axes
            g2d.setColor(new Color(100, 100, 150));
            g2d.drawLine(centerX - radius, centerY, centerX + radius, centerY); // X-axis
            g2d.drawLine(centerX, centerY - radius, centerX, centerY + radius); // Y-axis

            // Draw cursor indicator based on gyro
            double angle = Math.atan2(gy, gx);
            double magnitude = Math.min(Math.sqrt(gx * gx + gy * gy) * 20, radius - 10);

            int indicatorX = (int) (centerX + magnitude * Math.cos(angle));
            int indicatorY = (int) (centerY + magnitude * Math.sin(angle));

            // Draw cursor dot
            g2d.setColor(new Color(255, 100, 100)); // Red dot
            g2d.fillOval(indicatorX - 6, indicatorY - 6, 12, 12);

            // Draw line from center to indicator
            g2d.setColor(new Color(200, 100, 100));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX, centerY, indicatorX, indicatorY);
        }

        private void drawCursorIndicator(Graphics2D g2d, int centerX, int centerY, int cursorX, int cursorY) {
            // Draw a border box representing the screen
            int boxWidth = 240;
            int boxHeight = 50;
            int boxX = centerX - boxWidth / 2;
            int boxY = centerY - boxHeight / 2;

            g2d.setColor(new Color(50, 50, 70));
            g2d.fillRect(boxX, boxY, boxWidth, boxHeight);

            g2d.setColor(new Color(150, 150, 200));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(boxX, boxY, boxWidth, boxHeight);

            // Normalize cursor position to the box size (assuming screen is typically ~1920x1080)
            // Map cursor position to box coordinates
            double normalizedX = (double) cursorX / 1920.0; // Adjust based on actual screen width
            double normalizedY = (double) cursorY / 1080.0; // Adjust based on actual screen height

            // Clamp to box boundaries
            normalizedX = Math.max(0, Math.min(1, normalizedX));
            normalizedY = Math.max(0, Math.min(1, normalizedY));

            int arrowX = boxX + (int) (normalizedX * boxWidth);
            int arrowY = boxY + (int) (normalizedY * boxHeight);

            // Draw large arrow pointing to cursor position
            drawArrow(g2d, arrowX, arrowY, 15);

            // Draw position text below
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2d.drawString(String.format("Screen: (%d, %d)", cursorX, cursorY), boxX, boxY + boxHeight + 20);
        }

        private void drawArrow(Graphics2D g2d, int x, int y, int size) {
            // Draw a large arrow/pointer at the cursor position
            int[] arrowXPoints = {x, x - size, x + size};
            int[] arrowYPoints = {y - size, y + size, y + size};

            // Draw arrow body
            g2d.setColor(new Color(0, 255, 0)); // Bright green
            g2d.fillPolygon(arrowXPoints, arrowYPoints, 3);

            // Draw arrow outline
            g2d.setColor(new Color(255, 255, 0)); // Yellow outline
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(arrowXPoints, arrowYPoints, 3);

            // Draw a circle around the arrow for extra visibility
            g2d.setColor(new Color(255, 255, 0, 100)); // Semi-transparent yellow
            g2d.drawOval(x - size - 5, y - size - 5, (size + 5) * 2, (size + 5) * 2);
        }
    }
}

