package com.mousecontrol.communication;

import com.mousecontrol.models.MotionData;
import com.mousecontrol.processor.MovementProcessor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCPServer
 *
 * Runs in the background.
 * Waits for a connection from the Flutter mobile app.
 * Reads JSON messages line-by-line.
 * Passes MotionData to MovementProcessor.
 */
public class TCPServer {

    private final int port;
    private final MovementProcessor processor;

    public TCPServer(int port, MovementProcessor processor) {
        this.port = port;
        this.processor = processor;
    }

    /**
     * Starts the server in a background thread.
     */
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("TCP Server running on port " + port);

                // Accept a single connection (your phone)
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getRemoteSocketAddress());

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {

                    // DEBUG:
                    // System.out.println("Received: " + line);

                    MotionData data = parseJson(line);

                    if (data != null) {
                        processor.handle(data);
                    }
                }

            } catch (Exception e) {
                System.err.println("TCP Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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
