package com.codekage.showup.v2.util

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable backup cipher — pure-JVM crypto so the same logic runs on Android and Desktop.
 *
 * File layout:
 *   magic    "SHWB" (4 bytes)
 *   version  0x01   (1 byte)
 *   iter     int32  big-endian (PBKDF2 iterations)
 *   salt     16 bytes
 *   iv       12 bytes
 *   ciphertext + GCM tag (variable)
 *
 * Key derivation: PBKDF2WithHmacSHA256, 256-bit AES key.
 * Symmetric cipher: AES-256/GCM, 128-bit auth tag.
 */
object PortableBackupCipher {

    private const val MAGIC = "SHWB"
    private const val VERSION: Byte = 1
    const val DEFAULT_ITERATIONS = 600_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256

    fun encrypt(plain: File, encrypted: File, passphrase: CharArray, iterations: Int = DEFAULT_ITERATIONS) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt, iterations)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        encrypted.outputStream().use { out ->
            writeHeader(out, iterations, salt, iv)
            plain.inputStream().use { input ->
                javax.crypto.CipherOutputStream(out, cipher).use { cout ->
                    input.copyTo(cout)
                }
            }
        }
    }

    fun decrypt(encrypted: File, plain: File, passphrase: CharArray) {
        encrypted.inputStream().use { input ->
            val (iterations, salt, iv) = readHeader(input)
            val key = deriveKey(passphrase, salt, iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

            plain.outputStream().use { out ->
                javax.crypto.CipherInputStream(input, cipher).use { cin ->
                    cin.copyTo(out)
                }
            }
        }
    }

    private data class Header(val iterations: Int, val salt: ByteArray, val iv: ByteArray)

    private fun writeHeader(out: OutputStream, iterations: Int, salt: ByteArray, iv: ByteArray) {
        out.write(MAGIC.toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(VERSION))
        out.write(ByteBuffer.allocate(4).putInt(iterations).array())
        out.write(salt)
        out.write(iv)
    }

    private fun readHeader(input: InputStream): Header {
        val magic = ByteArray(4).also { require(input.read(it) == 4) { "truncated magic" } }
        require(String(magic, Charsets.US_ASCII) == MAGIC) { "not a ShowUp portable backup" }
        val version = input.read()
        require(version == VERSION.toInt()) { "unsupported backup version: $version" }
        val iterBytes = ByteArray(4).also { require(input.read(it) == 4) { "truncated iter" } }
        val iterations = ByteBuffer.wrap(iterBytes).int
        val salt = ByteArray(SALT_LEN).also { require(input.read(it) == SALT_LEN) { "truncated salt" } }
        val iv = ByteArray(IV_LEN).also { require(input.read(it) == IV_LEN) { "truncated iv" } }
        return Header(iterations, salt, iv)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        return SecretKeySpec(derived, "AES")
    }
}
