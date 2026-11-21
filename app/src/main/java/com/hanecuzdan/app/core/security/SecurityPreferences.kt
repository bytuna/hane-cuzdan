package com.hanecuzdan.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/** Parola, gizli soru, biyometrik ayar ve GitHub PAT bilgisini şifreli saklar. */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "hanecuzdan_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var secretQuestion: String
        get() = prefs.getString(KEY_SECRET_QUESTION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECRET_QUESTION, value).apply()

    var githubRepoUrl: String
        get() = prefs.getString(KEY_GITHUB_REPO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value).apply()

    var githubPat: String
        get() = prefs.getString(KEY_GITHUB_PAT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_PAT, value).apply()

    var familyId: String
        get() = prefs.getString(KEY_FAMILY_ID, "hane_aile_havuzu") ?: "hane_aile_havuzu"
        set(value) = prefs.edit().putString(KEY_FAMILY_ID, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Hane Üyesi") ?: "Hane Üyesi"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    val memberId: String
        get() {
            val existing = prefs.getString(KEY_MEMBER_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_MEMBER_ID, generated).apply()
            return generated
        }

    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0) // 0: Sistem, 1: Açık, 2: Koyu
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    fun hasPasswordSet(): Boolean = !prefs.getString(KEY_PASSWORD_HASH, null).isNullOrBlank()

    fun savePasswordAndSecret(password: String, question: String, answer: String) {
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, PasswordHasher.hash(password))
            .putString(KEY_SECRET_QUESTION, question.trim())
            .putString(KEY_SECRET_ANSWER_HASH, PasswordHasher.hashNormalizedAnswer(answer))
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        return PasswordHasher.verify(password, stored)
    }

    fun verifySecretAnswer(answer: String): Boolean {
        val stored = prefs.getString(KEY_SECRET_ANSWER_HASH, null) ?: return false
        return PasswordHasher.verifyNormalizedAnswer(answer, stored)
    }

    fun resetPasswordWithSecret(answer: String, newPassword: String): Boolean {
        if (!verifySecretAnswer(answer)) return false
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, PasswordHasher.hash(newPassword))
            .apply()
        return true
    }

    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_SECRET_QUESTION = "secret_question"
        private const val KEY_SECRET_ANSWER_HASH = "secret_answer_hash"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_GITHUB_PAT = "github_pat"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MEMBER_ID = "member_id"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
