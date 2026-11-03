package com.splitsmart.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.splitsmart.app.R

/** Horizontal bars for how much each member paid this month, with average marker. */
class PayerChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val label: String, val value: Double)

    private var slices: List<Slice> = emptyList()
    private var average: Double = 0.0
    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.divider)
    }
    private val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = ContextCompat.getColor(context, R.color.error)
        pathEffect = DashPathEffect(floatArrayOf(8f * density, 6f * density), 0f)
    }
    private val avgLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.error)
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 12f * density
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 11f * density
        textAlign = Paint.Align.RIGHT
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }

    private val palette = intArrayOf(
        ContextCompat.getColor(context, R.color.primary),
        ContextCompat.getColor(context, R.color.text_secondary),
        0xFF2A9D8F.toInt(),
        0xFFE76F51.toInt(),
        0xFF457B9D.toInt(),
        0xFF8D99AE.toInt()
    )

    fun setData(payerTotals: Map<String, Double>) {
        slices = payerTotals.entries
            .sortedByDescending { it.value }
            .map { Slice(it.key, it.value) }
        average = if (slices.isEmpty()) 0.0 else slices.sumOf { it.value } / slices.size
        requestLayout()
        invalidate()
    }

    fun clear() {
        slices = emptyList()
        average = 0.0
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = slices.size.coerceAtLeast(1)
        // Extra space for average legend
        val desired = (48f * density * rows + 22f * density).toInt()
        setMeasuredDimension(width, desired)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) {
            canvas.drawText("No payer data", width / 2f, height / 2f, emptyPaint)
            return
        }

        val padding = 4f * density
        val maxVal = slices.maxOf { it.value }.coerceAtLeast(average).coerceAtLeast(1e-9)
        var y = padding
        val barHeight = 16f * density
        val labelGap = 18f * density
        val topName = slices.first().label

        slices.forEachIndexed { index, slice ->
            val tag = if (slice.label == topName && slice.value > 0) "  ★ most" else ""
            canvas.drawText(slice.label + tag, padding, y + 12f * density, labelPaint)
            canvas.drawText(formatMoney(slice.value), width - padding, y + 12f * density, valuePaint)
            y += labelGap
            val barTop = y
            val barBottom = y + barHeight
            canvas.drawRoundRect(
                padding, barTop, width - padding, barBottom,
                8f * density, 8f * density, trackPaint
            )
            val filled = if (slice.value <= 0) 0f
            else ((slice.value / maxVal) * (width - 2 * padding)).toFloat().coerceAtLeast(4f * density)
            if (filled > 0f) {
                barPaint.color = palette[index % palette.size]
                canvas.drawRoundRect(
                    padding, barTop, padding + filled, barBottom,
                    8f * density, 8f * density, barPaint
                )
            }
            y += barHeight + 14f * density
        }

        if (average > 0 && slices.size > 1) {
            val avgX = padding + ((average / maxVal) * (width - 2 * padding)).toFloat()
            val lineTop = padding + labelGap
            val lineBottom = y - 10f * density
            canvas.drawLine(avgX, lineTop, avgX, lineBottom, avgPaint)
            canvas.drawText("avg ${formatMoney(average)}", avgX, height - 4f * density, avgLabelPaint)
        }
    }
}
