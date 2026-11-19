package app.toque.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small personal note for the home screen (messages from personal.json): a softly beating heart and
 * a message that changes every few seconds. Only cheap view-property animations (alpha, translation, scale), and everything
 * stops while the home screen is hidden.
 */
class LoveNote(c: Context, private val messages: List<String>, signature: String?) : LinearLayout(c) {
    /** Fixed height (layout px) so changing messages never move the layout. */
    val fixedHeight = Ui.u(112)
    companion object {
        const val ROSE = 0xFFD9A09A.toInt()
        private const val ROSE_BG = 0xFF2E2221.toInt()
        private const val ROSE_BORDER = 0xFF624541.toInt()
        private const val SHOW_MS = 9_000L
        private const val BEAT_MS = 3_000L

    }

    private val heart = Icon(c, Icon.Kind.HEART, ROSE)
    private val text: TextView = Ui.text(c, "", 20f, Ui.TEXT, Ui.REGULAR)
    private val from: TextView = Ui.text(c, signature ?: "", 15f, Ui.TEXT2, Ui.REGULAR)
    private var order = messages.indices.drop(1).shuffled() // the first message always comes first
    private var pos = -1
    private var running = false

    private val next = object : Runnable {
        override fun run() {
            if (!running) return
            text.animate().alpha(0f).translationY(-Ui.uf(6f)).setDuration(160).withEndAction {
                if (!running) return@withEndAction
                text.text = nextMessage()
                text.translationY = Ui.uf(6f)
                text.animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
            }.start()
            postDelayed(this, SHOW_MS)
        }
    }

    private val beat = object : Runnable {
        override fun run() {
            if (!running) return
            // "lub-dub": two quick pulses, then rest.
            heart.animate().scaleX(1.14f).scaleY(1.14f).setDuration(110).withEndAction {
                heart.animate().scaleX(1f).scaleY(1f).setDuration(120).withEndAction {
                    heart.animate().scaleX(1.08f).scaleY(1.08f).setDuration(90).withEndAction {
                        heart.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
                    }.start()
                }.start()
            }.start()
            postDelayed(this, BEAT_MS)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(Ui.u(20), Ui.u(12), Ui.u(20), Ui.u(12))
        background = Ui.round(ROSE_BG, 20f, ROSE_BORDER, 1f)
        val badge = FrameLayout(c)
        badge.addView(heart, Ui.flp(Ui.u(32), Ui.u(32), Gravity.CENTER))
        addView(badge, LayoutParams(Ui.u(40), Ui.u(40)).apply { rightMargin = Ui.u(16) })
        val col = Ui.vertical(c)
        text.setLineSpacing(Ui.uf(4f), 1f)
        Ui.ellipsize(text, 2)
        col.addView(text)
        if (signature != null) col.addView(from, Ui.lp(t = 6))
        addView(col, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (messages.isNotEmpty()) text.text = nextMessage() else visibility = View.GONE
    }

    private fun nextMessage(): String {
        pos++
        if (pos == 0) return messages[0]
        if (pos > order.size) {
            order = messages.indices.shuffled()
            pos = 1
        }
        return messages[order[pos - 1]]
    }

    fun start() {
        if (running || messages.isEmpty()) return
        running = true
        postDelayed(next, SHOW_MS)
        postDelayed(beat, 600)
    }

    fun stop() {
        running = false
        removeCallbacks(next)
        removeCallbacks(beat)
        heart.animate().cancel()
        text.animate().cancel()
        heart.scaleX = 1f; heart.scaleY = 1f
        text.alpha = 1f; text.translationY = 0f
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility != View.VISIBLE) stop()
    }
}
