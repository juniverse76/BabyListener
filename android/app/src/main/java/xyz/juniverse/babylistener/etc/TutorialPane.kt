package xyz.juniverse.babylistener.etc

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import xyz.juniverse.babylistener.R

/**
 * Created by juniverse on 05/12/2017.
 */
class TutorialPane @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    class ViewTextPair(val view: View, val text: String)

    private val location = IntArray(2)
    private val bgColor = 0xaa000000.toInt()
    private val pen = TextPaint()
    private val textPadding: Int
    private val targets = ArrayList<ViewTextPair>()
    private var ti = 0          // tutorial index
    private var staticLayout: StaticLayout? = null

    interface Progress {
        fun onProgress(index: Int)
        fun onFinished()
    }

    init {
        pen.color = Color.parseColor("#ffffff")
        pen.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        pen.textAlign = Paint.Align.CENTER

        setOnClickListener {
//            console.d("tutorial click")
            if (!showNextTarget()) {
                visibility = GONE
                progress?.onFinished()
            }
        }

        textPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics).toInt()
    }

    override fun draw(canvas: Canvas?) {
        val target = currentTarget?.view
        var text = currentTarget?.text
        if (target == null) {
            super.draw(canvas)
            return
        }

        location[0] = 0
        location[1] = 0
        getPosition(target, location)
        canvas?.clipRect(location[0].toFloat(), location[1].toFloat(), (location[0] + target.width).toFloat(), (location[1] + target.height).toFloat(), Region.Op.DIFFERENCE)

        canvas?.drawColor(bgColor)

        if (ti == targets.size - 1)
            text += resources.getString(R.string.tutorial_finish)
        else
            text += resources.getString(R.string.tutorial_continue)

        if (staticLayout == null)
            staticLayout = StaticLayout(text, pen, width - textPadding, Layout.Alignment.ALIGN_NORMAL, 1f, 1f, false)

        val textX = (width / 2).toFloat()
        val textY = if ((location[1] + target.height / 2) < (height / 2)) (location[1] + target.height + 100).toFloat() else (location[1] - 100 - staticLayout!!.height).toFloat()
        canvas?.save()
        canvas?.translate(textX, textY)
        staticLayout?.draw(canvas)
        canvas?.restore()

        super.draw(canvas)
    }

    private fun getPosition(view: View?, location: IntArray) {
        if (view == null) return

        location[0] += view.left
        location[1] += view.top
        if (parent != view.parent)
            getPosition(view.parent as View, location)
    }

    private var currentTarget: ViewTextPair? = null
    private fun showNextTarget(): Boolean {
        ti++
        if (ti >= targets.size)
            return false

        progress?.onProgress(ti)

        staticLayout = null
        currentTarget = targets[ti]
        invalidate()

        return true
    }

    fun addTargets(views: Array<View>, texts: Array<String>): Boolean {
        check(views.size == texts.size)
        (0 until views.size).mapTo(targets) { ViewTextPair(views[it], texts[it]) }
        return true
    }

    private var progress: Progress? = null
    fun startTutorial(onProgress: (Int) -> Unit, onFinished: () -> Unit) {
        progress = object: Progress {
            override fun onProgress(index: Int) = onProgress(index)
            override fun onFinished() = onFinished()
        }

        ti = -1
        showNextTarget()
    }

}