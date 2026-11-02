package com.splitsmart.app.business

import com.splitsmart.app.data.FirebaseAuthManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.model.User

class AccountManager(
    private val authManager: FirebaseAuthManager = FirebaseAuthManager.getInstance(),
    private val userRepository: UserRepository = UserRepository.getInstance()
) {
    data class ValidationResult(val ok: Boolean, val error: String? = null)

    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        uscId: String
    ): ValidationResult {
        if (name.isBlank()) return ValidationResult(false, "Name is required")
        if (email.isBlank()) return ValidationResult(false, "Email is required")
        if (!email.lowercase().endsWith("@usc.edu")) {
            return ValidationResult(false, "Email must be a @usc.edu address")
        }
        if (!uscId.matches(Regex("^\\d{10}$"))) {
            return ValidationResult(false, "USC ID must be exactly 10 digits")
        }
        if (password.isBlank()) return ValidationResult(false, "Password is required")
        if (password.length < 6) return ValidationResult(false, "Password must be at least 6 characters")
        return ValidationResult(true)
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        if (email.isBlank()) return ValidationResult(false, "Email is required")
        if (password.isBlank()) return ValidationResult(false, "Password is required")
        return ValidationResult(true)
    }

    suspend fun register(name: String, email: String, password: String, uscId: String): User {
        val validation = validateRegistration(name, email, password, uscId)
        require(validation.ok) { validation.error ?: "Invalid registration" }

        val normalizedEmail = email.trim().lowercase()
        val existing = userRepository.getByEmail(normalizedEmail)
        require(existing == null) { "An account with this email already exists" }

        val firebaseUser = authManager.register(normalizedEmail, password)
        val user = User(
            uid = firebaseUser.uid,
            name = name.trim(),
            email = normalizedEmail,
            uscId = uscId.trim()
        )
        userRepository.save(user)
        return user
    }

    suspend fun login(email: String, password: String): User {
        val validation = validateLogin(email, password)
        require(validation.ok) { validation.error ?: "Invalid login" }

        val firebaseUser = authManager.login(email.trim().lowercase(), password)
        return userRepository.getById(firebaseUser.uid)
            ?: throw IllegalStateException("User profile not found in Firestore")
    }

    fun logout() {
        authManager.logout()
    }

    fun currentUid(): String? = authManager.currentUid()

    fun isLoggedIn(): Boolean = authManager.isLoggedIn()

    suspend fun getCurrentUser(): User? {
        val uid = authManager.currentUid() ?: return null
        return userRepository.getById(uid)
    }

    suspend fun updateName(name: String): User {
        require(name.isNotBlank()) { "Name cannot be empty" }
        val uid = authManager.currentUid() ?: throw IllegalStateException("Not logged in")
        userRepository.updateName(uid, name.trim())
        return userRepository.getById(uid) ?: throw IllegalStateException("User not found")
    }

    companion object {
        @Volatile
        private var instance: AccountManager? = null

        fun getInstance(): AccountManager {
            return instance ?: synchronized(this) {
                instance ?: AccountManager().also { instance = it }
            }
        }
    }
}
