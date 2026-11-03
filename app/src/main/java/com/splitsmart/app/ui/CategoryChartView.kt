package com.splitsmart.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.splitsmart.app.R
import kotlin.math.min

/**
 * Lightweight category chart: pie on the left, horizontal bars on the right.
 * No third-party chart dependency — keeps the APK small for the course project.
 */
class CategoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val label: String, val value: Double)

    private var slices: List<Slice> = emptyList()

    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 28f
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 24f
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
        style = Paint.Style.FILL
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

    private val pieRect = RectF()
    private val density = resources.displayMetrics.density

    fun setData(categoryTotals: Map<String, Double>) {
        slices = categoryTotals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { Slice(it.key, it.value) }
        invalidate()
    }

    fun clear() {
        slices = emptyList()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (220 * density).toInt()
        val height = resolveSize(desired, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) {
            val cx = width / 2f
            val cy = height / 2f
            canvas.drawText("No spending this month", cx - 120f, cy, secondaryTextPaint)
            return
        }

        val total = slices.sumOf { it.value }.coerceAtLeast(1e-9)
        val padding = 12f * density
        val pieSize = min(height - 2 * padding, width * 0.42f)
        pieRect.set(padding, (height - pieSize) / 2f, padding + pieSize, (height + pieSize) / 2f)

        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.value / total * 360.0).toFloat()
            piePaint.color = palette[index % palette.size]
            canvas.drawArc(pieRect, startAngle, sweep, true, piePaint)
            startAngle += sweep
        }

        val barLeft = pieRect.right + 16f * density
        val barRight = width - padding
        val barAreaHeight = height - 2 * padding
        val rowHeight = barAreaHeight / slices.size.coerceAtLeast(1)
        val maxVal = slices.maxOf { it.value }.coerceAtLeast(1e-9)
        val labelWidth = 90f * density
        val valueWidth = 56f * density

        slices.forEachIndexed { index, slice ->
            val top = padding + index * rowHeight
            val color = palette[index % palette.size]
            textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
            canvas.drawText(slice.label, barLeft, top + rowHeight * 0.42f, textPaint)

            val trackLeft = barLeft + labelWidth
            val trackRight = barRight - valueWidth
            val trackTop = top + rowHeight * 0.55f
            val trackBottom = top + rowHeight * 0.85f
            canvas.drawRoundRect(trackLeft, trackTop, trackRight, trackBottom, 6f, 6f, emptyPaint)

            val filled = ((slice.value / maxVal) * (trackRight - trackLeft)).toFloat()
            barPaint.color = color
            canvas.drawRoundRect(trackLeft, trackTop, trackLeft + filled, trackBottom, 6f, 6f, barPaint)

            secondaryTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
            canvas.drawText(
                formatMoney(slice.value),
                trackRight + 8f * density,
                top + rowHeight * 0.75f,
                secondaryTextPaint
            )
        }
    }
}
