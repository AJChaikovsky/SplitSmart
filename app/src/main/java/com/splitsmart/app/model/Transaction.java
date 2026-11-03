package com.splitsmart.app.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One row of group history: either an expense or a settlement. */
public class Transaction {
    private String id = "";
    private TransactionType type = TransactionType.EXPENSE;
    private String groupId = "";
    private String title = "";
    private double amount = 0.0;
    private String date = "";
    private String category = "";
    private String note = "";
    private List<String> memberIds = new ArrayList<>();
    private Expense expense = null;
    private Settlement settlement = null;

    public String getId() { return id; }
    public TransactionType getType() { return type; }
    public String getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public String getNote() { return note; }
    public List<String> getMemberIds() { return memberIds; }
    public Expense getExpense() { return expense; }
    public Settlement getSettlement() { return settlement; }

    public static Transaction fromExpense(Expense expense) {
        Transaction tx = new Transaction();
        tx.id = expense.getId();
        tx.type = TransactionType.EXPENSE;
        tx.groupId = expense.getGroupId();
        tx.title = expense.getTitle();
        tx.amount = expense.getAmount();
        tx.date = expense.getDate();
        tx.category = expense.getCategory();
        tx.note = expense.getNote();
        tx.memberIds = new ArrayList<>(expense.getParticipantIds());
        tx.expense = expense;
        return tx;
    }

    public static Transaction fromSettlement(Settlement settlement) {
        Transaction tx = new Transaction();
        tx.id = settlement.getId();
        tx.type = TransactionType.SETTLEMENT;
        tx.groupId = settlement.getGroupId();
        tx.title = "Settlement";
        tx.amount = settlement.getAmount();
        tx.date = settlement.getDate();
        tx.category = "";
        tx.note = settlement.getNote();
        tx.memberIds = Arrays.asList(settlement.getFromUserId(), settlement.getToUserId());
        tx.settlement = settlement;
        return tx;
    }
}
