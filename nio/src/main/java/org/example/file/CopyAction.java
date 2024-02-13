package org.example.file;

import java.util.List;
import java.util.Map;

public interface CopyAction {
    void execute(String source, String target);
    void execute(String source, String target, Map<String, List<FileError>> exceptionInfo);
}
