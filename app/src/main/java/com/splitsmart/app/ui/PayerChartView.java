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

/** Donut chart of each member's share of the month's spending, in USC cardinal and gold. */
public class PayerChartView extends View {
    private static class Slice {
        final String label;
        final double value;

        Slice(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    // Alternates cardinal and gold shades so neighbouring slices stay distinct.
    private static final int[] PALETTE = {
            0xFFB52F3B, 0xFFE3AA14, 0xFF7A1F27, 0xFFF2D06B,
            0xFFD9707A, 0xFFA87C0A, 0xFF4F1218, 0xFFF7E3A3
    };

    private List<Slice> slices = Collections.emptyList();
    private double total = 0.0;
    private final float density = getResources().getDisplayMetrics().density;

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptySwatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerCaptionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF donutRect = new RectF();

    public PayerChartView(Context context) {
        this(context, null);
    }

    public PayerChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PayerChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int textPrimary = ContextCompat.getColor(context, R.color.text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);

        slicePaint.setStyle(Paint.Style.STROKE);
        slicePaint.setStrokeCap(Paint.Cap.BUTT);

        emptySwatchPaint.setStyle(Paint.Style.STROKE);
        emptySwatchPaint.setStrokeWidth(1.5f * density);
        emptySwatchPaint.setColor(textSecondary);

        centerValuePaint.setColor(textPrimary);
        centerValuePaint.setTextSize(16f * density);
        centerValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        centerValuePaint.setTextAlign(Paint.Align.CENTER);

        centerCaptionPaint.setColor(textSecondary);
        centerCaptionPaint.setTextSize(11f * density);
        centerCaptionPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(textPrimary);
        labelPaint.setTextSize(13f * density);

        valuePaint.setColor(textSecondary);
        valuePaint.setTextSize(12f * density);
        valuePaint.setTextAlign(Paint.Align.RIGHT);

        emptyPaint.setColor(textSecondary);
        emptyPaint.setTextSize(13f * density);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** @param payerTotals display name -> amount paid; members who paid $0 appear only in the legend. */
    public void setData(Map<String, Double> payerTotals) {
        List<Slice> data = new ArrayList<>();
        double sum = 0.0;
        for (Map.Entry<String, Double> entry : payerTotals.entrySet()) {
            data.add(new Slice(entry.getKey(), entry.getValue()));
            sum += entry.getValue();
        }
        data.sort((a, b) -> Double.compare(b.value, a.value));
        slices = data;
        total = sum;
        setContentDescription(describe());
        requestLayout();
        invalidate();
    }

    public void clear() {
        slices = Collections.emptyList();
        total = 0.0;
        setContentDescription(null);
        requestLayout();
        invalidate();
    }

    private String describe() {
        StringBuilder text = new StringBuilder("Share of spending");
        for (Slice slice : slices) {
            text.append(". ").append(slice.label).append(" paid ").append(UiUtils.formatMoney(slice.value))
                    .append(", ").append(percent(slice.value)).append(" percent");
        }
        return text.toString();
    }

    private int percent(double value) {
        return total <= 0 ? 0 : (int) Math.round(value / total * 100.0);
    }

    private float donutBlock() { return 196f * density; }
    private float legendRow() { return 28f * density; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int rows = Math.max(1, slices.size());
        setMeasuredDimension(width, (int) (donutBlock() + legendRow() * rows + 8f * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slices.isEmpty() || total <= 0) {
            canvas.drawText("No payer data", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }
        drawDonut(canvas);
        drawLegend(canvas, donutBlock() + 8f * density);
    }

    private void drawDonut(Canvas canvas) {
        float stroke = 30f * density;
        float size = Math.min(getWidth(), 176f * density);
        float left = (getWidth() - size) / 2f + stroke / 2f;
        float top = 4f * density + stroke / 2f;
        donutRect.set(left, top, left + size - stroke, top + size - stroke);
        slicePaint.setStrokeWidth(stroke);

        int paid = 0;
        for (Slice slice : slices) if (slice.value > 0) paid++;
        float gap = paid > 1 ? 1.5f : 0f;
        float start = -90f;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            if (slice.value <= 0) continue;
            float sweep = (float) (slice.value / total * 360.0);
            slicePaint.setColor(PALETTE[i % PALETTE.length]);
            canvas.drawArc(donutRect, start + gap / 2f, Math.max(sweep - gap, 0.5f), false, slicePaint);
            start += sweep;
        }

        canvas.drawText(UiUtils.formatMoney(total), donutRect.centerX(), donutRect.centerY() + 2f * density,
                centerValuePaint);
        canvas.drawText("total", donutRect.centerX(), donutRect.centerY() + 18f * density, centerCaptionPaint);
    }

    private void drawLegend(Canvas canvas, float top) {
        float radius = 5f * density;
        float y = top + 16f * density;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            if (slice.value > 0) {
                swatchPaint.setColor(PALETTE[i % PALETTE.length]);
                canvas.drawCircle(radius, y - radius + 1f * density, radius, swatchPaint);
            } else {
                canvas.drawCircle(radius, y - radius + 1f * density, radius - 0.75f * density, emptySwatchPaint);
            }
            canvas.drawText(slice.label, radius * 2 + 10f * density, y, labelPaint);
            String value = UiUtils.formatMoney(slice.value)
                    + String.format(Locale.US, "  ·  %d%%", percent(slice.value));
            canvas.drawText(value, getWidth(), y, valuePaint);
            y += legendRow();
        }
    }
}
