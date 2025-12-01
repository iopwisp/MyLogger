package org.example.logger;

import org.example.model.LogStructure;
import org.example.server.LogWebSocketServer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class LogDispatcher implements Runnable {

    private final BlockingQueue<LogStructure> blockingQueue;
    private final LogWebSocketServer webSocketServer;
    private volatile boolean running = true;
    private final FileRotator rotator;

    public LogDispatcher(BlockingQueue<LogStructure> queue, LogWebSocketServer ws) {
        this.blockingQueue = queue;
        this.webSocketServer = ws;
        this.rotator = new FileRotator("logs/app.log", 10 * 1024 * 1024); // 10MB rotation
    }

    public boolean enqueue(LogStructure msg) {
        if (!running) {
            return false;
        }
        try {
            return blockingQueue.offer(msg, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void run() {
        System.out.println("LogDispatcher started");
        try {
            while (running || !blockingQueue.isEmpty()) {
                try {
                    LogStructure msg = blockingQueue.poll(100, TimeUnit.MILLISECONDS);

                    if (msg != null) {
                        String logString = msg.toString();
                        webSocketServer.broadcast(logString);
                        System.out.println(logString);
                        rotator.write(msg);
                    }
                } catch (Exception ex) {
                    System.err.println("Error processing log: " + ex.getMessage());
                }
            }
        } finally {
            try {
                rotator.close();
                System.out.println("LogDispatcher stopped");
            } catch (Exception ex) {
                System.err.println("Error closing rotator: " + ex.getMessage());
            }
        }
    }

    public void shutdown() {
        running = false;
    }

    public void shutdownAndWait(long timeoutMs) throws InterruptedException {
        running = false;
        long start = System.currentTimeMillis();
        while (!blockingQueue.isEmpty() &&
                (System.currentTimeMillis() - start) < timeoutMs) {
            Thread.sleep(50);
        }
    }
}