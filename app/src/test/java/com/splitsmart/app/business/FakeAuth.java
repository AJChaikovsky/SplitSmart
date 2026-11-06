package com.splitsmart.app.business;

import com.google.firebase.auth.FirebaseAuth;
import com.splitsmart.app.data.FirebaseAuthManager;

import java.util.HashMap;
import java.util.Map;

/** In-memory stand-in for Firebase Authentication: tracks registered emails and the signed-in uid. */
class FakeAuth extends FirebaseAuthManager {
    private final Map<String, String> uidByEmail = new HashMap<>();
    private final Map<String, String> passwordByEmail = new HashMap<>();
    String signedInUid;
    boolean deletedCurrentUser;

    FakeAuth() {
        super((FirebaseAuth) null);
    }

    /** Registers an account directly (test setup) without signing in. */
    void addAccount(String uid, String email, String password) {
        uidByEmail.put(email, uid);
        passwordByEmail.put(email, password);
    }

    @Override
    public String currentUid() {
        return signedInUid;
    }

    @Override
    public boolean isLoggedIn() {
        return signedInUid != null;
    }

    @Override
    public String register(String email, String password) {
        // Same wording Firebase uses, so AccountManager's error mapping is exercised.
        if (uidByEmail.containsKey(email)) {
            throw new RuntimeException("The email address is already in use by another account.");
        }
        String uid = "uid-" + email;
        addAccount(uid, email, password);
        signedInUid = uid;
        return uid;
    }

    @Override
    public String login(String email, String password) {
        if (!password.equals(passwordByEmail.get(email))) {
            throw new RuntimeException("INVALID_LOGIN_CREDENTIALS");
        }
        signedInUid = uidByEmail.get(email);
        return signedInUid;
    }

    @Override
    public void logout() {
        signedInUid = null;
    }

    @Override
    public void deleteCurrentUser() {
        deletedCurrentUser = true;
        uidByEmail.values().remove(signedInUid);
        signedInUid = null;
    }
}
