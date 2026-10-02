package com.rickdnamps.autoclicker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service: required by Android to inject taps (dispatchGesture)
 * and lets us draw the floating panel without the "draw over apps" permission.
 */
class AutoClickService : AccessibilityService() {

    companion object {
        /** Non-null while the service is enabled and connected. */
        var instance: AutoClickService? = null
            private set
    }

    var overlay: OverlayController? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = OverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        overlay?.stopClicking()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        overlay?.hide()
        overlay = null
        instance = null
    }
}
