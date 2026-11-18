package com.hanecuzdan.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Tuzlu SHA-256 parola ve gizli soru cevabı hashleyici. */
object PasswordHasher {

    private const val SALT_BYTES = 16

    fun hash(rawPassword: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = sha256(salt + rawPassword.toByteArray(Charsets.UTF_8))
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(digest)
        return "$saltB64:$hashB64"
    }

    fun verify(rawPassword: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val actual = sha256(salt + rawPassword.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(expected, actual)
    }

    fun hashNormalizedAnswer(answer: String): String =
        hash(answer.trim().lowercase())

    fun verifyNormalizedAnswer(answer: String, stored: String): Boolean =
        verify(answer.trim().lowercase(), stored)

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)
}
