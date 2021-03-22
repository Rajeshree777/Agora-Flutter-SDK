package com.banuba.sdk.internal.utils;

import android.content.res.AssetManager;
import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;


public final class FileUtils {
    private static final int COPY_BUFFER_SIZE = 10240;
    private static final String ZIP_EXT = ".zip";


    private FileUtils() {
    }

    public static void
    copyAssets(AssetManager assetManager, File baseFolder, String path, List<String> assetsToCopy)
        throws IOException {
        String[] fileList = assetManager.list(path);
        if (requireNonNull(fileList).length == 0) {
            File file = new File(baseFolder, path);
            File parent = file.getParentFile();
            if (!parent.exists() && !file.getParentFile().mkdirs()) {
                throw new IOException(
                    "Failed to create " + parent + ". Check if you have `write` permissions");
            }
            try (InputStream inputStream = assetManager.open(path);
                 OutputStream outputStream = new FileOutputStream(file)) {
                processStreams(inputStream, outputStream);
            }
        } else {
            for (String children : fileList) {
                if (assetsToCopy.contains(children)) {
                    String fullPath = new File(path, children).getPath();
                    copyAssets(
                        assetManager, // clang-format break
                        baseFolder,
                        fullPath,
                        asList(requireNonNull(assetManager.list(fullPath))));
                }
            }
        }
    }

    private static boolean isFolderWithFiles(String filePath, AssetManager assetManager) {
        String[] files = null;
        try {
            files = assetManager.list(filePath);
        } catch (IOException e) {
            Logger.e(e.getMessage());
        }
        return files != null && files.length > 0;
    }


    private static void
    processStreams(@NonNull InputStream inputStream, @NonNull OutputStream outputStream)
        throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(inputStream);
             BufferedOutputStream out = new BufferedOutputStream(outputStream)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }


    @NonNull
    public static List<String> readLines(File file) {
        final List<String> lines = new ArrayList<>(10);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            final InputStreamReader isr = new InputStreamReader(fis);
            final BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            Logger.e(e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Logger.e(e.getMessage());
                }
            }
        }
        return lines;
    }


    /**
     * Read from assets shader files
     *
     * @param filename shader name
     * @return shader
     */
    @NonNull
    public static String readFromAssets(@NonNull String filename, AssetManager assetManager) {
        try {
            final BufferedReader reader =
                new BufferedReader(new InputStreamReader(assetManager.open(filename)));
            final StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = reader.readLine();
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
        return "";
    }

    public static void unzip(File zipFile) {
        String path = removeSuffix(zipFile.getPath(), ZIP_EXT);
        File dir = new File(path);
        dir.mkdir();
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File targetFile = new File(dir, removeSuffix(entry.getName(), File.separator));
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdir();
                }
                if (entry.isDirectory()) {
                    targetFile.mkdir();
                } else {
                    try (InputStream input = zip.getInputStream(entry);
                         OutputStream output = new FileOutputStream(targetFile)) {
                        processStreams(input, output);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("FileUtils unzip error :" + ex.getMessage());
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }


    private static String removeSuffix(String str, String suffix) {
        if (str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }
}
