package org.example.logger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.model.LogStructure;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
@Setter
public class LogDispatcher implements Runnable{

    private final BlockingQueue<LogStructure> blockingQueue = new LinkedBlockingDeque<>();
    private volatile boolean running = true;
    private final FileRotator rotator;

    public boolean enqueue(LogStructure msg) {
        if (!running) {
            return false;
        }
        return blockingQueue.offer(msg);
    }

    @Override
    public void run() {
        try {
            while (running || !blockingQueue.isEmpty()) {
                try {
                    LogStructure msg = blockingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        rotator.write(msg);
                    }
                } catch (Exception ex) {
                    System.err.println("Error writing log: " + ex.getMessage());

                }
            }
        } finally {
            try {
                rotator.close();
            } catch (Exception ex) {
                System.err.println("Error closing rotator: " + ex.getMessage());

            }
        }
    }

    public void shutdown(){
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
