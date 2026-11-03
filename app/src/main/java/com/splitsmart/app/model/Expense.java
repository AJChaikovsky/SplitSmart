package com.splitsmart.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Expense {
    private String id = "";
    private String groupId = "";
    private String title = "";
    private double amount = 0.0;
    private String date = "";
    private String category = ExpenseCategory.OTHER.name();
    private String payerId = "";
    private List<String> participantIds = new ArrayList<>();
    private String splitMethod = SplitMethod.EQUAL.name();
    private String note = "";
    private String creatorId = "";
    private List<ExpenseShare> shares = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = new ArrayList<>(participantIds); }
    public String getSplitMethod() { return splitMethod; }
    public void setSplitMethod(String splitMethod) { this.splitMethod = splitMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public List<ExpenseShare> getShares() { return shares; }
    public void setShares(List<ExpenseShare> shares) { this.shares = new ArrayList<>(shares); }

    public ExpenseCategory categoryEnum() {
        return ExpenseCategory.fromString(category);
    }

    public SplitMethod splitMethodEnum() {
        return SplitMethod.fromString(splitMethod);
    }

    public Map<String, Double> shareMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (ExpenseShare share : shares) map.put(share.getUserId(), share.getAmount());
        return map;
    }

    public Map<String, Object> toMap() {
        List<Map<String, Object>> shareMaps = new ArrayList<>();
        for (ExpenseShare share : shares) shareMaps.add(share.toMap());

        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("groupId", groupId);
        map.put("title", title);
        map.put("amount", amount);
        map.put("date", date);
        map.put("category", category);
        map.put("payerId", payerId);
        map.put("participantIds", participantIds);
        map.put("splitMethod", splitMethod);
        map.put("note", note);
        map.put("creatorId", creatorId);
        map.put("shares", shareMaps);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Expense fromMap(Map<String, Object> map) {
        // Shares are stored as a list of maps; older documents used a userId -> amount map.
        List<ExpenseShare> sharesList = new ArrayList<>();
        Object rawShares = map.get("shares");
        if (rawShares instanceof List) {
            for (Object item : (List<?>) rawShares) {
                if (item instanceof Map) sharesList.add(ExpenseShare.fromMap((Map<String, Object>) item));
            }
        } else if (rawShares instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawShares).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Number) {
                    sharesList.add(new ExpenseShare(
                            (String) entry.getKey(),
                            ((Number) entry.getValue()).doubleValue()
                    ));
                }
            }
        }

        Expense expense = new Expense();
        expense.id = MapReader.str(map, "id", "");
        expense.groupId = MapReader.str(map, "groupId", "");
        expense.title = MapReader.str(map, "title", "");
        expense.amount = MapReader.dbl(map, "amount", 0.0);
        expense.date = MapReader.str(map, "date", "");
        expense.category = MapReader.str(map, "category", ExpenseCategory.OTHER.name());
        expense.payerId = MapReader.str(map, "payerId", "");
        expense.participantIds = MapReader.strList(map, "participantIds");
        expense.splitMethod = MapReader.str(map, "splitMethod", SplitMethod.EQUAL.name());
        expense.note = MapReader.str(map, "note", "");
        expense.creatorId = MapReader.str(map, "creatorId", "");
        expense.shares = sharesList;
        return expense;
    }
}
