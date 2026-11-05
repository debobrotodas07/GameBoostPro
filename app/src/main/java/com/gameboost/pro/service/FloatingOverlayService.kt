package com.gameboost.pro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gameboost.pro.AimbotEngine
import com.gameboost.pro.GameBoostApplication
import com.gameboost.pro.LocationSpoofer
import com.gameboost.pro.R
import com.gameboost.pro.advantage.AdvantageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var collapsedView: View? = null
    private var expandedView: View? = null
    private var isExpanded = false
    private var isCollapsedAnimating = false
    private var isServiceRunning = true

    private var aimbotEngine: AimbotEngine? = null
    private var locationSpoofer: LocationSpoofer? = null
    private var advantageEngine: AdvantageEngine? = null

    private var aimbotSwitch: Switch? = null
    private var locationSwitch: Switch? = null
    private var smoothnessSlider: android.widget.SeekBar? = null
    private var fovSlider: android.widget.SeekBar? = null
    private var speedSlider: android.widget.SeekBar? = null
    private var fpsSlider: android.widget.SeekBar? = null
    private var cpuSlider: android.widget.SeekBar? = null

    private val prefs: SharedPreferences by lazy { GameBoostApplication.getPrefs() }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        aimbotEngine = AimbotEngine(this)
        locationSpoofer = LocationSpoofer(this)
        advantageEngine = AdvantageEngine.instance ?: AdvantageEngine(this)
        createCollapsedView()
        createExpandedView()
        addCollapsedView()
        startForegroundDetection()
    }

    private fun createCollapsedView() {
        collapsedView = LayoutInflater.from(this).inflate(R.layout.overlay_collapsed, null)
        val params = WindowManager.LayoutParams(
            120, 120,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }
        windowManager.addView(collapsedView, params)

        collapsedView?.setOnClickListener { expandSidebar() }
        collapsedView?.setOnLongClickListener {
            stopSelf()
            true
        }
    }

    private fun createExpandedView() {
        expandedView = LayoutInflater.from(this).inflate(R.layout.overlay_expanded, null)
        val params = WindowManager.LayoutParams(
            540, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            x = -540
            y = 0
        }
        // Don't add yet, will add on expand

        setupExpandedControls()
    }

    private fun setupExpandedControls() {
        val view = expandedView!!
        aimbotSwitch = view.findViewById(R.id.switch_aimbot)
        locationSwitch = view.findViewById(R.id.switch_location)
        smoothnessSlider = view.findViewById(R.id.seek_smoothness)
        fovSlider = view.findViewById(R.id.seek_fov)
        speedSlider = view.findViewById(R.id.seek_speed)
        fpsSlider = view.findViewById(R.id.seek_fps)
        cpuSlider = view.findViewById(R.id.seek_cpu)

        val headChip = view.findViewById<TextView>(R.id.chip_head)
        val chestChip = view.findViewById<TextView>(R.id.chip_chest)
        val fixedRadio = view.findViewById<TextView>(R.id.radio_fixed)
        val randomRadio = view.findViewById<TextView>(R.id.radio_random)
        val presetRadio = view.findViewById<TextView>(R.id.radio_preset)
        val presetDropdown = view.findViewById<android.widget.Spinner>(R.id.spinner_preset)
        val fixedLat = view.findViewById<android.widget.EditText>(R.id.edit_lat)
        val fixedLon = view.findViewById<android.widget.EditText>(R.id.edit_lon)
        val btnSetFixed = view.findViewById<android.widget.Button>(R.id.btn_set_fixed)
        val radiusSlider = view.findViewById<android.widget.SeekBar>(R.id.seek_radius)
        val btnPanic = view.findViewById<android.widget.Button>(R.id.btn_panic)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)

        aimbotSwitch?.setOnCheckedChangeListener { _, checked ->
            aimbotEngine?.active = checked
            if (checked) {
                smoothnessSlider?.visibility = View.VISIBLE
                fovSlider?.visibility = View.VISIBLE
                headChip?.visibility = View.VISIBLE
                chestChip?.visibility = View.VISIBLE
                aimbotEngine?.smoothness = smoothnessSlider?.progress.toFloat() / 100f ?: 0.4f
                aimbotEngine?.fovRadius = fovSlider?.progress.toFloat() ?: 150f
            } else {
                smoothnessSlider?.visibility = View.GONE
                fovSlider?.visibility = View.GONE
                headChip?.visibility = View.GONE
                chestChip?.visibility = View.GONE
            }
        }

        smoothnessSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                aimbotEngine?.smoothness = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        fovSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                aimbotEngine?.fovRadius = progress.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        headChip?.setOnClickListener {
            aimbotEngine?.targetBone = "HEAD"
            updateBoneChips()
        }
        chestChip?.setOnClickListener {
            aimbotEngine?.targetBone = "CHEST"
            updateBoneChips()
        }

        locationSwitch?.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                // Show location options
                fixedRadio?.visibility = View.VISIBLE
                randomRadio?.visibility = View.VISIBLE
                presetRadio?.visibility = View.VISIBLE
                presetDropdown?.visibility = View.VISIBLE
                fixedLat?.visibility = View.VISIBLE
                fixedLon?.visibility = View.VISIBLE
                btnSetFixed?.visibility = View.VISIBLE
                radiusSlider?.visibility = View.VISIBLE
                // Default to preset
                locationSpoofer?.spoofPreset("Tokyo JP")
            } else {
                locationSpoofer?.stop()
                fixedRadio?.visibility = View.GONE
                randomRadio?.visibility = View.GONE
                presetRadio?.visibility = View.GONE
                presetDropdown?.visibility = View.GONE
                fixedLat?.visibility = View.GONE
                fixedLon?.visibility = View.GONE
                btnSetFixed?.visibility = View.GONE
                radiusSlider?.visibility = View.GONE
            }
        }

        btnSetFixed?.setOnClickListener {
            val lat = fixedLat?.text?.toString()?.toDoubleOrNull()
            val lon = fixedLon?.text?.toString()?.toDoubleOrNull()
            lat?.let { lt -> lon?.let { ln -> locationSpoofer?.spoofFixed(lt, ln) } }
        }

        radiusSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // Random radius handled when random is selected
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val radius = seekBar?.progress?.toDouble() ?: 1.0
                // Get current location and spoof random
                // For now just use a default
            }
        })

        speedSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val multiplier = 1.0f + (progress / 100f) * 4.0f // 1.0x to 5.0x
                advantageEngine?.applySpeedHack(multiplier)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        fpsSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // FPS target display only
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        cpuSlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                com.gameboost.pro.ShizukuManager.setCpuGovernor(if (progress > 50) "performance" else "ondemand")
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        btnPanic?.setOnClickListener { panicMode() }
        btnPanic?.setOnLongClickListener {
            stopSelf()
            true
        }
        btnClose?.setOnClickListener { collapseSidebar() }
    }

    private fun updateBoneChips() {
        val headChip = expandedView?.findViewById<TextView>(R.id.chip_head)
        val chestChip = expandedView?.findViewById<TextView>(R.id.chip_chest)
        val isHead = aimbotEngine?.targetBone == "HEAD"
        headChip?.setBackgroundResource(if (isHead) R.drawable.chip_active else R.drawable.chip_inactive)
        chestChip?.setBackgroundResource(if (!isHead) R.drawable.chip_active else R.drawable.chip_inactive)
    }

    private fun expandSidebar() {
        if (isExpanded || isCollapsedAnimating) return
        isCollapsedAnimating = true
        val expanded = expandedView!!
        val params = expanded.layoutParams as WindowManager.LayoutParams
        windowManager.addView(expanded, params)
        animateSlide(expanded, -540, 0, 200) { isExpanded = true; isCollapsedAnimating = false }
    }

    private fun collapseSidebar() {
        if (!isExpanded || isCollapsedAnimating) return
        isCollapsedAnimating = true
        val expanded = expandedView!!
        animateSlide(expanded, 0, -540, 200) {
            windowManager.removeViewImmediate(expanded)
            isExpanded = false
            isCollapsedAnimating = false
        }
    }

    private fun animateSlide(view: View, fromX: Int, toX: Int, duration: Int, onEnd: () -> Unit) {
        val animation = object : Animation() {
            init { this.duration = duration.toLong(); interpolator = AccelerateDecelerateInterpolator() }
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val params = view.layoutParams as WindowManager.LayoutParams
                params.x = (fromX + (toX - fromX) * interpolatedTime).toInt()
                windowManager.updateViewLayout(view, params)
            }
        }
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) { onEnd() }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view.startAnimation(animation)
    }

    private fun panicMode() {
        aimbotEngine?.reset()
        locationSpoofer?.stop()
        advantageEngine?.applySpeedHack(1.0f)
        aimbotSwitch?.isChecked = false
        locationSwitch?.isChecked = false
        smoothnessSlider?.visibility = View.GONE
        fovSlider?.visibility = View.GONE
        // Restore backed up bytes would go here
        collapseSidebar()
    }

    private fun startForegroundDetection() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isServiceRunning) {
                val isFfForeground = isFreeFireForeground()
                if (isFfForeground && !isServiceRunning) {
                    // Already running
                } else if (!isFfForeground && isServiceRunning) {
                    // Stop service when FF not foreground
                    Handler(Looper.getMainLooper()).post { stopSelf() }
                    break
                }
                delay(500)
            }
        }
    }

    private fun isFreeFireForeground(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            now - 10000,
            now
        )
        return stats?.any { it.packageName == "com.dts.freefireth" || it.packageName == "com.dts.freefiremax" } ?: false
    }

    private fun addCollapsedView() {
        // Already added in createCollapsedView
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        collapseSidebar()
        if (collapsedView != null) {
            try { windowManager.removeView(collapsedView) } catch (e: Exception) {}
        }
        if (expandedView != null && isExpanded) {
            try { windowManager.removeView(expandedView) } catch (e: Exception) {}
        }
        aimbotEngine?.reset()
        locationSpoofer?.stop()
        super.onDestroy()
    }
}