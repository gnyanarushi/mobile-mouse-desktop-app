package com.mousecontrol.communication;

import com.mousecontrol.models.MotionData;
import com.mousecontrol.processor.MovementProcessor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import com.mousecontrol.controller.KeyboardController;
import com.mousecontrol.communication.WebSocketStreamer; // added import

/**
 * TCPServer
 *
 * Runs in the background.
 * Waits for a connection from the Flutter mobile app.
 * Reads JSON messages line-by-line.
 * Passes MotionData to MovementProcessor.
 *
 * Extended: accepts a small control JSON for starting/stopping UDP screen streaming and keyboard events.
 */
public class TCPServer {

    private final int port;
    private final MovementProcessor processor;
    private ServerCallback serverCallback;
    private UDPStreamer udpStreamer;
    private KeyboardController keyboardController;
    private WebSocketStreamer webSocketStreamer; // added

    // Track the currently connected client so we can close it when a new one connects
    private volatile Socket currentClient;
    private volatile Thread currentClientHandler;

    public interface ServerCallback {
        void onConnectionStatusChanged(String status);
    }

    public void setServerCallback(ServerCallback callback) {
        this.serverCallback = callback;
    }

    public void setUdpStreamer(UDPStreamer streamer) {
        this.udpStreamer = streamer;
    }

    public void setKeyboardController(KeyboardController kc) {
        this.keyboardController = kc;
    }

    // New setter for WebSocket streamer
    public void setWebSocketStreamer(WebSocketStreamer ws) {
        this.webSocketStreamer = ws;
    }

    public TCPServer(int port, MovementProcessor processor) {
        this.port = port;
        this.processor = processor;
    }

    /**
     * Starts the server in a background thread. Accepts multiple sequential client connections.
     */
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("TCP Server running on port " + port);
                if (serverCallback != null) {
                    serverCallback.onConnectionStatusChanged("Waiting for connection on port " + port);
                }

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket client = serverSocket.accept();

                        // If another client is connected, close it to allow this new one to take over
                        synchronized (this) {
                            if (currentClient != null && !currentClient.isClosed()) {
                                try {
                                    currentClient.close();
                                } catch (Exception ignored) {}
                            }
                            currentClient = client;
                        }

                        String clientAddress = client.getRemoteSocketAddress().toString();
                        InetAddress clientInet = client.getInetAddress();
                        System.out.println("Client connected: " + clientAddress);
                        if (serverCallback != null) {
                            serverCallback.onConnectionStatusChanged("Connected: " + clientAddress);
                        }

