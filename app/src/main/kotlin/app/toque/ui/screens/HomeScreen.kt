package app.toque.ui.screens

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import app.toque.Personal
import app.toque.Prefs
import app.toque.core.i18n.I18n
import app.toque.core.i18n.tr
import app.toque.core.machine.Snapshot
import app.toque.ui.Icon
import app.toque.ui.LoveNote
import app.toque.ui.MainActivity
import app.toque.ui.Screen
import app.toque.ui.Ui
import java.text.SimpleDateFormat
import java.util.Date

class HomeScreen(a: MainActivity) : Screen(a) {
    private val personal = Personal.load(a, a.app.log)
    override val title = personal.title ?: "Toque"
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var greeting: TextView
    private lateinit var recipesSub: TextView
    private lateinit var favSub: TextView
    private lateinit var settingsSub: TextView
    private lateinit var resumeCard: LinearLayout
    private lateinit var resumeText: TextView
    private lateinit var love: LoveNote
    private var unsub: (() -> Unit)? = null
    private var greetingSlot = -1

    private val tick = object : Runnable {
        override fun run() {
            refreshClock()
            view.postDelayed(this, 10_000)
        }
    }

    override fun build(): View {
        val root = Ui.horizontal(c, Gravity.TOP)
        root.setPadding(Ui.u(28), Ui.u(10), Ui.u(24), Ui.u(24))

        val left = Ui.vertical(c)
        greeting = Ui.ellipsize(Ui.text(c, "", 24f, if (personal.greetings.isEmpty()) Ui.TEXT2 else LoveNote.ROSE, Ui.REGULAR))
        left.addView(greeting, Ui.lp(t = 8))
        clock = Ui.text(c, "", 100f, Ui.TEXT, Ui.LIGHT)
        left.addView(clock, Ui.lp(t = 2))
        date = Ui.text(c, "", 22f, Ui.TEXT2, Ui.LIGHT)
        left.addView(date, Ui.lp(t = 2))

        val search = Ui.horizontal(c)
        search.setPadding(Ui.u(24), 0, Ui.u(24), 0)
        search.background = Ui.pressable(Ui.round(Ui.SURFACE, 36f, Ui.STROKE, 1f), 36f)
        search.addView(Icon(c, Icon.Kind.SEARCH, Ui.TEXT2), LinearLayout.LayoutParams(Ui.u(28), Ui.u(28)).apply { rightMargin = Ui.u(14) })
        search.addView(Ui.text(c, tr("Rechercher une recette…"), 21f, Ui.TEXT3))
        search.isClickable = true
        search.setOnClickListener { a.push(RecipesScreen(a, focusSearch = true)) }
        left.addView(search, Ui.lp(Ui.MATCH, Ui.u(64), t = 22))

        resumeCard = Ui.horizontal(c)
        resumeCard.setPadding(Ui.u(20), Ui.u(10), Ui.u(16), Ui.u(10))
        resumeCard.background = Ui.pressable(Ui.round(Ui.ACCENT_DIM, 24f), 24f)
        resumeCard.addView(Icon(c, Icon.Kind.PLAY, Ui.ACCENT), LinearLayout.LayoutParams(Ui.u(26), Ui.u(26)).apply { rightMargin = Ui.u(14) })
        resumeText = Ui.ellipsize(Ui.text(c, "", 18f, Ui.TEXT, Ui.MEDIUM), 1)
        resumeCard.addView(resumeText, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        resumeCard.isClickable = true
        resumeCard.setOnClickListener {
            val r = a.app.recipes.get(a.prefs.guidedRecipe)
            if (r != null) a.push(CookScreen(a, r, a.prefs.guidedStep)) else { a.prefs.guidedRecipe = ""; refresh() }
        }
        left.addView(resumeCard, Ui.lp(Ui.MATCH, Ui.WRAP, t = 12))
        left.addView(Ui.spacer(c), LinearLayout.LayoutParams(1, 0, 1f))
        love = LoveNote(c, personal.messages, personal.signature)
        if (personal.messages.isNotEmpty()) left.addView(love, Ui.lp(Ui.MATCH, love.fixedHeight, t = 12))
        root.addView(left, LinearLayout.LayoutParams(0, Ui.MATCH, 0.42f).apply { rightMargin = Ui.u(28) })

        // Two columns x three rows: wider tiles, so German labels fit on one line.
        val grid = Ui.vertical(c)
        val rows = List(3) { Ui.horizontal(c) }
        val (t1, s1) = tile(Icon.Kind.BOOK, tr("Recettes")) { a.push(RecipesScreen(a)) }
        recipesSub = s1
        rows[0].addView(t1, tileLp(true))
        rows[0].addView(tile(Icon.Kind.POT, tr("Cuisson\nmanuelle"), tr("Temps, chaleur, vitesse")) { a.push(ManualScreen(a)) }.first, tileLp(false))
        rows[1].addView(tile(Icon.Kind.KNEAD, tr("Programmes"), tr("Pétrir, vapeur, rissoler")) { a.push(ProgramsScreen(a)) }.first, tileLp(true))
        rows[1].addView(tile(Icon.Kind.SCALE, tr("Balance"), tr("Peser au gramme")) { a.push(ScaleScreen(a)) }.first, tileLp(false))
        val (t5, s5) = tile(Icon.Kind.STAR, tr("Favoris")) { a.push(RecipesScreen(a, favorites = true)) }
        favSub = s5
        rows[2].addView(t5, tileLp(true))
        val (t6, s6) = tile(Icon.Kind.SETTINGS, tr("Réglages")) { a.push(SettingsScreen(a)) }
        settingsSub = s6
        rows[2].addView(t6, tileLp(false))
        rows.forEachIndexed { i, r ->
            grid.addView(r, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f).apply { if (i < 2) bottomMargin = Ui.u(16) })
        }
        root.addView(grid, LinearLayout.LayoutParams(0, Ui.MATCH, 0.58f))
        return root
    }

