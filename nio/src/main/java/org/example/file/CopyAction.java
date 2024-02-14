package org.example.file;

import org.example.file.exception.FileError;

import java.util.List;
import java.util.Map;

public interface CopyAction {
    void execute(String source, String target);
    void execute(String source, String target, Map<String, List<FileError>> exceptionInfo);
}
