package org.example;

import org.example.logger.LogDispatcher;
import org.example.logger.MyLogger;
import org.example.model.LogStructure;
import org.example.server.LogWebSocketServer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) {
        BlockingQueue<LogStructure> queue = new LinkedBlockingQueue<>();
        LogWebSocketServer ws = new LogWebSocketServer(9090);

        ws.start();
        System.out.println("WebSocket server started on ws://localhost:9090");
        System.out.println("Open dashboard.html in your browser");

        LogDispatcher dispatcher = new LogDispatcher(queue, ws);
        MyLogger logger = new MyLogger(dispatcher, queue);

        Thread dispatcherThread = new Thread(dispatcher);
        dispatcherThread.setName("LogDispatcher");
        dispatcherThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Application started successfully");
        logger.info("Initializing components...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.warn("Cache response time is slow: 250ms");
        logger.debug("Loading configuration from config.yml");
        logger.error("Database connection failed: timeout after 30s");
        logger.info("Retrying database connection...");
        logger.warn("Memory usage is high: 85%");
        logger.debug("Processing request from user: admin");
        logger.info("Request processed successfully");

        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(2000);
                logger.info("check #" + (i + 1));
                if (i == 2) {
                    logger.warn("Network latency detected: 150ms");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Shutting down application");
        try {
            dispatcher.shutdownAndWait(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            ws.stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Application stopped");
    }
}