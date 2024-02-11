package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            FileUtil.copy("nio/src/main/resources/video.mp4", "nio/src/main/resources/result.mp4");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}