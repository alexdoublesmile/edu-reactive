package org.example.file.exception;

import org.example.file.FileError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.Instant.now;

public final class ExceptionHelper {
    public static void handleException(
            FileError.FileErrorType errorType,
            String key,
            String message,
            Throwable ex,
            Map<String, List<FileError>> exceptionInfo
    ) {
        System.err.println(message);
        exceptionInfo.getOrDefault(key, new ArrayList<>()).add(FileError.builder()
                .type(errorType)
                .message(message)
                .timestamp(now())
                .cause(ex)
                .build());
    }
}
