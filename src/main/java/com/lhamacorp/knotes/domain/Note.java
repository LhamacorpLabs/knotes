package com.lhamacorp.knotes.domain;

import com.lhamacorp.knotes.exception.DecryptionException;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import com.lhamacorp.knotes.util.CompressionUtils;
import com.lhamacorp.knotes.util.EncryptionUtils;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

import static com.lhamacorp.knotes.domain.EncryptionMode.PUBLIC;

@Document("notes")
public record Note(
        @Id String id,
        @Field("content") Binary compressedData,
        String createdBy,
        Instant createdAt,
        Instant modifiedAt,
        EncryptionMode encryptionMode,
        @Field("salt") Binary encryptionSalt,
        Boolean requiresPassword,
        String color,
        Boolean pinned
) {

    public static final String ANONYMOUS = "1";

    public Note(String id, Binary compressedData, String createdBy, Instant createdAt, Instant modifiedAt,
                EncryptionMode encryptionMode, Binary encryptionSalt, Boolean requiresPassword) {
        this(id, compressedData, createdBy, createdAt, modifiedAt, encryptionMode, encryptionSalt, requiresPassword, null, null);
    }

    public Note(String id, String content, String createdBy, Instant createdAt, Instant modifiedAt) {
        this(id, content, createdBy, createdAt, modifiedAt, PUBLIC, null);
    }

    public Note(String id, String content, String createdBy, Instant createdAt, Instant modifiedAt, EncryptionMode encryptionMode, String password) {
        EncryptionMode mode = encryptionMode != null ? encryptionMode : PUBLIC;

        if (createdBy == null || createdBy.equals(ANONYMOUS)) {
            mode = PUBLIC;
        }

        byte[] salt = null;
        if (mode != PUBLIC) {
            salt = EncryptionUtils.generateSalt();
        }

        Binary processedContent = processContent(content, mode, createdBy, password, salt);
        Binary storedSalt = salt != null ? new Binary(salt) : null;

        this(id, processedContent, createdBy, createdAt, modifiedAt, mode, storedSalt, mode == EncryptionMode.PASSWORD_SHARED, null, null);
    }

    /**
     * Returns a copy with new display metadata. Content, encryption and timestamps are untouched,
     * so changing a color or pin does not count as a content edit.
     */
    public Note withDisplay(String color, Boolean pinned) {
        return new Note(id, compressedData, createdBy, createdAt, modifiedAt, encryptionMode,
                encryptionSalt, requiresPassword, color, pinned);
    }

    public Note withDisplayFrom(Note other) {
        return withDisplay(other.color(), other.pinned());
    }

    public String content() {
        return content(null, null);
    }

    public String content(String requestingUserId, String password) {
        if (compressedData == null) {
            return null;
        }

        byte[] data = compressedData.getData();

        EncryptionMode mode = encryptionMode != null ? encryptionMode : PUBLIC;

        if (mode != PUBLIC && !"1".equals(createdBy)) {
            if (encryptionSalt == null) {
                throw new DecryptionException("Encryption metadata missing for encrypted content");
            }

            byte[] salt = encryptionSalt.getData();
            byte[] key;

            try {
                if (mode == EncryptionMode.PRIVATE) {
                    if (!createdBy.equals(requestingUserId)) {
                        throw new UnauthorizedException("Not authorized to decrypt this content");
                    }
                    key = EncryptionUtils.deriveOwnerKey(requestingUserId, salt);

                } else {
                    if (password == null || password.isEmpty()) {
                        throw new DecryptionException("Password required to decrypt this content");
                    }
                    key = EncryptionUtils.derivePasswordKey(password, salt);
                }

                data = EncryptionUtils.decrypt(data, key, salt);

            } catch (DecryptionException | UnauthorizedException e) {
                throw e;
            } catch (Exception e) {
                throw new DecryptionException("Failed to decrypt content", e);
            }
        }

        return CompressionUtils.decompress(data);
    }

    private static Binary processContent(String content, EncryptionMode encryptionMode, String createdBy, String password, byte[] salt) {
        if (content == null) {
            return null;
        }

        byte[] compressed = CompressionUtils.compress(content);
        EncryptionMode mode = encryptionMode != null ? encryptionMode : PUBLIC;

        if (mode == PUBLIC || "1".equals(createdBy) || salt == null) {
            return new Binary(compressed);
        }

        byte[] key;

        try {
            if (mode == EncryptionMode.PRIVATE) {
                key = EncryptionUtils.deriveOwnerKey(createdBy, salt);
            } else {
                if (password == null || password.isEmpty()) {
                    throw new IllegalArgumentException("Password required for PASSWORD_SHARED mode");
                }
                key = EncryptionUtils.derivePasswordKey(password, salt);
            }

            byte[] encrypted = EncryptionUtils.encrypt(compressed, key);
            return new Binary(encrypted);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt content content", e);
        }
    }

}
