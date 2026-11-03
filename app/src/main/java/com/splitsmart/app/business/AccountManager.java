package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.isBlank;
import static com.splitsmart.app.business.Preconditions.require;

import com.google.firebase.auth.FirebaseUser;
import com.splitsmart.app.data.FirebaseAuthManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.model.User;

import java.util.Locale;

public class AccountManager {
    private static volatile AccountManager instance;

    private final FirebaseAuthManager authManager;
    private final UserRepository userRepository;

    public AccountManager() {
        this(FirebaseAuthManager.getInstance(), UserRepository.getInstance());
    }

    public AccountManager(FirebaseAuthManager authManager, UserRepository userRepository) {
        this.authManager = authManager;
        this.userRepository = userRepository;
    }

    public static AccountManager getInstance() {
        if (instance == null) {
            synchronized (AccountManager.class) {
                if (instance == null) instance = new AccountManager();
            }
        }
        return instance;
    }

    public static class ValidationResult {
        private final boolean ok;
        private final String error;

        public ValidationResult(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public boolean isOk() { return ok; }
        public String getError() { return error; }

        static ValidationResult valid() { return new ValidationResult(true, null); }
        static ValidationResult invalid(String error) { return new ValidationResult(false, error); }
    }

    public ValidationResult validateRegistration(String name, String email, String password, String uscId) {
        if (isBlank(name)) return ValidationResult.invalid("Name is required");
        if (isBlank(email)) return ValidationResult.invalid("Email is required");
        if (!email.toLowerCase(Locale.ROOT).endsWith("@usc.edu")) {
            return ValidationResult.invalid("Email must be a @usc.edu address");
        }
        if (!uscId.matches("^\\d{10}$")) {
            return ValidationResult.invalid("USC ID must be exactly 10 digits");
        }
        if (isBlank(password)) return ValidationResult.invalid("Password is required");
        if (password.length() < 6) return ValidationResult.invalid("Password must be at least 6 characters");
        return ValidationResult.valid();
    }

    public ValidationResult validateLogin(String email, String password) {
        if (isBlank(email)) return ValidationResult.invalid("Email is required");
        if (isBlank(password)) return ValidationResult.invalid("Password is required");
        return ValidationResult.valid();
    }

    public User register(String name, String email, String password, String uscId) {
        ValidationResult validation = validateRegistration(name, email, password, uscId);
        require(validation.isOk(), validation.getError() != null ? validation.getError() : "Invalid registration");

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedUscId = uscId.trim();

        // Auth first so Firestore reads/writes run as a signed-in user.
        // Email uniqueness is enforced by Firebase Authentication.
        FirebaseUser firebaseUser;
        try {
            firebaseUser = authManager.register(normalizedEmail, password);
        } catch (RuntimeException e) {
            String msg = messageOf(e);
            if (containsAny(msg, "email address is already in use", "ERROR_EMAIL_ALREADY_IN_USE")) {
                throw new IllegalArgumentException("An account with this email already exists");
            } else if (containsAny(msg, "badly formatted")) {
                throw new IllegalArgumentException("Email address is invalid");
            } else if (containsAny(msg, "weak")) {
                throw new IllegalArgumentException("Password is too weak");
            }
            throw e;
        }

        try {
            User existingUscId = userRepository.getByUscId(normalizedUscId);
            require(existingUscId == null, "An account with this USC ID already exists");

            User user = new User(firebaseUser.getUid(), name.trim(), normalizedEmail, normalizedUscId);
            userRepository.save(user);
            return user;
        } catch (RuntimeException e) {
            // Roll back orphan Auth account if profile creation fails.
            try {
                authManager.deleteCurrentUser();
            } catch (RuntimeException ignored) {
                authManager.logout();
            }
            throw e;
        }
    }

    public User login(String email, String password) {
        ValidationResult validation = validateLogin(email, password);
        require(validation.isOk(), validation.getError() != null ? validation.getError() : "Invalid login");

        FirebaseUser firebaseUser;
        try {
            firebaseUser = authManager.login(email.trim().toLowerCase(Locale.ROOT), password);
        } catch (RuntimeException e) {
            String msg = messageOf(e);
            if (containsAny(msg, "password is invalid", "WRONG_PASSWORD", "INVALID_LOGIN_CREDENTIALS")) {
                throw new IllegalArgumentException("Incorrect email or password");
            } else if (containsAny(msg, "no user record", "USER_NOT_FOUND")) {
                throw new IllegalArgumentException("No account found with that email");
            } else if (containsAny(msg, "badly formatted", "INVALID_EMAIL")) {
                throw new IllegalArgumentException("Email address is invalid");
            } else if (containsAny(msg, "network", "NETWORK_REQUEST_FAILED")) {
                throw new IllegalArgumentException("Network error. Check your connection and try again");
            } else if (containsAny(msg, "disabled")) {
                throw new IllegalArgumentException("This account has been disabled");
            }
            throw e;
        }
        User user = userRepository.getById(firebaseUser.getUid());
        return Preconditions.requireState(user, "User profile not found in Firestore");
    }

    public void logout() {
        authManager.logout();
    }

    public String currentUid() {
        return authManager.currentUid();
    }

    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }

    public User getCurrentUser() {
        String uid = authManager.currentUid();
        return uid == null ? null : userRepository.getById(uid);
    }

    public User updateName(String name) {
        require(!isBlank(name), "Name cannot be empty");
        String uid = Preconditions.requireState(authManager.currentUid(), "Not logged in");
        userRepository.updateName(uid, name.trim());
        return Preconditions.requireState(userRepository.getById(uid), "User not found");
    }

    private static String messageOf(Exception e) {
        return e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String lowerMessage, String... needles) {
        for (String needle : needles) {
            if (lowerMessage.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
