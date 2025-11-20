package org.example;

import org.example.logger.FileRotator;
import org.example.logger.LogDispatcher;
import org.example.logger.MyLogger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoggerTest {

    @TempDir
    Path logDir;

    private ExecutorService dispatcherPool;
    private LogDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcherPool = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
        if (dispatcherPool != null) {
            dispatcherPool.shutdownNow();
            dispatcherPool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void startDispatcher(FileRotator rotator) {
        dispatcher = new LogDispatcher(rotator);
        dispatcherPool.submit(dispatcher);
    }

    @Test
    void testSimpleLogging() throws Exception {
        FileRotator rotator = new FileRotator(logDir, 1024 * 1024);
        startDispatcher(rotator);
        MyLogger log = new MyLogger(dispatcher);

        log.info("info");
        log.error("error");

        awaitFileCount(1, 5, TimeUnit.SECONDS);

        Path logFile = Files.list(logDir).findFirst().orElseThrow();
        List<String> lines = Files.readAllLines(logFile);

        assertTrue(lines.stream().anyMatch(l -> l.contains("info")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("error")));
    }

    @Test
    void testRotation() throws Exception {
        FileRotator rotator = new FileRotator(logDir, 200);
        startDispatcher(rotator);
        MyLogger log = new MyLogger(dispatcher);

        for (int i = 0; i < 100; i++) {
            log.info("Rotation test message " + i);
        }

        awaitFileCount(2, 10, TimeUnit.SECONDS);

        long files = Files.list(logDir).count();
        assertTrue(files >= 2, "Expected at least 2 log files after rotation, got " + files);
    }

    @Test
    void testMultithreading() throws Exception {
        FileRotator rotator = new FileRotator(logDir, 5 * 1024 * 1024);
        startDispatcher(rotator);
        MyLogger log = new MyLogger(dispatcher);

        ExecutorService workers = Executors.newFixedThreadPool(10);
        int tasks = 10;
        int messagesPerTask = 200;

        for (int i = 0; i < tasks; i++) {
            workers.submit(() -> {
                for (int j = 0; j < messagesPerTask; j++) {
                    log.info(Thread.currentThread().getName() + " message " + j);
                }
            });
        }

        workers.shutdown();
        assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS));

        awaitFileCount(1, 10, TimeUnit.SECONDS);

        try (Stream<Path> files = Files.list(logDir)) {
            List<String> allLines = files
                    .filter(Files::isRegularFile)
                    .flatMap(p -> {
                        try {
                            return Files.lines(p);
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    }).toList();

            assertTrue(allLines.stream().anyMatch(l -> l.contains("message")));
            assertTrue(allLines.size() >= tasks * messagesPerTask * 0.9,
                    "Most messages from different threads should be present");
        }
    }

    private void awaitFileCount(int minCount, long timeout, TimeUnit unit) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (Files.list(logDir).count() >= minCount) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        fail("Timeout waiting for at least " + minCount + " log file(s)");
    }
}