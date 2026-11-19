package app.toque.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Small on/off switch (track + knob). The surrounding row handles taps. */
class ToggleSwitch(context: Context, initial: Boolean) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var pos = if (initial) 1f else 0f
    private var anim: ValueAnimator? = null

    var checked: Boolean = initial
        set(v) {
            if (v == field) return
            field = v
            anim?.cancel()
            anim = ValueAnimator.ofFloat(pos, if (v) 1f else 0f).apply {
                duration = 140
                addUpdateListener { pos = it.animatedValue as Float; invalidate() }
                start()
            }
        }

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        track.color = blend(Ui.SURFACE3, Ui.ACCENT, pos)
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, track)
        val inset = Ui.uf(4f)
        val kr = r - inset
        val cx = r + (w - 2 * r) * pos
        knob.color = blend(Ui.TEXT2, Ui.ON_ACCENT, pos)
        canvas.drawCircle(cx, r, kr, knob)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt() and 0xFF
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
