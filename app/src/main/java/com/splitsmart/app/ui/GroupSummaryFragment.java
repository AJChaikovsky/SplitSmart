package com.splitsmart.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.splitsmart.app.R;
import com.splitsmart.app.business.SummaryManager;
import com.splitsmart.app.databinding.ActivityMonthlySummaryBinding;
import com.splitsmart.app.model.MonthlySummary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class GroupSummaryFragment extends GroupTabFragment {
    private ActivityMonthlySummaryBinding binding;
    private final SummaryManager summaryManager = SummaryManager.getInstance();
    private UiTask loading;

    public GroupSummaryFragment() {
        super(R.layout.activity_monthly_summary);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = ActivityMonthlySummaryBinding.bind(view);
        Calendar calendar = Calendar.getInstance();
        String year = savedInstanceState != null ? savedInstanceState.getString("year") : null;
        String month = savedInstanceState != null ? savedInstanceState.getString("month") : null;
        binding.etYear.setText(year != null ? year : String.valueOf(calendar.get(Calendar.YEAR)));
        binding.etMonth.setText(month != null ? month : String.valueOf(calendar.get(Calendar.MONTH) + 1));
        binding.btnLoad.setOnClickListener(v -> refresh());
    }

    @Override
    public void refresh() {
        Integer year = UiUtils.parseInt(UiUtils.text(binding.etYear));
        Integer month = UiUtils.parseInt(UiUtils.text(binding.etMonth));
        if (year == null || month == null || month < 1 || month > 12) {
            UiUtils.toast(requireContext(), getString(R.string.invalid_period));
            return;
        }
        String groupId = getGroupId();
        if (loading != null) loading.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        loading = UiUtils.loadData(this, () -> summaryManager.getMonthlySummary(groupId, year, month),
                this::render, message -> {
                    binding.progress.setVisibility(View.GONE);
                    UiUtils.toast(requireContext(), message);
                });
    }

    private void render(MonthlySummary summary) {
        binding.tvTotalSpending.setText(UiUtils.formatMoney(summary.getTotalSpending()));
        binding.tvExpenseCount.setText(getResources().getQuantityString(
                R.plurals.expense_count, summary.getExpenseCount(), summary.getExpenseCount()));
        binding.tvSummary.setText(summary.isEmpty()
                ? getString(R.string.no_expenses_month)
                : getString(R.string.category_period, summary.getYear(), summary.getMonth()));
        binding.layoutCharts.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
        if (summary.isEmpty()) {
            binding.categoryChart.clear();
            binding.payerChart.clear();
            binding.tvPayerInsights.setText("");
        } else {
            binding.categoryChart.setData(summary.getCategoryTotals(), UiUtils.formatMoney(summary.getTotalSpending()));
            binding.payerChart.setData(summary.getPayerTotals());
            if (summary.getPayerInsights().isEmpty()) {
                binding.tvPayerInsights.setText(getString(R.string.payers_equal));
            } else {
                StringBuilder insights = new StringBuilder();
                for (String insight : summary.getPayerInsights()) {
                    if (insights.length() > 0) insights.append("\n");
                    insights.append("• ").append(insight);
                }
                binding.tvPayerInsights.setText(insights.toString());
            }
        }
        binding.llCategoryTotals.removeAllViews();
        binding.llCategoryTotals.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
        List<Map.Entry<String, Double>> totals = new ArrayList<>(summary.getCategoryTotals().entrySet());
        totals.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Double> entry : totals) {
            View row = getLayoutInflater().inflate(R.layout.item_category_total, binding.llCategoryTotals, false);
            ((TextView) row.findViewById(R.id.tvCategory)).setText(UiUtils.displayChoice(entry.getKey()));
            ((TextView) row.findViewById(R.id.tvTotal)).setText(UiUtils.formatMoney(entry.getValue()));
            binding.llCategoryTotals.addView(row);
        }
        binding.progress.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            outState.putString("year", UiUtils.text(binding.etYear));
            outState.putString("month", UiUtils.text(binding.etMonth));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
