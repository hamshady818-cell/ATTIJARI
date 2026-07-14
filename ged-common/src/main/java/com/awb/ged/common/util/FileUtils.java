package com.awb.ged.common.util;

import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.TechnicalException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>FileUtils</h1>
 * <p>
 * Utility class for common file manipulations, checksum calculations, and mime-type resolutions.
 * Designed to be framework-agnostic, relying only on JDK APIs.
 * </p>
 * <p>
 * This class is crucial for the document ingestion phase, helping check file integrity via SHA-256,
 * validate formats, and resolve metadata.
 * </p>
 */
public final class FileUtils {

    private static final int BUFFER_SIZE = 8192;
    
    // Built-in map of common extensions to MIME types as a fast, reliable offline fallback.
    private static final Map<String, String> EXTENSION_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("pdf", "application/pdf");
        map.put("doc", "application/msword");
        map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("xls", "application/vnd.ms-excel");
        map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        map.put("ppt", "application/vnd.ms-powerpoint");
        map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("png", "image/png");
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("gif", "image/gif");
        map.put("tiff", "image/tiff");
        map.put("tif", "image/tiff");
        map.put("txt", "text/plain");
        map.put("csv", "text/csv");
        map.put("json", "application/json");
        map.put("xml", "application/xml");
        map.put("zip", "application/zip");
        EXTENSION_MAP = Collections.unmodifiableMap(map);
    }

    private FileUtils() {
        // Prevent instantiation
    }

    /**
     * Calculates the SHA-256 checksum of an input stream.
     * Use this method for large files to avoid loading the entire content into memory.
     *
     * @param inputStream the stream to calculate checksum for
     * @return the SHA-256 hexadecimal string representation
     * @throws TechnicalException if the hashing algorithm is not available or reading the stream fails
     */
    public static String calculateChecksum(InputStream inputStream) {
        if (inputStream == null) {
            throw new TechnicalException(ErrorCode.INTERNAL_ERROR, "Input stream cannot be null for checksum calculation.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(ErrorCode.INTERNAL_ERROR, "SHA-256 algorithm not available.", e, null);
        } catch (IOException e) {
            throw new TechnicalException(ErrorCode.STORAGE_READ_ERROR, "Failed to read stream for checksum calculation.", e, null);
        }
    }

    /**
     * Calculates the SHA-256 checksum of a byte array.
     *
     * @param content the content to calculate checksum for
     * @return the SHA-256 hexadecimal string representation
     * @throws TechnicalException if the hashing algorithm is not available
     */
    public static String calculateChecksum(byte[] content) {
        if (content == null) {
            throw new TechnicalException(ErrorCode.INTERNAL_ERROR, "Content cannot be null for checksum calculation.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(ErrorCode.INTERNAL_ERROR, "SHA-256 algorithm not available.", e, null);
        }
    }

    /**
     * Detects the MIME type of a file based on its filename or path using standard Java capabilities.
     * Falls back to a predefined map of enterprise formats if the filesystem probe fails.
     *
     * @param path the path to the file
     * @return the MIME type string (e.g. "application/pdf"), or "application/octet-stream" if undetermined
     */
    public static String detectMimeType(Path path) {
        if (path == null) {
            return "application/octet-stream";
        }
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null && !mimeType.isEmpty()) {
                return mimeType;
            }
        } catch (IOException ignored) {
            // Fallback to filename-based detection
        }
        return detectMimeType(path.getFileName().toString());
    }

    /**
     * Detects the MIME type based on filename extension.
     *
     * @param filename the name of the file
     * @return the MIME type string, or "application/octet-stream" if undetermined
     */
    public static String detectMimeType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        String extension = getFileExtension(filename).toLowerCase();
        return EXTENSION_MAP.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Extracts the extension of a file from its name.
     *
     * @param filename the name of the file (e.g. "invoice.pdf")
     * @return the file extension (e.g. "pdf"), or empty string if no extension exists
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastDotIndex > lastSeparatorIndex && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Extracts the base name of a file from its name (removes the directory path and extension).
     *
     * @param filename the name of the file (e.g. "C:/docs/invoice.pdf")
     * @return the base filename without path or extension (e.g. "invoice")
     */
    public static String getBaseName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        // Normalize separators and strip path
        String cleanName = filename.replace('\\', '/');
        int lastSlashIndex = cleanName.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            cleanName = cleanName.substring(lastSlashIndex + 1);
        }
        // Strip extension
        int lastDotIndex = cleanName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            cleanName = cleanName.substring(0, lastDotIndex);
        }
        return cleanName;
    }

    /**
     * Formats a raw byte size into a human-readable string (e.g. "2.5 MB", "420 KB").
     *
     * @param sizeInBytes size of the file in bytes
     * @return human-readable formatted size
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(sizeInBytes)/Math.log10(1024));
        return String.format("%.1f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
