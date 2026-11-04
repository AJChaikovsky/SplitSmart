package com.splitsmart.app.model;

/** The current user's totals in one group: what they owe and what they're owed. */
public class GroupBalance {
    private final String groupId;
    private final String groupName;
    private final double toPay;
    private final double toReceive;

    public GroupBalance(String groupId, String groupName, double toPay, double toReceive) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.toPay = toPay;
        this.toReceive = toReceive;
    }

    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public double getToPay() { return toPay; }
    public double getToReceive() { return toReceive; }

    public boolean isSettled() {
        return toPay < 0.005 && toReceive < 0.005;
    }
}
