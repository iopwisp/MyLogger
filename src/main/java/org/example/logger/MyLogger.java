package org.example.logger;

import org.example.model.LogLevel;
import org.example.model.LogStructure;

import java.util.concurrent.BlockingQueue;

public class MyLogger {

    private final LogDispatcher logDispatcher;
    private final BlockingQueue<LogStructure> queue;

    public MyLogger(LogDispatcher logDispatcher, BlockingQueue<LogStructure> queue) {
        this.logDispatcher = logDispatcher;
        this.queue = queue;
    }

    public void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public void error(String msg) {
        log(LogLevel.ERROR, msg);
    }

    public void debug(String msg) {
        log(LogLevel.DEBUG, msg);
    }

    private void log(LogLevel level, String msg) {
        try {
            LogStructure logStructure = new LogStructure(
                    level,
                    msg,
                    System.currentTimeMillis(),
                    Thread.currentThread().getName()
            );

            boolean enqueued = logDispatcher.enqueue(logStructure);

            if (!enqueued) {
                System.err.println("Failed to enqueue log message: " + msg);
            }
        } catch (Exception ex) {
            System.err.println("Error logging message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}