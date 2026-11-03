package com.splitsmart.app.model;

public class FriendBalance {
    private final User friend;
    /** Positive => friend owes you; negative => you owe friend. */
    private final double netAmount;

    public FriendBalance(User friend, double netAmount) {
        this.friend = friend;
        this.netAmount = netAmount;
    }

    public User getFriend() { return friend; }
    public double getNetAmount() { return netAmount; }

    public boolean youAreOwed() {
        return netAmount > 0.009;
    }

    public boolean youOwe() {
        return netAmount < -0.009;
    }

    public boolean isSettled() {
        return !youAreOwed() && !youOwe();
    }
}
