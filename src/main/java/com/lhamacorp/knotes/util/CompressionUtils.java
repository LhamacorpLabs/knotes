package com.lhamacorp.knotes.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for GZIP compression and decompression of text content.
 * Provides transparent compression for note content to reduce database storage size.
 */
public class CompressionUtils {

    /**
     * Compresses a string using GZIP compression.
     *
     * @param input the string to compress
     * @return compressed byte array, or null if input is null/empty
     * @throws RuntimeException if compression fails
     */
    public static byte[] compress(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {

            gzip.write(input.getBytes(StandardCharsets.UTF_8));
            gzip.close();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress text content", e);
        }
    }

    /**
     * Decompresses a GZIP compressed byte array back to a string.
     *
     * @param compressed the compressed byte array
     * @return decompressed string, or empty string if input is null/empty
     * @throws RuntimeException if decompression fails
     */
    public static String decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return "";
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzip = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress text content", e);
        }
    }

    /**
     * Calculates the compression ratio as a percentage.
     *
     * @param originalSize   original content size in bytes
     * @param compressedSize compressed content size in bytes
     * @return compression ratio (e.g., 0.75 means 75% reduction)
     */
    public static double getCompressionRatio(int originalSize, int compressedSize) {
        if (originalSize == 0) return 0.0;
        return 1.0 - ((double) compressedSize / originalSize);
    }
}