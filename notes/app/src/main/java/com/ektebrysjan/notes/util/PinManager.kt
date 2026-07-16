package com.ektebrysjan.notes.util

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores and verifies the optional private-notes PIN.
 *
 * The PIN itself is never stored. We keep a random salt plus SHA-256(salt || pin), both Base64, in
 * private SharedPreferences. This gates access to private notes inside the app — it does not encrypt
 * the database (see the in-app Privacy section, which says so honestly).
 */
class PinManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, salt.toB64())
            .putString(KEY_HASH, hash(salt, pin).toB64())
            .apply()
    }

    /** True if [pin] matches the stored PIN. False if wrong or no PIN is set. */
    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.fromB64() ?: return false
        val stored = prefs.getString(KEY_HASH, null)?.fromB64() ?: return false
        return MessageDigest.isEqual(stored, hash(salt, pin))
    }

    fun clear() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
    }

    private fun hash(salt: ByteArray, pin: String): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(pin.toByteArray(Charsets.UTF_8))
        }

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        private const val PREFS = "notes_prefs"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val SALT_BYTES = 16

        /** Minimum PIN length enforced by the UI. */
        const val MIN_PIN_LENGTH = 4
    }
}
