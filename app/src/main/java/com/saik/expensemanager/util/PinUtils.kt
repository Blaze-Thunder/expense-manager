package com.saik.expensemanager.util

object PinUtils {
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(android.util.Base64.decode(salt, android.util.Base64.NO_WRAP))
        val hash = digest.digest(pin.toByteArray())
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, salt: String, storedHash: String): Boolean {
        return hashPin(pin, salt) == storedHash
    }
}
