package org.example.util;

public final class PrintUtil {
    public static void printProgress(long processed, long total) {
        double progress = ((double) processed / total) * 100;
        System.out.printf("\rProgress: %.1f%% ", progress);
    }
}
