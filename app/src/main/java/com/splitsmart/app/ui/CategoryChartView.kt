package com.splitsmart.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.splitsmart.app.R
import kotlin.math.min

/**
 * Monthly summary graphics: donut chart + legend + vertical bars.
 */
class CategoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val label: String, val value: Double)

    private var slices: List<Slice> = emptyList()
    private var centerLabel: String = ""

    private val density = resources.displayMetrics.density

    private val donutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.divider)
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 15f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val centerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        textSize = 16f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val centerCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 12f * density
    }
    private val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 11f * density
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
    }

    private val palette = intArrayOf(
        ContextCompat.getColor(context, R.color.primary),
        ContextCompat.getColor(context, R.color.accent),
        0xFF2A9D8F.toInt(),
        0xFFE76F51.toInt(),
        0xFF264653.toInt(),
        0xFFE9C46A.toInt(),
        0xFF457B9D.toInt(),
        0xFF8D99AE.toInt()
    )

    private val donutRect = RectF()

    fun setData(categoryTotals: Map<String, Double>, totalLabel: String = "") {
        slices = categoryTotals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { Slice(it.key, it.value) }
        centerLabel = totalLabel.ifBlank {
            formatMoney(slices.sumOf { it.value })
        }
        requestLayout()
        invalidate()
    }

    fun clear() {
        slices = emptyList()
        centerLabel = ""
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val donutBlock = (210 * density).toInt()
        val legendRows = slices.size.coerceAtLeast(1)
        val legendBlock = (28 * density * legendRows + 28 * density).toInt()
        val barBlock = (28 * density * legendRows + 40 * density).toInt()
        val desired = donutBlock + legendBlock + barBlock
        setMeasuredDimension(width, desired)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) {
            canvas.drawText("No spending this month", width / 2f, height / 2f, emptyPaint)
            return
        }

        val total = slices.sumOf { it.value }.coerceAtLeast(1e-9)
        val padding = 12f * density
        drawDonut(canvas, total, padding)
        val legendTop = donutRect.bottom + 20f * density
        val legendBottom = drawLegend(canvas, total, legendTop, padding)
        drawBars(canvas, legendBottom + 16f * density, padding)
    }

    private fun drawDonut(canvas: Canvas, total: Double, padding: Float) {
        val size = min(width - 2 * padding, 180f * density)
        val left = (width - size) / 2f
        donutRect.set(left, padding, left + size, padding + size)

        val stroke = 28f * density
        donutPaint.strokeWidth = stroke
        donutPaint.strokeCap = Paint.Cap.BUTT

        var start = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.value / total * 360.0).toFloat().coerceAtLeast(1.5f)
            donutPaint.color = palette[index % palette.size]
            // Slight gap between slices for readability
            val gap = if (slices.size > 1) 2f else 0f
            canvas.drawArc(donutRect, start + gap / 2f, sweep - gap, false, donutPaint)
            start += sweep
        }

        val cx = donutRect.centerX()
        val cy = donutRect.centerY()
        canvas.drawText(centerLabel, cx, cy - 4f * density, centerValuePaint)
        canvas.drawText("total", cx, cy + 14f * density, centerCaptionPaint)
    }

    private fun drawLegend(canvas: Canvas, total: Double, top: Float, padding: Float): Float {
        canvas.drawText("Breakdown", padding, top, titlePaint)
        var y = top + 22f * density
        val swatch = 10f * density
        slices.forEachIndexed { index, slice ->
            val color = palette[index % palette.size]
            barPaint.color = color
            canvas.drawRoundRect(
                padding,
                y - swatch + 2f * density,
                padding + swatch,
                y + 2f * density,
                3f * density,
                3f * density,
                barPaint
            )
            val pct = (slice.value / total * 100.0)
            val line = "${slice.label}  ·  ${formatMoney(slice.value)}  (${"%.0f".format(pct)}%)"
            canvas.drawText(line, padding + swatch + 10f * density, y, labelPaint)
            y += 26f * density
        }
        return y
    }

    private fun drawBars(canvas: Canvas, top: Float, padding: Float) {
        canvas.drawText("Compare", padding, top, titlePaint)
        var y = top + 18f * density
        val maxVal = slices.maxOf { it.value }.coerceAtLeast(1e-9)
        val barLeft = padding
        val barRight = width - padding
        val barHeight = 14f * density

        slices.forEachIndexed { index, slice ->
            canvas.drawText(slice.label, barLeft, y, metaPaint)
            y += 6f * density
            canvas.drawRoundRect(barLeft, y, barRight, y + barHeight, 7f * density, 7f * density, trackPaint)
            val filled = ((slice.value / maxVal) * (barRight - barLeft)).toFloat().coerceAtLeast(barHeight)
            barPaint.color = palette[index % palette.size]
            canvas.drawRoundRect(barLeft, y, barLeft + filled, y + barHeight, 7f * density, 7f * density, barPaint)
            y += barHeight + 14f * density
        }
    }
}
