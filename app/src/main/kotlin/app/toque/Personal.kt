package app.toque

import android.content.Context
import app.toque.core.json.Json
import java.io.File

/**
 * Optional personal touches, read from `Android/data/app.toque/files/personal.json` on the device
 * (never shipped in the app, so nothing private ends up in the public repository):
 *
 * {
 *   "title": "Home screen title",
 *   "greetings": { "morning": [...], "day": [...], "evening": [...], "night": [...] },
 *   "messages": ["Rotating note on the home screen", ...],
 *   "signature": "Small line under each note"
 * }
 *
 * Every field is optional. Without the file Toque looks as usual.
 */
class Personal(
    val title: String? = null,
    val greetings: List<List<String>> = emptyList(),
    val messages: List<String> = emptyList(),
    val signature: String? = null,
) {
    /** Greeting for a time slot (0 morning, 1 day, 2 evening, 3 night), or null. */
    fun greeting(slot: Int): String? = greetings.getOrNull(slot)?.takeIf { it.isNotEmpty() }?.random()

    companion object {
        const val FILE = "personal.json"

        fun load(context: Context, log: Logbook? = null): Personal {
            val f = try { context.getExternalFilesDir(null)?.let { File(it, FILE) } } catch (_: Exception) { null }
            if (f == null || !f.isFile) return Personal()
            return try {
                parse(f.readText())
            } catch (e: Exception) {
                log?.write("perso", "$FILE ignoré : ${e.message}")
                Personal()
            }
        }

        fun parse(json: String): Personal {
            val o = Json.parse(json) as? Map<*, *> ?: return Personal()
            fun strings(v: Any?): List<String> = (v as? List<*>).orEmpty().mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotEmpty) }
            val g = o["greetings"] as? Map<*, *>
            return Personal(
                title = (o["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
                greetings = if (g == null) emptyList() else listOf("morning", "day", "evening", "night").map { strings(g[it]) },
                messages = strings(o["messages"]),
                signature = (o["signature"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
}
