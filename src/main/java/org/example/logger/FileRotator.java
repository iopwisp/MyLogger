package org.example.logger;

import lombok.Getter;
import lombok.Setter;
import org.example.interfaces.FileRotatorInterface;
import org.example.model.LogStructure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Setter
@Getter
public class FileRotator implements FileRotatorInterface {

    private final long maxSizeBytes;
    private final Path logDir;
    private BufferedWriter writer;
    private int fileIndex = 0;
    private long currentFileSize = 0;

    public FileRotator(Path logDir, long maxSizeBytes) throws IOException {
        this.logDir = logDir;
        this.maxSizeBytes = maxSizeBytes;

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        openNewFile();
    }

    private void openNewFile() throws IOException {
        if (writer != null) writer.close();
        Path file = logDir.resolve("log_%d.txt".formatted(fileIndex++));
        writer = Files.newBufferedWriter(file
                , StandardOpenOption.CREATE
                , StandardOpenOption.APPEND);
        if (Files.exists(file)) {
            currentFileSize = Files.size(file);
        } else {
            currentFileSize = 0;
        }
    }

    public synchronized void write(LogStructure msg) throws IOException {
        if (writer == null) {
            throw new IOException("FileRotator is closed");
        }
        String line = msg.toString();
        writer.write(line);
        writer.newLine();
        writer.flush();

        currentFileSize += line.getBytes().length + System.lineSeparator().length();

        if (currentFileSize > maxSizeBytes) {
            openNewFile();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } finally {
                writer = null;
            }
        }
    }

}
