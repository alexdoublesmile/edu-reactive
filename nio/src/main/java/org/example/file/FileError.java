package org.example.file;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FileError {
    private FileErrorType type;
    private Instant timestamp;
    private String message;
    private Throwable cause;

    public enum FileErrorType {
        OPEN_ERROR,
        COPY_ERROR,
        READ_ERROR,
        WRITE_ERROR,
        INTEGRITY_ERROR,
        RETRY_WAITING_ERROR,
        MOVE_ERROR
    }
}
