package com.mousecontrol.communication;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * UDPStreamer
 *
 * Captures the primary screen, JPEG-encodes frames, fragments them into
 * small UDP packets and sends to a single client target.
 *
 * Lightweight, single-target streamer intended for local network (LAN) use.
 */
public class UDPStreamer {

    private final int fragmentSize; // bytes of JPEG payload per UDP packet
    private volatile boolean running = false;
    private Thread worker;
    private DatagramSocket socket;
    private InetAddress clientAddr;
    private int clientPort;
    private final AtomicInteger frameSeq = new AtomicInteger(0);

    public UDPStreamer(int fragmentSize) {
        this.fragmentSize = fragmentSize > 200 ? fragmentSize : 1000; // sane minimum
    }

    public synchronized void startStreaming(InetAddress addr, int port, int fps, int maxWidth, float quality) throws Exception {
        if (running) return; // already streaming
        this.clientAddr = addr;
        this.clientPort = port;
        this.socket = new DatagramSocket();
        this.running = true;

        worker = new Thread(() -> {
            try {
                Robot robot = new Robot();
                long frameIntervalMs = Math.max(1, 1000 / Math.max(1, fps));

                while (running) {
                    long start = System.currentTimeMillis();

                    try {
                        // capture full virtual screen (handles multi-monitor setups)
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

                        // overlay visible cursor marker onto the captured image
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

                        // encode JPEG
                        byte[] jpeg = encodeJpeg(capture, quality);

                        if (jpeg != null && jpeg.length > 0) {
                            sendFragments(jpeg, frameSeq.getAndIncrement());
                        }

                    } catch (Exception e) {
                        System.err.println("UDPStreamer capture/send error: " + e.getMessage());
                    }

                    long elapsed = System.currentTimeMillis() - start;
                    long toSleep = frameIntervalMs - elapsed;
                    if (toSleep > 0) {
                        try { Thread.sleep(toSleep); } catch (InterruptedException ignored) {}
                    }
                }

            } catch (Exception e) {
                System.err.println("UDPStreamer worker error: " + e.getMessage());
            } finally {
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
                running = false;
            }
        }, "udp-streamer-thread");

        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stopStreaming() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try { worker.join(500); } catch (InterruptedException ignored) {}
            worker = null;
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
    }

    public boolean isStreaming() {
        return running;
    }

    public InetAddress getClientAddr() {
        return clientAddr;
    }

    public int getClientPort() {
        return clientPort;
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

    private void sendFragments(byte[] jpeg, int seq) {
        try {
            int total = (jpeg.length + fragmentSize - 1) / fragmentSize;
            for (int i = 0; i < total; i++) {
                int offset = i * fragmentSize;
                int len = Math.min(fragmentSize, jpeg.length - offset);

                // header: 4 bytes magic, 1 byte version, 4 bytes seq, 2 bytes total, 2 bytes index
                ByteBuffer header = ByteBuffer.allocate(13);
                header.putInt(0x4D535452); // 'MSTR'
                header.put((byte)1); // version
                header.putInt(seq);
                header.putShort((short) total);
                header.putShort((short) i);

                byte[] packet = new byte[header.position() + len];
                System.arraycopy(header.array(), 0, packet, 0, header.position());
                System.arraycopy(jpeg, offset, packet, header.position(), len);

                DatagramPacket dp = new DatagramPacket(packet, packet.length, clientAddr, clientPort);
                socket.send(dp);
            }
        } catch (Exception e) {
            System.err.println("UDPStreamer sendFragments error: " + e.getMessage());
        }
    }
}

