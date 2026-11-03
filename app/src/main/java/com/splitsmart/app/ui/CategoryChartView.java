package com.splitsmart.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.splitsmart.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monthly summary graphics: donut chart + legend + vertical bars.
 */
public class CategoryChartView extends View {
    private static class Slice {
        final String label;
        final double value;

        Slice(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<Slice> slices = Collections.emptyList();
    private String centerLabel = "";

    private final float density = getResources().getDisplayMetrics().density;

    private final Paint donutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerCaptionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint metaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette;

    private final RectF donutRect = new RectF();

    public CategoryChartView(Context context) {
        this(context, null);
    }

    public CategoryChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategoryChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int primary = ContextCompat.getColor(context, R.color.primary);
        int textPrimary = ContextCompat.getColor(context, R.color.text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);

        donutPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStyle(Paint.Style.FILL);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.divider));

        titlePaint.setColor(textPrimary);
        titlePaint.setTextSize(15f * density);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        centerValuePaint.setColor(primary);
        centerValuePaint.setTextSize(16f * density);
        centerValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        centerValuePaint.setTextAlign(Paint.Align.CENTER);

        centerCaptionPaint.setColor(textSecondary);
        centerCaptionPaint.setTextSize(11f * density);
        centerCaptionPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(textPrimary);
        labelPaint.setTextSize(12f * density);

        metaPaint.setColor(textSecondary);
        metaPaint.setTextSize(11f * density);

        emptyPaint.setColor(textSecondary);
        emptyPaint.setTextSize(14f * density);
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        palette = new int[]{
                primary,
                textSecondary,
                0xFF2A9D8F,
                0xFFE76F51,
                0xFF264653,
                0xFFE9C46A,
                0xFF457B9D,
                0xFF8D99AE
        };
    }

    public void setData(Map<String, Double> categoryTotals, String totalLabel) {
        List<Slice> data = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > 0) data.add(new Slice(entry.getKey(), entry.getValue()));
        }
        data.sort((a, b) -> Double.compare(b.value, a.value));
        slices = data;
        centerLabel = totalLabel == null || totalLabel.trim().isEmpty()
                ? UiUtils.formatMoney(sum())
                : totalLabel;
        requestLayout();
        invalidate();
    }

    public void clear() {
        slices = Collections.emptyList();
        centerLabel = "";
        requestLayout();
        invalidate();
    }

    private double sum() {
        double total = 0.0;
        for (Slice slice : slices) total += slice.value;
        return total;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int donutBlock = (int) (210 * density);
        int legendRows = Math.max(1, slices.size());
        int legendBlock = (int) (28 * density * legendRows + 28 * density);
        int barBlock = (int) (28 * density * legendRows + 40 * density);
        setMeasuredDimension(width, donutBlock + legendBlock + barBlock);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slices.isEmpty()) {
            canvas.drawText("No spending this month", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        double total = Math.max(sum(), 1e-9);
        float padding = 12f * density;
        drawDonut(canvas, total, padding);
        float legendTop = donutRect.bottom + 20f * density;
        float legendBottom = drawLegend(canvas, total, legendTop, padding);
        drawBars(canvas, legendBottom + 16f * density, padding);
    }

    private void drawDonut(Canvas canvas, double total, float padding) {
        float size = Math.min(getWidth() - 2 * padding, 180f * density);
        float left = (getWidth() - size) / 2f;
        donutRect.set(left, padding, left + size, padding + size);

        donutPaint.setStrokeWidth(28f * density);
        donutPaint.setStrokeCap(Paint.Cap.BUTT);

        float start = -90f;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            float sweep = Math.max((float) (slice.value / total * 360.0), 1.5f);
            donutPaint.setColor(palette[index % palette.length]);
            // Slight gap between slices for readability
            float gap = slices.size() > 1 ? 2f : 0f;
            canvas.drawArc(donutRect, start + gap / 2f, sweep - gap, false, donutPaint);
            start += sweep;
        }

        float cx = donutRect.centerX();
        float cy = donutRect.centerY();
        canvas.drawText(centerLabel, cx, cy - 4f * density, centerValuePaint);
        canvas.drawText("total", cx, cy + 14f * density, centerCaptionPaint);
    }

    private float drawLegend(Canvas canvas, double total, float top, float padding) {
        canvas.drawText("Breakdown", padding, top, titlePaint);
        float y = top + 22f * density;
        float swatch = 10f * density;
        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            barPaint.setColor(palette[index % palette.length]);
            canvas.drawRoundRect(
                    padding,
                    y - swatch + 2f * density,
                    padding + swatch,
                    y + 2f * density,
                    3f * density,
                    3f * density,
                    barPaint
            );
            double pct = slice.value / total * 100.0;
            String line = slice.label + "  ·  " + UiUtils.formatMoney(slice.value)
                    + "  (" + String.format(Locale.getDefault(), "%.0f", pct) + "%)";
            canvas.drawText(line, padding + swatch + 10f * density, y, labelPaint);
            y += 26f * density;
        }
        return y;
    }

    private void drawBars(Canvas canvas, float top, float padding) {
        canvas.drawText("Compare", padding, top, titlePaint);
        float y = top + 18f * density;
        double maxVal = 1e-9;
        for (Slice slice : slices) maxVal = Math.max(maxVal, slice.value);
        float barLeft = padding;
        float barRight = getWidth() - padding;
        float barHeight = 14f * density;

        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            canvas.drawText(slice.label, barLeft, y, metaPaint);
            y += 6f * density;
            canvas.drawRoundRect(barLeft, y, barRight, y + barHeight, 7f * density, 7f * density, trackPaint);
            float filled = Math.max((float) (slice.value / maxVal * (barRight - barLeft)), barHeight);
            barPaint.setColor(palette[index % palette.length]);
            canvas.drawRoundRect(barLeft, y, barLeft + filled, y + barHeight, 7f * density, 7f * density, barPaint);
            y += barHeight + 14f * density;
        }
    }
}
