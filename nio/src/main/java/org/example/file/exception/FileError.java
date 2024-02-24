package org.example.file.exception;

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

    @Override
    public String toString() {
        return
                "- type=" + type +
                "\n- timestamp=" + timestamp +
                "\n- message='" + message +
                '\n';
    }

    public enum FileErrorType {
        CREATE_ERROR,
        EXIST_ERROR,
        OPEN_ERROR,
        READ_ERROR,
        HASH_ERROR,
        WRITE_ERROR,
        COPY_ERROR,
        COPY_INTEGRITY_ERROR,
        RETRY_WAITING_ERROR,
        MOVE_ERROR
    }
}
