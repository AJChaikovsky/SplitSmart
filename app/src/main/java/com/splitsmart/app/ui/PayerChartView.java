package com.splitsmart.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.splitsmart.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Horizontal bars for how much each member paid this month, with average marker. */
public class PayerChartView extends View {
    private static class Slice {
        final String label;
        final double value;

        Slice(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<Slice> slices = Collections.emptyList();
    private double average = 0.0;
    private final float density = getResources().getDisplayMetrics().density;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avgLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette;

    public PayerChartView(Context context) {
        this(context, null);
    }

    public PayerChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PayerChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int primary = ContextCompat.getColor(context, R.color.primary);
        int error = ContextCompat.getColor(context, R.color.error);
        int textPrimary = ContextCompat.getColor(context, R.color.text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);

        barPaint.setStyle(Paint.Style.FILL);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.divider));

        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setStrokeWidth(2f * density);
        avgPaint.setColor(error);
        avgPaint.setPathEffect(new DashPathEffect(new float[]{8f * density, 6f * density}, 0f));

        avgLabelPaint.setColor(error);
        avgLabelPaint.setTextSize(10f * density);
        avgLabelPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(textPrimary);
        labelPaint.setTextSize(12f * density);

        valuePaint.setColor(textSecondary);
        valuePaint.setTextSize(11f * density);
        valuePaint.setTextAlign(Paint.Align.RIGHT);

        emptyPaint.setColor(textSecondary);
        emptyPaint.setTextSize(13f * density);
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        palette = new int[]{
                primary,
                textSecondary,
                0xFF2A9D8F,
                0xFFE76F51,
                0xFF457B9D,
                0xFF8D99AE
        };
    }

    public void setData(Map<String, Double> payerTotals) {
        List<Slice> data = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : payerTotals.entrySet()) {
            data.add(new Slice(entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }
        data.sort((a, b) -> Double.compare(b.value, a.value));
        slices = data;
        average = data.isEmpty() ? 0.0 : total / data.size();
        requestLayout();
        invalidate();
    }

    public void clear() {
        slices = Collections.emptyList();
        average = 0.0;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int rows = Math.max(1, slices.size());
        // Extra space for average legend
        setMeasuredDimension(width, (int) (48f * density * rows + 22f * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slices.isEmpty()) {
            canvas.drawText("No payer data", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float padding = 4f * density;
        double maxVal = Math.max(average, 1e-9);
        for (Slice slice : slices) maxVal = Math.max(maxVal, slice.value);
        float y = padding;
        float barHeight = 16f * density;
        float labelGap = 18f * density;
        String topName = slices.get(0).label;
        float trackWidth = getWidth() - 2 * padding;

        for (int index = 0; index < slices.size(); index++) {
            Slice slice = slices.get(index);
            String tag = slice.label.equals(topName) && slice.value > 0 ? "  ★ most" : "";
            canvas.drawText(slice.label + tag, padding, y + 12f * density, labelPaint);
            canvas.drawText(UiUtils.formatMoney(slice.value), getWidth() - padding, y + 12f * density, valuePaint);
            y += labelGap;
            float barTop = y;
            float barBottom = y + barHeight;
            canvas.drawRoundRect(padding, barTop, getWidth() - padding, barBottom,
                    8f * density, 8f * density, trackPaint);
            float filled = slice.value <= 0 ? 0f
                    : Math.max((float) (slice.value / maxVal * trackWidth), 4f * density);
            if (filled > 0f) {
                barPaint.setColor(palette[index % palette.length]);
                canvas.drawRoundRect(padding, barTop, padding + filled, barBottom,
                        8f * density, 8f * density, barPaint);
            }
            y += barHeight + 14f * density;
        }

        if (average > 0 && slices.size() > 1) {
            float avgX = padding + (float) (average / maxVal * trackWidth);
            float lineTop = padding + labelGap;
            float lineBottom = y - 10f * density;
            canvas.drawLine(avgX, lineTop, avgX, lineBottom, avgPaint);
            canvas.drawText("avg " + UiUtils.formatMoney(average), avgX, getHeight() - 4f * density, avgLabelPaint);
        }
    }
}
