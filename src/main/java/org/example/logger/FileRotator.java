package org.example.logger;

import org.example.model.LogStructure;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileRotator {
    private final String basePath;
    private final long maxFileSize;
    private BufferedWriter writer;
    private long currentSize;

    public FileRotator(String basePath, long maxFileSize) {
        this.basePath = basePath;
        this.maxFileSize = maxFileSize;
        this.currentSize = 0;
        initWriter();
    }

    private void initWriter() {
        try {
            Path path = Paths.get(basePath);
            Files.createDirectories(path.getParent());

            File file = path.toFile();
            if (file.exists()) {
                currentSize = file.length();
            }

            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }

    public synchronized void write(LogStructure log) {
        if (writer == null) {
            return;
        }

        try {
            String logLine = log.toString() + System.lineSeparator();
            writer.write(logLine);
            writer.flush();

            currentSize += logLine.getBytes().length;

            if (currentSize >= maxFileSize) {
                rotate();
            }
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    private void rotate() throws IOException {
        close();

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rotatedPath = basePath.replace(".log", "_" + timestamp + ".log");

        Path source = Paths.get(basePath);
        Path target = Paths.get(rotatedPath);

        if (Files.exists(source)) {
            Files.move(source, target);
        }

        currentSize = 0;
        initWriter();
    }

    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("Failed to close log file: " + e.getMessage());
            }
            writer = null;
        }
    }
}