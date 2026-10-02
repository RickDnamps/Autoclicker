package com.rickdnamps.autoclicker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Floating control bar + draggable click targets, drawn as accessibility overlays.
 */
class OverlayController(private val service: AutoClickService) {

    private companion object {
        /** The controller whose panel is on screen; there must never be two. */
        var active: OverlayController? = null
    }

    private class Target(val view: TargetView, val params: WindowManager.LayoutParams)

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = service.resources.displayMetrics.density
    private val engine = ClickEngine(service) { onEngineStopped() }

    private var config = ClickConfig.load(service)
    private var panel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var playButton: ImageButton? = null
    private val targets = mutableListOf<Target>()

    val isShowing: Boolean get() = panel != null

    /** True while the panel is hidden because the user opened the settings from it. */
    private var hiddenForSettings = false

    fun show() {
        hiddenForSettings = false
        if (isShowing) return
        active?.takeIf { it !== this }?.hide()
        active = this
        config = ClickConfig.load(service)
        createPanel(Point(dp(8), dp(160)))
        val saved = ClickConfig.loadTargets(service)
        if (saved.isEmpty()) addTarget() else saved.forEach { addTarget(it) }
    }

    fun hide() {
        hiddenForSettings = false
        if (active === this) active = null
        if (!isShowing) return
        engine.stop()
        saveTargets()
        targets.forEach { removeView(it.view) }
        targets.clear()
        panel?.let { removeView(it) }
        panel = null
        panelParams = null
        playButton = null
    }

    fun stopClicking() = engine.stop()

    /** Brings the panel back when the user leaves the settings opened from the gear. */
    fun restoreAfterSettings() {
        if (hiddenForSettings) show()
    }

    /** Re-reads the settings and resizes the bar / targets accordingly. */
    fun applyConfig() {
        config = ClickConfig.load(service)
        if (!isShowing) return
        engine.stop()
        panelParams?.let { old ->
            panel?.let { removeView(it) }
            createPanel(Point(old.x, old.y))
        }
        val size = targetSizePx()
        for (t in targets) {
            val delta = (t.params.width - size) / 2
            t.params.x += delta
            t.params.y += delta
            t.params.width = size
            t.params.height = size
            wm.updateViewLayout(t.view, t.params)
        }
        saveTargets()
    }

    // ---------------------------------------------------------------- panel

    private fun createPanel(position: Point) {
        val scale = config.panelScalePercent / 100f
        val buttonSize = (dp(44) * scale).toInt()
        val iconPadding = (dp(10) * scale).toInt()

        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.bg_panel)
            val pad = (dp(4) * scale).toInt()
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        val params = overlayParams(position.x, position.y,
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)

        val handle = ImageView(service).apply {
            setImageResource(R.drawable.ic_drag)
            contentDescription = service.getString(R.string.cd_drag)
            setPadding(iconPadding, iconPadding / 2, iconPadding, iconPadding / 2)
            setOnTouchListener(DragListener(layout, params))
        }
        layout.addView(handle, LinearLayout.LayoutParams(buttonSize, buttonSize * 3 / 4))

        fun button(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) =
            ImageButton(service).apply {
                setImageResource(icon)
                setBackgroundResource(R.drawable.bg_panel_button)
                contentDescription = service.getString(description)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                setOnClickListener { onClick() }
            }.also {
                val lp = LinearLayout.LayoutParams(buttonSize, buttonSize)
                lp.topMargin = (dp(2) * scale).toInt()
                layout.addView(it, lp)
            }

        playButton = button(R.drawable.ic_play, R.string.cd_play) { toggleRun() }
        button(R.drawable.ic_add, R.string.cd_add) { engine.stop(); addTarget() }
        button(R.drawable.ic_remove, R.string.cd_remove) { engine.stop(); removeLastTarget() }
        button(R.drawable.ic_settings, R.string.cd_settings) { openSettings() }
        button(R.drawable.ic_close, R.string.cd_close) { ClickEngine.stopAll(); hide() }

        wm.addView(layout, params)
        panel = layout
        panelParams = params
    }

    private fun updatePlayIcon() {
        playButton?.setImageResource(if (engine.isRunning) R.drawable.ic_stop else R.drawable.ic_play)
    }

    private fun openSettings() {
        // Clicking always stops and the panel steps aside while the settings are open.
        ClickEngine.stopAll()
        hide()
        hiddenForSettings = true
        service.startActivity(
            Intent(service, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    // -------------------------------------------------------------- clicking

    private fun toggleRun() {
        // Stop must always work, whichever engine is clicking.
        if (ClickEngine.isAnyRunning) {
            ClickEngine.stopAll()
            updatePlayIcon()
            return
        }
        if (targets.isEmpty()) {
            toast(R.string.toast_no_target)
            return
        }
        config = ClickConfig.load(service)
        val points = targets.map { t ->
            val location = IntArray(2)
            t.view.getLocationOnScreen(location)
            PointF(location[0] + t.view.width / 2f, location[1] + t.view.height / 2f)
        }
        // Let the injected taps go through the markers to the game underneath.
        setTargetsTouchable(false)
        engine.start(points, config)
        updatePlayIcon()
    }

    private fun onEngineStopped() {
        setTargetsTouchable(true)
        updatePlayIcon()
    }

    // --------------------------------------------------------------- targets

    private fun addTarget(position: Point? = null) {
        val size = targetSizePx()
        val pos = position ?: run {
            val metrics = service.resources.displayMetrics
            val shift = dp(24) * (targets.size % 6)
            Point(metrics.widthPixels / 2 - size / 2 + shift, metrics.heightPixels / 2 - size / 2 + shift)
        }
        val view = TargetView(service, targets.size + 1)
        val params = overlayParams(pos.x, pos.y, size, size)
        view.setOnTouchListener(DragListener(view, params))
        wm.addView(view, params)
        targets += Target(view, params)
        if (position == null) saveTargets()
    }

    private fun removeLastTarget() {
        val last = targets.removeLastOrNull() ?: return
        removeView(last.view)
        saveTargets()
    }

    private fun setTargetsTouchable(touchable: Boolean) {
        for (t in targets) {
            t.params.flags = if (touchable) {
                t.params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                t.params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            t.view.alpha = if (touchable) 1f else 0.6f
            if (t.view.isAttachedToWindow) wm.updateViewLayout(t.view, t.params)
        }
    }

    private fun saveTargets() {
        if (!isShowing) return
        ClickConfig.saveTargets(service, targets.map { Point(it.params.x, it.params.y) })
    }

    // --------------------------------------------------------------- helpers

    private fun targetSizePx() = dp(config.targetSizeDp)

    private fun dp(value: Int) = (value * density).toInt()

    private fun toast(@StringRes text: Int) =
        Toast.makeText(service, text, Toast.LENGTH_SHORT).show()

    private fun removeView(view: View) {
        // May fail if the service is already being torn down.
        if (view.isAttachedToWindow) runCatching { wm.removeView(view) }
    }

    private fun overlayParams(x: Int, y: Int, width: Int, height: Int) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    /** Moves [window] while the finger drags the view this listener is attached to. */
    private inner class DragListener(
        private val window: View,
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    if (window.isAttachedToWindow) wm.updateViewLayout(window, params)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> saveTargets()
            }
            return true
        }
    }
}
