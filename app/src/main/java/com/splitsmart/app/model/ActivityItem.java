package com.splitsmart.app.model;

public class ActivityItem {
    private final String id;
    private final ActivityKind kind;
    private final String groupId;
    private final String groupName;
    private final String title;
    private final String subtitle;
    private final double amount;
    private final String date;
    private final String actorName;

    public ActivityItem(String id, ActivityKind kind, String groupId, String groupName, String title,
                        String subtitle, double amount, String date, String actorName) {
        this.id = id;
        this.kind = kind;
        this.groupId = groupId;
        this.groupName = groupName;
        this.title = title;
        this.subtitle = subtitle;
        this.amount = amount;
        this.date = date;
        this.actorName = actorName;
    }

    public String getId() { return id; }
    public ActivityKind getKind() { return kind; }
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getActorName() { return actorName; }
}
