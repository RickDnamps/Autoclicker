package com.rickdnamps.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.random.Random

/**
 * Taps the given screen points in a loop using accessibility gestures.
 *
 * The next tap is only dispatched once the previous gesture has finished,
 * because dispatching a new gesture cancels the one in progress.
 */
class ClickEngine(
    private val service: AccessibilityService,
    private val onStopped: () -> Unit,
) {
    companion object {
        private const val START_DELAY_MS = 150L

        /** Only one engine may click at a time. */
        private var running: ClickEngine? = null

        val isAnyRunning: Boolean get() = running != null

        /** Emergency stop for whatever is clicking. */
        fun stopAll() {
            running?.stop()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var points: List<PointF> = emptyList()
    private var config = ClickConfig()

    /** Bumped on every start/stop so callbacks from an old run are ignored. */
    private var generation = 0
    private var startedAt = 0L
    private var nextDueAt = 0L
    private var cycles = 0L
    private var pointIndex = 0

    var isRunning = false
        private set

    private val tick = Runnable { dispatchNext() }

    fun start(points: List<PointF>, config: ClickConfig) {
        if (points.isEmpty()) return
        running?.takeIf { it !== this }?.stop()
        stopInternal()
        running = this
        this.points = points
        this.config = config.sanitized()
        isRunning = true
        generation++
        cycles = 0
        pointIndex = 0
        startedAt = SystemClock.uptimeMillis()
        // Short delay so the markers become click-through before the first tap.
        nextDueAt = startedAt + START_DELAY_MS
        handler.postAtTime(tick, nextDueAt)
    }

    fun stop() {
        if (!isRunning) return
        stopInternal()
        onStopped()
    }

    private fun stopInternal() {
        if (running === this) running = null
        isRunning = false
        generation++
        handler.removeCallbacks(tick)
    }

    private fun limitReached(): Boolean = when (config.stopMode) {
        StopMode.NEVER -> false
        StopMode.COUNT -> cycles >= config.stopCount
        StopMode.TIME -> SystemClock.uptimeMillis() - startedAt >= config.stopSeconds * 1000
    }

    private fun dispatchNext() {
        if (!isRunning) return
        // The service was disconnected or replaced: this engine is orphaned.
        if (AutoClickService.instance !== service || limitReached()) {
            stop()
            return
        }

        val targets = if (config.simultaneous) {
            points.take(GestureDescription.getMaxStrokeCount())
        } else {
            listOf(points[pointIndex])
        }
        val duration = config.pressMs.coerceAtMost(GestureDescription.getMaxGestureDuration())
        val builder = GestureDescription.Builder()
        for (p in targets) {
            val path = Path().apply { moveTo(jitter(p.x), jitter(p.y)) }
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        }

        val gen = generation
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (gen == generation) scheduleNext()
            }

            // Cancelled e.g. when the user touches the screen: keep going.
            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (gen == generation) scheduleNext()
            }
        }
        if (!service.dispatchGesture(builder.build(), callback, handler)) {
            stop()
        }
    }

    private fun scheduleNext() {
        if (config.simultaneous) {
            cycles++
        } else {
            pointIndex++
            if (pointIndex >= points.size) {
                pointIndex = 0
                cycles++
            }
        }

        var interval = config.intervalMs
        if (config.randomInterval && config.randomIntervalMs > 0) {
            interval += Random.nextLong(-config.randomIntervalMs, config.randomIntervalMs + 1)
        }
        nextDueAt += interval.coerceAtLeast(ClickConfig.MIN_INTERVAL_MS)
        // If the device could not keep up, do not burst to catch up.
        val now = SystemClock.uptimeMillis()
        if (nextDueAt < now) nextDueAt = now
        handler.postAtTime(tick, nextDueAt)
    }

    private fun jitter(value: Float): Float {
        val offset = if (config.randomOffset && config.randomOffsetPx > 0) {
            Random.nextInt(-config.randomOffsetPx, config.randomOffsetPx + 1)
        } else {
            0
        }
        // Gesture paths must not have negative coordinates.
        return (value + offset).coerceAtLeast(0f)
    }
}
