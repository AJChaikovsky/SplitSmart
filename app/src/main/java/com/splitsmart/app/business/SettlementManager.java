package com.splitsmart.app.business;

import static com.splitsmart.app.business.Preconditions.require;
import static com.splitsmart.app.business.Preconditions.requireFound;
import static com.splitsmart.app.business.Preconditions.requireState;

import com.splitsmart.app.data.SettlementRepository;
import com.splitsmart.app.model.Settlement;

import java.util.List;

public class SettlementManager {
    private static volatile SettlementManager instance;

    private final SettlementRepository settlementRepository;
    private final BalanceManager balanceManager;
    private final AccountManager accountManager;

    public SettlementManager() {
        this(SettlementRepository.getInstance(), BalanceManager.getInstance(), AccountManager.getInstance());
    }

    public SettlementManager(SettlementRepository settlementRepository, BalanceManager balanceManager,
                             AccountManager accountManager) {
        this.settlementRepository = settlementRepository;
        this.balanceManager = balanceManager;
        this.accountManager = accountManager;
    }

    public static SettlementManager getInstance() {
        if (instance == null) {
            synchronized (SettlementManager.class) {
                if (instance == null) instance = new SettlementManager();
            }
        }
        return instance;
    }

    public Settlement createSettlement(String groupId, String toUserId, double amount, String date, String note) {
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(amount > 0, "Amount must be greater than 0");
        require(MoneyUtils.hasAtMostTwoDecimals(amount), "Amount may have at most 2 decimal places");
        MoneyUtils.requireIsoDate(date);
        require(!toUserId.equals(currentUid), "Cannot settle with yourself");

        List<BalanceManager.PairBalance> balances = balanceManager.getBalancesForGroup(groupId);
        double owed = balanceManager.amountOwed(currentUid, toUserId, balances);
        require(owed > 0, "You do not owe this user anything");
        require(amount <= owed + 0.001, "Amount cannot exceed what you owe (" + owed + ")");

        Settlement settlement = new Settlement();
        settlement.setGroupId(groupId);
        settlement.setFromUserId(currentUid);
        settlement.setToUserId(toUserId);
        settlement.setAmount(MoneyUtils.roundToCents(amount));
        settlement.setDate(date.trim());
        settlement.setNote(note.trim());
        settlement.setCreatorId(currentUid);
        return settlementRepository.save(settlement);
    }

    public Settlement editSettlement(String settlementId, double amount, String date, String note) {
        Settlement existing = requireFound(settlementRepository.getById(settlementId), "Settlement not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(existing.getCreatorId().equals(currentUid), "Only the creator can edit this settlement");
        require(amount > 0, "Amount must be greater than 0");
        require(MoneyUtils.hasAtMostTwoDecimals(amount), "Amount may have at most 2 decimal places");
        MoneyUtils.requireIsoDate(date);

        // Re-check owed excluding this settlement's effect: add it back temporarily
        List<BalanceManager.PairBalance> balances = balanceManager.getBalancesForGroup(existing.getGroupId());
        double currentOwed = balanceManager.amountOwed(existing.getFromUserId(), existing.getToUserId(), balances);
        double maxAllowed = currentOwed + existing.getAmount();
        require(amount <= maxAllowed + 0.001, "Amount cannot exceed what is owed (" + maxAllowed + ")");

        existing.setAmount(MoneyUtils.roundToCents(amount));
        existing.setDate(date.trim());
        existing.setNote(note.trim());
        return settlementRepository.save(existing);
    }

    public void deleteSettlement(String settlementId) {
        Settlement existing = requireFound(settlementRepository.getById(settlementId), "Settlement not found");
        String currentUid = requireState(accountManager.currentUid(), "Not logged in");
        require(existing.getCreatorId().equals(currentUid), "Only the creator can delete this settlement");
        settlementRepository.delete(settlementId);
    }

    public Settlement getSettlement(String settlementId) {
        return requireFound(settlementRepository.getById(settlementId), "Settlement not found");
    }
}
