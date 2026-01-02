package com.mousecontrol;


import com.mousecontrol.communication.TCPServer;
import com.mousecontrol.communication.UDPStreamer;
import com.mousecontrol.communication.WebSocketStreamer;
import com.mousecontrol.controller.MouseController;
import com.mousecontrol.controller.KeyboardController;
import com.mousecontrol.processor.MovementProcessor;
import com.mousecontrol.ui.DashboardUI;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**9
     * Check if xdotool is installed on the system
     */
    private static boolean checkXdotoolInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "xdotool");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args)  {


        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     Mobile Mouse Controller - Desktop Application           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        // Get OS information
        String osName = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        String userName = System.getProperty("user.name");
        boolean isWindows = osName.contains("win");
        boolean isLinux = osName.contains("linux");

        // System diagnostics
        System.out.println("📊 System Information:");
        System.out.println("  OS: " + System.getProperty("os.name") + " " + osVersion);
        System.out.println("  Java Version: " + javaVersion);
        System.out.println("  User: " + userName);

        if (isLinux) {
            System.out.println("  Display: " + System.getenv("DISPLAY"));
            System.out.println("  Session Type: " + System.getenv("XDG_SESSION_TYPE"));
        }

        System.out.println("\n🔧 Platform-Specific Setup:");

        // Windows-specific instructions
        if (isWindows) {
            System.out.println("\n  ✅ WINDOWS DETECTED");
            System.out.println("  The application is ready to use on Windows!");
            System.out.println("  • Cursor control: ✓ Fully supported");
            System.out.println("  • Mouse clicks: ✓ Fully supported");
            System.out.println("  • No additional setup needed");
            System.out.println("\n  📱 Ready to connect from mobile device:");
            System.out.println("     1. Note your computer's IP address");
            System.out.println("     2. Open Flutter app on mobile");
            System.out.println("     3. Connect to <YOUR_IP>:5000");
            System.out.println("     4. Enjoy your mobile mouse!\n");
        }
        // Linux-specific instructions
        else if (isLinux) {
            System.out.println("\n  🐧 LINUX DETECTED");

            String xdgSession = System.getenv("XDG_SESSION_TYPE");
            boolean isWayland = xdgSession != null && xdgSession.contains("wayland");

            System.out.println("  Session Type: " + (xdgSession != null ? xdgSession.toUpperCase() : "UNKNOWN"));

            // Check for xdotool
            boolean hasXdotool = checkXdotoolInstalled();

            if (hasXdotool) {
                System.out.println("  ✅ xdotool: INSTALLED");
                System.out.println("  • Cursor control: ✓ Optimal (no sudo needed)");
                System.out.println("  • Mouse clicks: ✓ Optimal");
                System.out.println("  • Status: Ready to use!\n");
            } else {
                System.out.println("  ❌ xdotool: NOT INSTALLED");
                System.out.println("  • Recommended: Install xdotool for best experience");
                System.out.println("  • Command: sudo apt-get install xdotool");
                System.out.println("  • After install, restart the application\n");
                System.out.println("  ⚠️  Fallback: Using Robot class (may require sudo)");
                System.out.println("  • If cursor doesn't move, run: sudo ./gradlew run");
            }

            System.out.println("  💡 Troubleshooting:");
            if (isWayland) {
                System.out.println("     • Running on Wayland - some features may be limited");
                System.out.println("     • For best results, switch to X11 session if available");
            }
            System.out.println("     • Check DISPLAY variable: echo $DISPLAY");
            System.out.println("     • Test xdotool: xdotool mousemove 100 100\n");
        }

        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║ Starting application...                                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        try {
            // Create headless dashboard (no Swing window will be created)
            DashboardUI dashboard = new DashboardUI();
            dashboard.addLog("Initializing...");

            // Create controller and processor
            MouseController mouse = new MouseController();
            MovementProcessor processor = new MovementProcessor(mouse);

            // Tuning defaults - adjusted for small gyro values
            // Reduced sensitivity so movements are slower/less jumpy by default
            // (was 10.0). You can further tune this value as needed.
            processor.setSensitivity(2.0);
            processor.setSmoothing(0.1);      // Less smoothing to preserve movement
            processor.setDeadZone(0.0);       // Disable dead zone for testing

            // Set up UI callbacks from processor
            processor.setUICallback(new MovementProcessor.UICallback() {
                @Override
                public void onGyroUpdate(double gyroX, double gyroY) {
                    dashboard.updateGyroValues(gyroX, gyroY);
                }

                @Override
                public void onCursorMove(int moveX, int moveY) {
                    dashboard.updateCursorMovement(moveX, moveY);
                    dashboard.addLog(String.format("Move: (%+d, %+d)", moveX, moveY));
                }

                @Override
                public void onCursorPosition(int x, int y) {
                    dashboard.updateCursorPosition(x, y);
                }
            });

            // Start TCP Server
            TCPServer server = new TCPServer(5000, processor);
            server.setServerCallback(new TCPServer.ServerCallback() {
                @Override
                public void onConnectionStatusChanged(String status) {
                    dashboard.setConnectionStatus(status);
                    dashboard.addLog(status);
                }
            });

            // Create and attach UDPStreamer with sane defaults
            UDPStreamer streamer = new UDPStreamer(1100); // fragment size ~1100 bytes
            server.setUdpStreamer(streamer);

            // Create and attach KeyboardController
            KeyboardController kc = new KeyboardController();
            server.setKeyboardController(kc);

            // Create and attach WebSocketStreamer (optional)
            WebSocketStreamer ws = new WebSocketStreamer();
            server.setWebSocketStreamer(ws);

            server.start();

            dashboard.addLog("TCP Server started on port 5000");
            dashboard.addLog("Waiting for connection from Flutter...");
            dashboard.addLog("Tip: You can start WebSocket streaming by sending a control JSON over TCP: {\"websocket\":{\"cmd\":\"start\",\"port\":8080}}\n");

            // Keep app running
            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled exception in main", e);
        }


    }
}
