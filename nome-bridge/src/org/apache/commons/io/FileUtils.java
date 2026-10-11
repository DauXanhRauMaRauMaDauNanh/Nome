package org.apache.commons.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {
    public static String readFileToString(File file, Charset encoding) throws IOException {
        return Files.readString(file.toPath(), encoding != null ? encoding : StandardCharsets.UTF_8);
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public static void writeStringToFile(File file, String data, Charset encoding) throws IOException {
        Files.writeString(file.toPath(), data, encoding != null ? encoding : StandardCharsets.UTF_8);
    }

    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        Files.write(file.toPath(), data);
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
