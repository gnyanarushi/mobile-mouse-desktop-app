package com.mousecontrol;


import com.mousecontrol.communication.TCPServer;
import com.mousecontrol.controller.MouseController;
import com.mousecontrol.processor.MovementProcessor;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args)  {


        System.out.println("Starting Mouse Controller App...");

        try {
            MouseController mouse = new MouseController();
            MovementProcessor processor = new MovementProcessor(mouse);

            // Tuning defaults
            processor.setSensitivity(12.0);
            processor.setSmoothing(0.25);
            processor.setDeadZone(0.02);

            // Start TCP Server
            TCPServer server = new TCPServer(5000, processor);
            server.start();

            System.out.println("Waiting for connection from Flutter...");

            // Keep app running
            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled exception in main", e);
        }


    }
}
