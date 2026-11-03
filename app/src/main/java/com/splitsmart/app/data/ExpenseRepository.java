package com.splitsmart.app.data;

import com.splitsmart.app.model.Expense;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExpenseRepository {
    private static volatile ExpenseRepository instance;

    private final FirestoreManager firestore;

    public ExpenseRepository() {
        this(FirestoreManager.getInstance());
    }

    public ExpenseRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static ExpenseRepository getInstance() {
        if (instance == null) {
            synchronized (ExpenseRepository.class) {
                if (instance == null) instance = new ExpenseRepository();
            }
        }
        return instance;
    }

    /** Saves the expense, assigning a new id if it has none, and returns it. */
    public Expense save(Expense expense) {
        if (expense.getId().trim().isEmpty()) expense.setId(UUID.randomUUID().toString());
        firestore.setDocument(FirestoreManager.COLLECTION_EXPENSES, expense.getId(), expense.toMap());
        return expense;
    }

    public Expense getById(String id) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_EXPENSES, id);
        return data == null ? null : Expense.fromMap(data);
    }

    /** All expenses in a group, newest date first. */
    public List<Expense> getByGroup(String groupId) {
        List<Expense> expenses = new ArrayList<>();
        for (Map<String, Object> data : firestore.queryEquals(
                FirestoreManager.COLLECTION_EXPENSES, "groupId", groupId)) {
            expenses.add(Expense.fromMap(data));
        }
        expenses.sort(Comparator.comparing(Expense::getDate).reversed());
        return expenses;
    }

    public void delete(String id) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_EXPENSES, id);
    }

    public void deleteByGroup(String groupId) {
        for (Expense expense : getByGroup(groupId)) delete(expense.getId());
    }
}
