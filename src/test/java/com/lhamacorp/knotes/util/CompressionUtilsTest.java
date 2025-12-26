package com.lhamacorp.knotes.util;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CompressionUtilsTest {

    @Test
    void compressAndDecompress_shouldReturnOriginalText() {
        // Given
        String originalText = "This is a test note with some content that should compress well. " +
                             "This is repeated text. This is repeated text. This is repeated text.";

        // When
        byte[] compressed = CompressionUtils.compress(originalText);
        String decompressed = CompressionUtils.decompress(compressed);

        // Then
        assertEquals(originalText, decompressed);
        assertTrue(compressed.length < originalText.getBytes().length,
                  "Compressed size should be smaller than original");
    }

    @Test
    void compress_withNullInput_shouldReturnEmptyArray() {
        // When
        byte[] result = CompressionUtils.compress(null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void compress_withEmptyInput_shouldReturnEmptyArray() {
        // When
        byte[] result = CompressionUtils.compress("");

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void decompress_withNullInput_shouldReturnEmptyString() {
        // When
        String result = CompressionUtils.decompress(null);

        // Then
        assertEquals("", result);
    }

    @Test
    void decompress_withEmptyInput_shouldReturnEmptyString() {
        // When
        String result = CompressionUtils.decompress(new byte[0]);

        // Then
        assertEquals("", result);
    }

    @Test
    void getCompressionRatio_shouldCalculateCorrectly() {
        // Given
        String text = "This is some text that will be compressed for testing purposes. " +
                     "Repeated content. Repeated content. Repeated content.";

        // When
        byte[] compressed = CompressionUtils.compress(text);
        double ratio = CompressionUtils.getCompressionRatio(
            text.getBytes().length,
            compressed.length
        );

        // Then
        assertTrue(ratio > 0.0, "Should have some compression ratio");
        assertTrue(ratio < 1.0, "Compression ratio should be less than 1.0");

        System.out.println("Basic Text Test:");
        System.out.println("Original size: " + text.getBytes().length + " bytes");
        System.out.println("Compressed size: " + compressed.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%", ratio * 100));
    }

    @Test
    void compress_largeRepeatingText_shouldAchieveHighCompressionRatio() {
        // Given - Large text with lots of repetition (typical of notes with repeated patterns)
        StringBuilder sb = new StringBuilder();
        String pattern = "This is a repeated pattern in a note that demonstrates compression. ";
        for (int i = 0; i < 50; i++) {
            sb.append(pattern);
        }
        String largeText = sb.toString();

        // When
        byte[] compressed = CompressionUtils.compress(largeText);
        String decompressed = CompressionUtils.decompress(compressed);
        double ratio = CompressionUtils.getCompressionRatio(largeText.getBytes().length, compressed.length);

        // Then
        assertEquals(largeText, decompressed, "Decompressed text should match original");
        assertTrue(ratio > 0.8, "Should achieve high compression ratio for repetitive text");

        System.out.println("\nLarge Repetitive Text Test:");
        System.out.println("Original size: " + largeText.getBytes().length + " bytes");
        System.out.println("Compressed size: " + compressed.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%", ratio * 100));
        System.out.println("Size reduction: " + (largeText.getBytes().length - compressed.length) + " bytes saved");
    }

    @Test
    void compress_jsonLikeContent_shouldCompressWell() {
        // Given - JSON-like content (common in technical notes)
        String jsonContent = """
            {
              "project": "kNotes",
              "technologies": ["Java", "Spring Boot", "MongoDB", "JavaScript"],
              "features": {
                "compression": true,
                "dark_mode": true,
                "auto_save": true
              },
              "description": "A note-taking application with GZIP compression",
              "metadata": {
                "created": "2024",
                "author": "Developer",
                "version": "1.0"
              }
            }
            """.repeat(10); // Repeat to make it larger

        // When
        byte[] compressed = CompressionUtils.compress(jsonContent);
        String decompressed = CompressionUtils.decompress(compressed);
        double ratio = CompressionUtils.getCompressionRatio(jsonContent.getBytes().length, compressed.length);

        // Then
        assertEquals(jsonContent, decompressed);
        assertTrue(ratio > 0.5, "JSON content should compress well due to repeated patterns");

        System.out.println("\nJSON-like Content Test:");
        System.out.println("Original size: " + jsonContent.getBytes().length + " bytes");
        System.out.println("Compressed size: " + compressed.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%", ratio * 100));
    }

    @Test
    void compress_codeSnippet_shouldPreserveFormatting() {
        // Given - Code snippet (common content in developer notes)
        String codeSnippet = """
            public class Example {
                private String value;

                public Example(String value) {
                    this.value = value;
                }

                public String getValue() {
                    return this.value;
                }

                public void setValue(String value) {
                    this.value = value;
                }

                @Override
                public String toString() {
                    return "Example{value='" + value + "'}";
                }
            }
            """;

        // When
        byte[] compressed = CompressionUtils.compress(codeSnippet);
        String decompressed = CompressionUtils.decompress(compressed);

        // Then
        assertEquals(codeSnippet, decompressed, "Code formatting should be preserved exactly");
        assertTrue(compressed.length < codeSnippet.getBytes().length, "Code should still compress");

        System.out.println("\nCode Snippet Test:");
        System.out.println("Original size: " + codeSnippet.getBytes().length + " bytes");
        System.out.println("Compressed size: " + compressed.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%",
            CompressionUtils.getCompressionRatio(codeSnippet.getBytes().length, compressed.length) * 100));
    }

    @Test
    void compress_mixedContent_shouldHandleVariousCharacters() {
        // Given - Mixed content with various characters (emoji, unicode, special chars)
        String mixedContent = """
            📝 Note Title: Project Planning

            ✅ Tasks completed:
            • Database setup ✓
            • API endpoints ✓
            • Frontend components ✓

            🔄 In Progress:
            • Testing & validation
            • Performance optimization
            • Documentation updates

            💡 Ideas for improvement:
            - Add real-time collaboration
            - Implement version history
            - Mobile app development

            Special characters: àáâãäåæçèéêë ñ ø ß ü ÿ
            Math symbols: α β γ δ ∑ ∫ ∆ π ∞
            """;

        // When
        byte[] compressed = CompressionUtils.compress(mixedContent);
        String decompressed = CompressionUtils.decompress(compressed);

        // Then
        assertEquals(mixedContent, decompressed, "Mixed content with unicode should be preserved");
        assertTrue(compressed.length < mixedContent.getBytes(StandardCharsets.UTF_8).length, "Should still achieve compression");

        System.out.println("\nMixed Content with Unicode Test:");
        System.out.println("Original size: " + mixedContent.getBytes().length + " bytes");
        System.out.println("Compressed size: " + compressed.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%",
            CompressionUtils.getCompressionRatio(mixedContent.getBytes().length, compressed.length) * 100));
    }
}