                        // Start a handler thread for this client
                        Thread handler = new Thread(() -> handleClient(client, clientInet), "tcp-client-handler");
                        handler.setDaemon(true);
                        currentClientHandler = handler;
                        handler.start();

                    } catch (Exception e) {
                        System.err.println("TCP accept loop error: " + e.getMessage());
                        // brief pause to avoid hot loop on persistent errors
                        try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }

            } catch (Exception e) {
                System.err.println("TCP Server error: " + e.getMessage());
                if (serverCallback != null) {
                    serverCallback.onConnectionStatusChanged("Error: " + e.getMessage());
                }
                e.printStackTrace();
            }
        }, "tcp-server-thread").start();
    }

    /**
     * Handle a single client's incoming lines until it disconnects. Cleans up references on exit.
     */
    private void handleClient(Socket client, InetAddress clientInet) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JSONObject json = new JSONObject(line);
                    if (json.has("stream") && udpStreamer != null) {
                        JSONObject s = json.getJSONObject("stream");
                        String cmd = s.optString("cmd", "");
                        if ("start".equalsIgnoreCase(cmd)) {
                            int port = s.optInt("port", 6000);
                            int fps = s.optInt("fps", 12);
                            int maxWidth = s.optInt("maxWidth", 1280);
                            double q = s.optDouble("quality", 0.7);
                            try {
                                udpStreamer.startStreaming(clientInet, port, fps, maxWidth, (float) q);
                                if (serverCallback != null) serverCallback.onConnectionStatusChanged("Started streaming to " + clientInet.getHostAddress() + ":" + port);
                            } catch (Exception e) {
                                System.err.println("Failed to start UDP streaming: " + e.getMessage());
                                if (serverCallback != null) serverCallback.onConnectionStatusChanged("Streaming error: " + e.getMessage());
                            }
                        } else if ("stop".equalsIgnoreCase(cmd)) {
                            udpStreamer.stopStreaming();
                            if (serverCallback != null) serverCallback.onConnectionStatusChanged("Stopped streaming");
                        }
                        continue;
                    }

                    // WebSocket control
                    if (json.has("websocket") && webSocketStreamer != null) {
                        JSONObject s = json.getJSONObject("websocket");
                        String cmd = s.optString("cmd", "");
                        if ("start".equalsIgnoreCase(cmd)) {
                            int wsPort = s.optInt("port", 8080);
                            int fps = s.optInt("fps", 12);
                            int maxWidth = s.optInt("maxWidth", 1280);
                            double q = s.optDouble("quality", 0.7);
                            try {
                                webSocketStreamer.start(wsPort, fps, maxWidth, (float) q);
                                if (serverCallback != null) serverCallback.onConnectionStatusChanged("WebSocket server started on port " + wsPort + " (client should connect to ws://<DESKTOP_IP>:" + wsPort + ")");
                            } catch (Exception e) {
                                System.err.println("Failed to start WebSocket server: " + e.getMessage());
                                if (serverCallback != null) serverCallback.onConnectionStatusChanged("WebSocket error: " + e.getMessage());
                            }
                        } else if ("stop".equalsIgnoreCase(cmd)) {
                            webSocketStreamer.stop();
                            if (serverCallback != null) serverCallback.onConnectionStatusChanged("WebSocket server stopped");
                        }
                        continue;
                    }

                    if (json.has("keyboard") && keyboardController != null) {
                        JSONObject k = json.getJSONObject("keyboard");
                        String cmd = k.optString("cmd", "");
                        if ("type".equalsIgnoreCase(cmd)) {
                            String text = k.optString("text", "");
                            keyboardController.typeString(text);
                        } else if ("tap".equalsIgnoreCase(cmd)) {
                            int key = k.optInt("keyCode", -1);
                            if (key != -1) keyboardController.tapKey(key);
                        } else if ("press".equalsIgnoreCase(cmd)) {
                            int key = k.optInt("keyCode", -1);
                            if (key != -1) keyboardController.pressKey(key);
                        } else if ("release".equalsIgnoreCase(cmd)) {
                            int key = k.optInt("keyCode", -1);
                            if (key != -1) keyboardController.releaseKey(key);
                        }
                        continue;
                    }
                } catch (Exception e) {
                    // not a control JSON; fall through to motion parsing
                }

                MotionData data = parseJson(line);

                if (data != null) {
                    processor.handle(data);
                }
            }

        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try { if (client != null && !client.isClosed()) client.close(); } catch (Exception ignored) {}
            // Stop any active streaming associated with this client to avoid orphaned streams
            try {
                if (udpStreamer != null && udpStreamer.isStreaming()) {
                    udpStreamer.stopStreaming();
                }
            } catch (Exception ignored) {}

            try {
                if (webSocketStreamer != null && webSocketStreamer.isRunning()) {
                    webSocketStreamer.stop();
                }
            } catch (Exception ignored) {}

            synchronized (this) {
                if (currentClient == client) currentClient = null;
                currentClientHandler = null;
            }
            if (serverCallback != null) {
                serverCallback.onConnectionStatusChanged("Client disconnected");
            }
            System.out.println("Client disconnected: " + client);
        }
    }

    /**
     * Convert JSON string into MotionData object.
     */
    private MotionData parseJson(String line) {
        try {
            JSONObject json = new JSONObject(line);

            double gx = json.optDouble("gyroX", 0);
            double gy = json.optDouble("gyroY", 0);
            boolean left = json.optBoolean("leftClick", false);
            boolean right = json.optBoolean("rightClick", false);

            return new MotionData(gx, gy, left, right);

        } catch (Exception e) {
            System.err.println("Invalid JSON: " + line);
            return null;
        }
    }
}
