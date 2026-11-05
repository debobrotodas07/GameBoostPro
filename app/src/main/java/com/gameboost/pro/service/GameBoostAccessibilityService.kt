package com.gameboost.pro.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.gameboost.pro.AimbotEngine

class GameBoostAccessibilityService : AccessibilityService() {

    companion object {
        @Suppress("UNUSED_PROPERTY")
        var instance: GameBoostAccessibilityService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for gesture injection
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service ready for gesture dispatch
    }
}