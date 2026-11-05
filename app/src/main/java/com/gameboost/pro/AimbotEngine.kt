package com.gameboost.pro

import android.content.Context
import android.graphics.Path
import android.view.accessibility.GestureDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AimbotEngine(val context: Context) {

    var active = false
    var smoothness = 0.4f
    var fovRadius = 150f
    var targetBone = "HEAD"

    private val sw = context.resources.displayMetrics.widthPixels.toFloat()
    private val sh = context.resources.displayMetrics.heightPixels.toFloat()
    private val cx = sw / 2f
    private val cy = sh / 2f

    private var tickJob: kotlinx.coroutines.Job? = null

    fun tick(entities: List<EntityData>, viewMatrix: FloatArray) {
        if (!active) return
        var best: EntityData? = null
        var bestDist = Float.MAX_VALUE
        for (e in entities) {
            if (!e.isAlive || e.isTeammate) continue
            val bone = if (targetBone == "HEAD") e.boneHead else e.boneChest
            val s = worldToScreen(bone, viewMatrix) ?: continue
            val d = Math.hypot((s.x - cx).toDouble(), (s.y - cy).toDouble()).toFloat()
            if (d < fovRadius && d < bestDist) {
                bestDist = d
                best = e
                best.screenPos = s
            }
        }
        best?.let {
            val mx = (it.screenPos!!.x - cx) * (1f - smoothness)
            val my = (it.screenPos!!.y - cy) * (1f - smoothness)
            injectSwipe(cx, cy, cx + mx, cy + my)
        }
    }

    fun startTickLoop(getEntities: () -> List<EntityData>, getViewMatrix: () -> FloatArray) {
        tickJob?.cancel()
        tickJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val entities = getEntities()
                val viewMatrix = getViewMatrix()
                tick(entities, viewMatrix)
                delay(16)
            }
        }
    }

    fun stopTickLoop() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 8L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        GameBoostAccessibilityService.instance?.dispatchGesture(gesture, null, null)
    }

    private fun worldToScreen(pos: Vector3, m: FloatArray): Vector2? {
        val cx2 = m[0] * pos.x + m[1] * pos.y + m[2] * pos.z + m[3]
        val cy2 = m[4] * pos.x + m[5] * pos.y + m[6] * pos.z + m[7]
        val cw = m[12] * pos.x + m[13] * pos.y + m[14] * pos.z + m[15]
        if (cw < 0.1f) return null
        return Vector2((cx2 / cw + 1f) * 0.5f * sw, (-cy2 / cw + 1f) * 0.5f * sh)
    }

    fun reset() {
        active = false
        stopTickLoop()
    }
}