package com.mousecontrol.communication;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

/**
 * WebSocketStreamer
 *
 * Simple WebSocket server that accepts one client and streams JPEG frames as binary WebSocket messages.
 * Uses Java-WebSocket (org.java-websocket) library. If the dependency isn't present, user will need to add it.
 */
public class WebSocketStreamer {

    private volatile boolean running = false;
    private SimpleWsServer server;
    private final AtomicInteger frameSeq = new AtomicInteger(0);

    public void start(int port, int fps, int maxWidth, float quality) throws Exception {
        if (running) return;
        server = new SimpleWsServer(new InetSocketAddress(port), fps, maxWidth, quality);
        server.start();
        running = true;
        System.out.println("WebSocketStreamer started on port " + port);
    }

    public void stop() {
        running = false;
        if (server != null) {
            try { server.stop(1000); } catch (Exception ignored) {}
            server = null;
        }
    }

    public boolean isRunning() { return running; }

    private static class SimpleWsServer extends WebSocketServer {
        private final int fps;
        private final int maxWidth;
        private final float quality;
        private volatile WebSocket clientSocket;
        private Thread worker;

        public SimpleWsServer(InetSocketAddress addr, int fps, int maxWidth, float quality) {
            super(addr);
            this.fps = fps;
            this.maxWidth = maxWidth;
            this.quality = quality;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("WS client connected: " + conn.getRemoteSocketAddress());
            this.clientSocket = conn;
            startWorker();
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("WS client disconnected: " + conn.getRemoteSocketAddress());
            this.clientSocket = null;
            stopWorker();
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // optional: handle control messages from client
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("WS error: " + ex.getMessage());
        }

        @Override
        public void onStart() {
            System.out.println("WS server started and listening");
        }

        private void startWorker() {
            if (worker != null && worker.isAlive()) return;
            worker = new Thread(() -> {
                try {
                    Robot robot = new Robot();
                    long frameIntervalMs = Math.max(1, 1000 / Math.max(1, fps));
                    while (clientSocket != null && clientSocket.isOpen()) {
                        long start = System.currentTimeMillis();

                        try {
                            // capture full virtual screen to include all monitors
                            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                            GraphicsDevice[] devices = ge.getScreenDevices();

                            int minX = Integer.MAX_VALUE;
                            int minY = Integer.MAX_VALUE;
                            int maxX = Integer.MIN_VALUE;
                            int maxY = Integer.MIN_VALUE;

                            for (GraphicsDevice device : devices) {
                                Rectangle b = device.getDefaultConfiguration().getBounds();
                                minX = Math.min(minX, b.x);
                                minY = Math.min(minY, b.y);
                                maxX = Math.max(maxX, b.x + b.width);
                                maxY = Math.max(maxY, b.y + b.height);
                            }

                            Rectangle screenRect = new Rectangle(minX, minY, maxX - minX, maxY - minY);
                            BufferedImage capture = robot.createScreenCapture(screenRect);

                            // overlay visible cursor marker
                            try {
                                PointerInfo pinfo = MouseInfo.getPointerInfo();
                                if (pinfo != null) {
                                    Point p = pinfo.getLocation();
                                    int rx = p.x - screenRect.x;
                                    int ry = p.y - screenRect.y;
                                    Graphics2D g = capture.createGraphics();
                                    g.setColor(Color.RED);
                                    int size = Math.max(8, Math.min(24, capture.getWidth() / 80));
                                    g.fillOval(rx - size/2, ry - size/2, size, size);
                                    g.setColor(Color.WHITE);
                                    g.drawOval(rx - size/2, ry - size/2, size, size);
                                    g.dispose();
                                }
                            } catch (Exception e) {
                                // ignore cursor overlay errors
                            }

                            // scale if needed
                            if (maxWidth > 0 && capture.getWidth() > maxWidth) {
                                int newW = maxWidth;
                                int newH = (int) (((double) capture.getHeight() / capture.getWidth()) * newW);
                                Image tmp = capture.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                                BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                                scaled.getGraphics().drawImage(tmp, 0, 0, null);
                                capture = scaled;
                            }

                            byte[] jpeg = encodeJpeg(capture, quality);
                            if (jpeg != null && jpeg.length > 0 && clientSocket != null && clientSocket.isOpen()) {
                                clientSocket.send(jpeg);
                            }

                        } catch (Exception e) {
                            System.err.println("WS worker capture/send error: " + e.getMessage());
                        }

                        long elapsed = System.currentTimeMillis() - start;
                        long toSleep = frameIntervalMs - elapsed;
                        if (toSleep > 0) {
                            try { Thread.sleep(toSleep); } catch (InterruptedException ignored) {}
                        }
                    }
                } catch (Exception e) {
                    System.err.println("WS worker fatal: " + e.getMessage());
                }
            }, "ws-streamer-thread");
            worker.setDaemon(true);
            worker.start();
        }

        private void stopWorker() {
            if (worker != null) {
                worker.interrupt();
                try { worker.join(500); } catch (InterruptedException ignored) {}
                worker = null;
            }
        }

        private byte[] encodeJpeg(BufferedImage img, float quality) throws Exception {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                if (!writers.hasNext()) {
                    ImageIO.write(img, "jpg", baos);
                    return baos.toByteArray();
                }
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
                }
                MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos);
                writer.setOutput(mcios);
                writer.write(null, new IIOImage(img, null, null), param);
                mcios.close();
                writer.dispose();
                return baos.toByteArray();
            } catch (Exception e) {
                throw new Exception("JPEG encode failed: " + e.getMessage(), e);
            }
        }
    }
}

