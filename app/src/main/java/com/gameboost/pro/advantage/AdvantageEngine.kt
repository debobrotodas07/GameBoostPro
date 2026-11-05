package com.gameboost.pro.advantage

import android.content.Context
import android.util.Log

class AdvantageEngine(private val context: Context) {

    companion object {
        private const val TAG = "AdvantageEngine"
        @Suppress("UNUSED_PROPERTY")
        var instance: AdvantageEngine? = null
    }

    init {
        instance = this
        System.loadLibrary("advantage")
    }

    external fun initSpeedHack(multiplier: Float)
    external fun setSpeedMultiplier(multiplier: Float)
    external fun setAimSmooth(factor: Float)
    external fun patchMemoryValue(lib: String, pattern: ByteArray, replacement: ByteArray): Boolean
    external fun getSpeedMultiplier(): Float
    external fun getAimSmoothFactor(): Float

    fun initialize(multiplier: Float = 1.0f) {
        initSpeedHack(multiplier)
        Log.d(TAG, "AdvantageEngine initialized with speed multiplier: $multiplier")
    }

    fun applySpeedHack(multiplier: Float) {
        setSpeedMultiplier(multiplier)
        Log.d(TAG, "Speed hack applied: ${multiplier}x")
    }

    fun applyAimSmooth(factor: Float) {
        setAimSmooth(factor)
        Log.d(TAG, "Aim smooth applied: $factor")
    }

    fun patchRecoil(libName: String): Boolean {
        val pattern = byteArrayOf(0x00, 0x00, 0x80, 0x3F, 0x00, 0x00, 0x80, 0x3F)
        val replacement = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        return patchMemoryValue(libName, pattern, replacement)
    }

    fun patchNoRecoil(libName: String, offset: Long): Boolean {
        val pattern = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val replacement = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        return patchMemoryValue(libName, pattern, replacement)
    }
}