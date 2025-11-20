package org.example;

import org.example.logger.FileRotator;
import org.example.logger.LogDispatcher;
import org.example.logger.MyLogger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Thread dispatcherThread = null;

        try {
            Path logDir = Paths.get("logs");
            long maxFileSizeBytes = 10 * 1024 * 1024;

            FileRotator rotator = new FileRotator(logDir, maxFileSizeBytes);
            LogDispatcher dispatcher = new LogDispatcher(rotator);
            MyLogger logger = new MyLogger(dispatcher);

            dispatcherThread = new Thread(dispatcher, "LogDispatcher-Thread");
            dispatcherThread.start();

            logger.info("Application started");
            logger.debug("Debug information");
            logger.warn("Warning message");
            logger.error("Error occurred");

            for (int i = 0; i < 100; i++) {
                logger.info("Processing item " + i);
                Thread.sleep(10);
            }

            logger.info("Application finished successfully");

        } catch (Exception ex) {
            System.err.println("Application error: " + ex.getMessage());
            ex.getStackTrace();
        } finally {
            if (dispatcherThread != null) {
                try {
                    LogDispatcher dispatcher = (LogDispatcher)
                            ((Runnable) dispatcherThread).getClass()
                                    .getDeclaredField("this$0").get(dispatcherThread);

                    dispatcher.shutdownAndWait(5000);

                    dispatcherThread.join(1000);

                    System.out.println("Logger shutdown completed");
                } catch (Exception ex) {
                    System.err.println("Error during shutdown: " + ex.getMessage());
                }
            }
        }
    }
}