    private fun tileLp(gap: Boolean) = LinearLayout.LayoutParams(0, Ui.MATCH, 1f).apply { if (gap) rightMargin = Ui.u(16) }

    private fun tile(icon: Icon.Kind, title: String, sub: String = "", onClick: () -> Unit): Pair<LinearLayout, TextView> {
        val t = Ui.horizontal(c)
        t.setPadding(Ui.u(18), Ui.u(14), Ui.u(16), Ui.u(14))
        t.background = Ui.pressable(Ui.round(Ui.SURFACE, 20f, Ui.SURFACE2, 1f), 20f)
        val badge = android.widget.FrameLayout(c)
        badge.background = Ui.round(Ui.ACCENT_DIM, 22f)
        badge.addView(Icon(c, icon, Ui.ACCENT), Ui.flp(Ui.u(26), Ui.u(26), Gravity.CENTER))
        t.addView(badge, LinearLayout.LayoutParams(Ui.u(48), Ui.u(48)).apply { rightMargin = Ui.u(14) })
        val texts = Ui.vertical(c)
        texts.addView(Ui.ellipsize(Ui.text(c, title.replace('\n', ' '), 22f, Ui.TEXT, Ui.MEDIUM)))
        val s = Ui.ellipsize(Ui.text(c, sub, 16f, Ui.TEXT2).apply { setLineSpacing(0f, 1.05f) }, 2)
        texts.addView(s, Ui.lp(t = 4))
        t.addView(texts, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        t.isClickable = true
        t.setOnClickListener { onClick() }
        return t to s
    }

    private fun refreshClock() {
        clock.text = MainActivity.now()
        date.text = SimpleDateFormat(tr("EEEE d MMMM"), I18n.locale).format(Date()).replaceFirstChar { it.uppercaseChar() }
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val slot = when (h) { in 5..10 -> 0; in 11..17 -> 1; in 18..22 -> 2; else -> 3 }
        if (slot != greetingSlot) {
            greetingSlot = slot
            greeting.text = personal.greeting(slot) ?: when (slot) {
                0 -> tr("Bonjour")
                1 -> tr("Bon après-midi")
                else -> tr("Bonsoir")
            }
        }
    }

    private fun refresh() {
        val repo = a.app.recipes
        val n = repo.count
        recipesSub.text = when {
            !repo.loaded -> tr("Chargement…")
            n == 0 -> tr("Aucune recette Lidl pour l'instant")
            else -> tr("%d recettes Lidl", n)
        }
        val favs = repo.index.entries.count { it.favorite }
        favSub.text = if (favs == 0) tr("Aucun favori") else if (favs == 1) tr("1 favori") else tr("%d favoris", favs)
        settingsSub.text = a.prefs.mode.label
        val guided = a.prefs.guidedRecipe
        val title = if (guided.isEmpty()) null else repo.index.entries.firstOrNull { it.id == guided }?.title
        resumeCard.visibility = if (title != null) View.VISIBLE else View.GONE
        if (title != null) resumeText.text = tr("Reprendre « %s » — étape %d", title, a.prefs.guidedStep + 1)
    }

    override fun onShow() {
        greetingSlot = -1 // a new greeting every time the home screen appears
        refreshClock()
        refresh()
        unsub = a.app.recipes.onChange { refresh() }
        view.removeCallbacks(tick)
        view.postDelayed(tick, 10_000)
        love.start()
    }

    override fun onHide() {
        love.stop()
        unsub?.invoke()
        view.removeCallbacks(tick)
    }

    override fun onSnapshot(s: Snapshot) {
        if (a.prefs.mode == Prefs.Mode.DEMO) settingsSub.text = a.prefs.mode.label
    }
}
