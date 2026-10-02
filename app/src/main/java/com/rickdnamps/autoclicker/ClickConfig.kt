package com.rickdnamps.autoclicker

import android.content.Context
import android.graphics.Point

enum class StopMode { NEVER, COUNT, TIME }

/** Every user setting, persisted in SharedPreferences. */
data class ClickConfig(
    val intervalMs: Long = 100,
    val pressMs: Long = 10,
    val randomInterval: Boolean = false,
    val randomIntervalMs: Long = 20,
    val randomOffset: Boolean = false,
    val randomOffsetPx: Int = 8,
    val simultaneous: Boolean = false,
    val stopMode: StopMode = StopMode.NEVER,
    val stopCount: Long = 100,
    val stopSeconds: Long = 60,
    val targetSizeDp: Int = 56,
    val panelScalePercent: Int = 100,
) {
    companion object {
        const val MIN_INTERVAL_MS = 1L
        const val MIN_PRESS_MS = 1L
        const val MAX_PRESS_MS = 10_000L

        private const val PREFS = "autoclicker"
        private const val KEY_TARGETS = "targets"

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun load(context: Context): ClickConfig {
            val p = prefs(context)
            val d = ClickConfig()
            return ClickConfig(
                intervalMs = p.getLong("intervalMs", d.intervalMs),
                pressMs = p.getLong("pressMs", d.pressMs),
                randomInterval = p.getBoolean("randomInterval", d.randomInterval),
                randomIntervalMs = p.getLong("randomIntervalMs", d.randomIntervalMs),
                randomOffset = p.getBoolean("randomOffset", d.randomOffset),
                randomOffsetPx = p.getInt("randomOffsetPx", d.randomOffsetPx),
                simultaneous = p.getBoolean("simultaneous", d.simultaneous),
                stopMode = runCatching { StopMode.valueOf(p.getString("stopMode", null)!!) }
                    .getOrDefault(d.stopMode),
                stopCount = p.getLong("stopCount", d.stopCount),
                stopSeconds = p.getLong("stopSeconds", d.stopSeconds),
                targetSizeDp = p.getInt("targetSizeDp", d.targetSizeDp),
                panelScalePercent = p.getInt("panelScalePercent", d.panelScalePercent),
            ).sanitized()
        }

        /** Window positions (top-left x,y) of the click targets. */
        fun loadTargets(context: Context): List<Point> =
            prefs(context).getString(KEY_TARGETS, "").orEmpty()
                .split(';')
                .mapNotNull { entry ->
                    val parts = entry.split(',')
                    val x = parts.getOrNull(0)?.toIntOrNull()
                    val y = parts.getOrNull(1)?.toIntOrNull()
                    if (x != null && y != null) Point(x, y) else null
                }

        fun saveTargets(context: Context, targets: List<Point>) {
            prefs(context).edit()
                .putString(KEY_TARGETS, targets.joinToString(";") { "${it.x},${it.y}" })
                .apply()
        }
    }

    fun sanitized() = copy(
        intervalMs = intervalMs.coerceAtLeast(MIN_INTERVAL_MS),
        pressMs = pressMs.coerceIn(MIN_PRESS_MS, MAX_PRESS_MS),
        randomIntervalMs = randomIntervalMs.coerceAtLeast(0),
        randomOffsetPx = randomOffsetPx.coerceAtLeast(0),
        stopCount = stopCount.coerceAtLeast(1),
        stopSeconds = stopSeconds.coerceAtLeast(1),
        targetSizeDp = targetSizeDp.coerceIn(32, 120),
        panelScalePercent = panelScalePercent.coerceIn(60, 160),
    )

    fun save(context: Context) {
        prefs(context).edit()
            .putLong("intervalMs", intervalMs)
            .putLong("pressMs", pressMs)
            .putBoolean("randomInterval", randomInterval)
            .putLong("randomIntervalMs", randomIntervalMs)
            .putBoolean("randomOffset", randomOffset)
            .putInt("randomOffsetPx", randomOffsetPx)
            .putBoolean("simultaneous", simultaneous)
            .putString("stopMode", stopMode.name)
            .putLong("stopCount", stopCount)
            .putLong("stopSeconds", stopSeconds)
            .putInt("targetSizeDp", targetSizeDp)
            .putInt("panelScalePercent", panelScalePercent)
            .apply()
    }
}
