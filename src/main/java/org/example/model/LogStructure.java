package org.example.model;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record LogStructure(LogLevel logLevel,
                           String message,
                           long localDateTime,
                           String threadName) {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    @NotNull
    @Override
    public String toString() {
        String formattedTime = FORMATTER.format(Instant.ofEpochMilli(localDateTime));
        return String.format("[%s] [%s] [%s] %s",
                formattedTime,
                logLevel,
                threadName,
                message);
    }
}
