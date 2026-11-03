package com.splitsmart.app.data;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Firebase Authentication wrapper. Register, login and delete block and must run off the main thread. */
public class FirebaseAuthManager {
    private static volatile FirebaseAuthManager instance;

    private final FirebaseAuth auth;

    public FirebaseAuthManager() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseAuthManager(FirebaseAuth auth) {
        this.auth = auth;
    }

    public static FirebaseAuthManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseAuthManager.class) {
                if (instance == null) instance = new FirebaseAuthManager();
            }
        }
        return instance;
    }

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public String currentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser register(String email, String password) {
        AuthResult result = TaskAwaiter.await(auth.createUserWithEmailAndPassword(email, password));
        if (result.getUser() == null) {
            throw new IllegalStateException("Registration succeeded but user is null");
        }
        return result.getUser();
    }

    public FirebaseUser login(String email, String password) {
        AuthResult result = TaskAwaiter.await(auth.signInWithEmailAndPassword(email, password));
        if (result.getUser() == null) {
            throw new IllegalStateException("Login succeeded but user is null");
        }
        return result.getUser();
    }

    public void logout() {
        auth.signOut();
    }

    public void deleteCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        TaskAwaiter.await(user.delete());
    }
}
