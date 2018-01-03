package xyz.juniverse.babylistener.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import xyz.juniverse.babylistener.etc.console

/**
 * Created by juniverse on 30/11/2017.
 */
class WavePlotter @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint()
    private val linePaint = Paint()
    init {
        cellPaint.color = 0xffff0000.toInt()
        cellPaint.style = Paint.Style.STROKE
        cellPaint.strokeWidth = 1f

        linePaint.color = 0x880000ff.toInt()
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        valueUnitHeight = height.toFloat() / maxValue.toFloat()
//        console.d("onMeasure", valueUnitHeight)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            super.onDraw(canvas)
            return
        }

        val width = canvas.width.toFloat()
        val t = canvas.height - (threshold * valueUnitHeight)
        canvas.drawLine(0f, t, width, t, linePaint)

        if (values != null) {
            val size = values?.size?.toFloat() ?: 1f
            val cellWidth = width / size
            values?.mapIndexed { index, value ->
                if (value > 0) {
                    val x = index * cellWidth
                    val h = (value * valueUnitHeight)
                    canvas.drawLine(x, canvas.height.toFloat(), x, canvas.height - h, cellPaint)
                }
            }
        }
        super.onDraw(canvas)
    }

    private var valueUnitHeight: Float = 0f
    private var threshold: Float = 0f
    fun setUnit(unit: Int) {
        threshold = unit.toFloat()
        invalidate()
    }

    private var maxValue = 0
    fun setMaxValue(max: Int) {
        console.d("setMaxValue")
        maxValue = max
    }

    private var values: ShortArray? = null

    fun setValues(values: ShortArray) {
        this.values = values
        postInvalidate()
    }

    fun clear() {
        values = null
        invalidate()
    }
}