package org.example.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.example.file.exception.FileError;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.example.file.exception.ExceptionHelper.handleException;
import static org.example.file.exception.FileError.FileErrorType.EXIST_ERROR;
import static org.example.file.exception.FileError.FileErrorType.HASH_ERROR;

public final class HashHelper {

    public static String getFileHash(String filePath, Map<String, List<FileError>> exceptionInfo) {
        String hash = "";
        try {
            hash = DigestUtils.sha256Hex(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            handleException(EXIST_ERROR, filePath, format("File %s doesn't exist", filePath), e, exceptionInfo);
        } catch (IOException e) {
            handleException(HASH_ERROR, filePath, format("Failed to calculate hash from %s", filePath), e, exceptionInfo);
        }
        return hash;
    }
}
