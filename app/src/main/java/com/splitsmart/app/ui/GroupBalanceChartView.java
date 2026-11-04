package com.splitsmart.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.splitsmart.app.R;
import com.splitsmart.app.model.GroupBalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Paired horizontal bars per group: cardinal for what you owe, gold for what you're owed.
 * All bars share one scale so groups can be compared at a glance.
 */
public class GroupBalanceChartView extends View {
    private static final int MAX_GROUPS = 6;

    private List<GroupBalance> groups = Collections.emptyList();
    private double maxValue = 0.0;

    private final float density = getResources().getDisplayMetrics().density;
    private final Paint owePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint owedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GroupBalanceChartView(Context context) {
        this(context, null);
    }

    public GroupBalanceChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GroupBalanceChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        owePaint.setColor(ContextCompat.getColor(context, R.color.chart_owe));
        owedPaint.setColor(ContextCompat.getColor(context, R.color.chart_owed));
        trackPaint.setColor(ContextCompat.getColor(context, R.color.divider));

        namePaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        namePaint.setTextSize(13f * density);
        namePaint.setFakeBoldText(true);

        amountPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        amountPaint.setTextSize(12f * density);
        amountPaint.setTextAlign(Paint.Align.RIGHT);

        legendPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        legendPaint.setTextSize(12f * density);
    }

    /** Shows groups with an open balance, largest first; settled groups are left out. */
    public void setData(List<GroupBalance> balances) {
        List<GroupBalance> open = new ArrayList<>();
        maxValue = 0.0;
        for (GroupBalance balance : balances) {
            if (balance.isSettled()) continue;
            if (open.size() == MAX_GROUPS) break;
            open.add(balance);
            maxValue = Math.max(maxValue, Math.max(balance.getToPay(), balance.getToReceive()));
        }
        groups = open;
        setContentDescription(describe());
        requestLayout();
        invalidate();
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    private String describe() {
        StringBuilder text = new StringBuilder(getContext().getString(R.string.balances_by_group));
        for (GroupBalance balance : groups) {
            text.append(". ").append(balance.getGroupName()).append(": ")
                    .append(getContext().getString(R.string.group_totals,
                            UiUtils.formatMoney(balance.getToPay()),
                            UiUtils.formatMoney(balance.getToReceive())));
        }
        return text.toString();
    }

    private float legendHeight() { return 28f * density; }
    private float rowHeight() { return 66f * density; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (legendHeight() + rowHeight() * groups.size());
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (groups.isEmpty()) return;

        drawLegend(canvas);

        float amountColumn = 76f * density;
        float barLeft = 0f;
        float barRight = getWidth() - amountColumn;
        float barHeight = 10f * density;
        float radius = barHeight / 2f;
        double scale = Math.max(maxValue, 1e-9);

        float y = legendHeight();
        for (GroupBalance balance : groups) {
            CharSequence name = TextUtils.ellipsize(balance.getGroupName(),
                    new android.text.TextPaint(namePaint), getWidth(), TextUtils.TruncateAt.END);
            canvas.drawText(name.toString(), 0f, y + 14f * density, namePaint);

            float oweTop = y + 24f * density;
            drawBar(canvas, barLeft, barRight, oweTop, barHeight, radius, balance.getToPay() / scale, owePaint);
            canvas.drawText(UiUtils.formatMoney(balance.getToPay()), getWidth(), oweTop + barHeight, amountPaint);

            float owedTop = oweTop + barHeight + 8f * density;
            drawBar(canvas, barLeft, barRight, owedTop, barHeight, radius, balance.getToReceive() / scale, owedPaint);
            canvas.drawText(UiUtils.formatMoney(balance.getToReceive()), getWidth(), owedTop + barHeight, amountPaint);

            y += rowHeight();
        }
    }

    private void drawBar(Canvas canvas, float left, float right, float top, float height, float radius,
                         double fraction, Paint fill) {
        canvas.drawRoundRect(left, top, right, top + height, radius, radius, trackPaint);
        if (fraction <= 0) return;
        float width = Math.max((float) (fraction * (right - left)), height);
        canvas.drawRoundRect(left, top, left + width, top + height, radius, radius, fill);
    }

    private void drawLegend(Canvas canvas) {
        float dot = 5f * density;
        float baseline = 14f * density;
        float x = dot;
        canvas.drawCircle(x, baseline - dot, dot, owePaint);
        x += dot + 8f * density;
        String owe = getContext().getString(R.string.you_owe);
        canvas.drawText(owe, x, baseline, legendPaint);
        x += legendPaint.measureText(owe) + 20f * density + dot;
        canvas.drawCircle(x, baseline - dot, dot, owedPaint);
        x += dot + 8f * density;
        canvas.drawText(getContext().getString(R.string.you_are_owed), x, baseline, legendPaint);
    }
}
