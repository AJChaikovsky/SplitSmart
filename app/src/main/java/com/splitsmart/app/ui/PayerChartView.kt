package com.splitsmart.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.splitsmart.app.R

/** Horizontal bars for how much each member paid this month. */
class PayerChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val label: String, val value: Double)

    private var slices: List<Slice> = emptyList()
    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.divider)
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
        ContextCompat.getColor(context, R.color.accent),
        0xFF2A9D8F.toInt(),
        0xFFE76F51.toInt(),
        0xFF457B9D.toInt(),
        0xFF8D99AE.toInt()
    )

    fun setData(payerTotals: Map<String, Double>) {
        slices = payerTotals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { Slice(it.key, it.value) }
        requestLayout()
        invalidate()
    }

    fun clear() {
        slices = emptyList()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = slices.size.coerceAtLeast(1)
        val desired = (44f * density * rows + 8f * density).toInt()
        setMeasuredDimension(width, desired)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) {
            canvas.drawText("No payer data", width / 2f, height / 2f, emptyPaint)
            return
        }

        val padding = 4f * density
        val maxVal = slices.maxOf { it.value }.coerceAtLeast(1e-9)
        var y = padding
        val barHeight = 16f * density
        val labelGap = 18f * density

        slices.forEachIndexed { index, slice ->
            canvas.drawText(slice.label, padding, y + 12f * density, labelPaint)
            canvas.drawText(formatMoney(slice.value), width - padding, y + 12f * density, valuePaint)
            y += labelGap
            canvas.drawRoundRect(
                padding, y, width - padding, y + barHeight,
                8f * density, 8f * density, trackPaint
            )
            val filled = ((slice.value / maxVal) * (width - 2 * padding))
                .toFloat()
                .coerceAtLeast(barHeight)
            barPaint.color = palette[index % palette.size]
            canvas.drawRoundRect(
                padding, y, padding + filled, y + barHeight,
                8f * density, 8f * density, barPaint
            )
            y += barHeight + 14f * density
        }
    }
}
