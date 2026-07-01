package com.mes.catalogue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for the original file bytes stored at rest in the
 * catalogue DB.
 *
 * <p>Each {@link #encrypt} call draws a fresh random 12-byte IV which is
 * prepended to the GCM output, so a stored blob is laid out as
 * {@code [12-byte IV][ciphertext + 16-byte auth tag]}. GCM authenticates the
 * ciphertext, so tampering or a wrong key makes {@link #decrypt} fail loudly
 * rather than return corrupted bytes — important for banking data integrity.</p>
 *
 * <p>The key comes from {@code mes.file.encryption-key} (env
 * {@code MES_FILE_ENCRYPTION_KEY}). A Base64 value that decodes to exactly 32
 * bytes is used directly as the AES-256 key; any other value is hashed (SHA-256)
 * into a 256-bit key, so a raw passphrase also works. If the property is blank a
 * built-in DEVELOPMENT key is used and a warning is logged — production MUST set
 * a real key.</p>
 */
@Component
public class FileEncryption {

    private static final Logger log = LoggerFactory.getLogger(FileEncryption.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;          // 96-bit nonce recommended for GCM
    private static final int TAG_LENGTH_BITS = 128;   // 16-byte authentication tag
    private static final byte[] DEV_KEY_SEED =
            "mes-dev-file-encryption-key-2024".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public FileEncryption(@Value("${mes.file.encryption-key:}") String configuredKey) {
        this.keySpec = new SecretKeySpec(resolveKey(configuredKey), ALGORITHM);
    }

    private static byte[] resolveKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            log.warn("mes.file.encryption-key is not set — using a built-in DEVELOPMENT key. "
                    + "Set MES_FILE_ENCRYPTION_KEY (e.g. `openssl rand -base64 32`) for any "
                    + "non-local environment; stored files are otherwise decryptable by anyone "
                    + "with access to the source.");
            return to256(DEV_KEY_SEED);
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(configuredKey.trim());
        } catch (IllegalArgumentException notBase64) {
            raw = configuredKey.getBytes(StandardCharsets.UTF_8);
        }
        return to256(raw);
    }

    /** Returns the bytes unchanged if already a 256-bit key, else derives one via SHA-256. */
    private static byte[] to256(byte[] material) {
        if (material.length == 32) {
            return material;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(material);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available for key derivation", e);
        }
    }

    /** Encrypts plaintext, returning {@code [12-byte IV][ciphertext + tag]}. */
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] out = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, out, IV_LENGTH, ciphertext.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt file content", e);
        }
    }

    /** Reverses {@link #encrypt}: reads the IV prefix, then decrypts and authenticates. */
    public byte[] decrypt(byte[] stored) {
        if (stored == null) {
            return null;
        }
        if (stored.length < IV_LENGTH) {
            throw new IllegalStateException("Stored file blob is too short to be valid ciphertext");
        }
        try {
            byte[] iv = Arrays.copyOfRange(stored, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(stored, IV_LENGTH, stored.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt stored file content "
                    + "(wrong key, or corrupted/legacy plaintext data)", e);
        }
    }
}
