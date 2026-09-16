package com.travelhistory.app.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Modern Android-supported cryptographic utilities for securing backups using AES-256-GCM.
 *
 * SECURITY SPECIFICATIONS:
 * - Cipher: AES-256 in GCM mode (AES/GCM/NoPadding)
 * - Authentication Tag: 128-bit integrity tag
 * - Key Derivation: PBKDF2WithHmacSHA256 (65,536 iterations, 256-bit key)
 * - Salt: 16 cryptographically secure random bytes
 * - IV/Nonce: 12 cryptographically secure random bytes
 * - Header Tag: TRAVELTRACE_ENC_v1
 * - Zero plaintext password persistence.
 */
object BackupCrypto {

    private const val HEADER_MAGIC = "TRAVELTRACE_ENC_v1\n"
    private val HEADER_BYTES = HEADER_MAGIC.toByteArray(Charsets.UTF_8)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    /**
     * Checks whether the given byte array represents an encrypted TravelTrace backup.
     */
    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < HEADER_BYTES.size + SALT_LENGTH + IV_LENGTH + 16) {
            return false
        }
        for (i in HEADER_BYTES.indices) {
            if (bytes[i] != HEADER_BYTES[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Encrypts plaintext JSON backup string using user-supplied password.
     */
    fun encrypt(plaintextJson: String, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val plaintextBytes = plaintextJson.toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintextBytes)

        val buffer = ByteBuffer.allocate(HEADER_BYTES.size + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        buffer.put(HEADER_BYTES)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    /**
     * Decrypts encrypted backup bytes using user-supplied password.
     * Throws [IllegalArgumentException] or [SecurityException] if password is wrong or data corrupted.
     */
    fun decrypt(encryptedBytes: ByteArray, password: CharArray): String {
        require(isEncrypted(encryptedBytes)) {
            "Invalid backup format: Not an encrypted TravelTrace backup."
        }

        val offset = HEADER_BYTES.size
        val salt = encryptedBytes.copyOfRange(offset, offset + SALT_LENGTH)
        val ivOffset = offset + SALT_LENGTH
        val iv = encryptedBytes.copyOfRange(ivOffset, ivOffset + IV_LENGTH)
        val ciphertextOffset = ivOffset + IV_LENGTH
        val ciphertext = encryptedBytes.copyOfRange(ciphertextOffset, encryptedBytes.size)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw SecurityException("Incorrect password or corrupted backup file.", e)
        }

        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
