package com.gameboost.pro.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.roundToInt
import com.gameboost.pro.AimbotEngine
import com.gameboost.pro.GameBoostApplication
import com.gameboost.pro.LocationSpoofer
import com.gameboost.pro.R
import com.gameboost.pro.ShizukuManager
import com.gameboost.pro.advantage.AdvantageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBoostTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = GameBoostApplication.getPrefs()

    var selectedGame by remember { mutableStateOf(0) }
    val games = listOf("Free Fire", "PUBG", "COD Mobile", "BGMI", "Fortnite")

    // Performance sliders
    var cpuGov by remember { mutableStateOf(prefs.getInt("cpu_gov", 100)) }
    var gpuFreq by remember { mutableStateOf(prefs.getInt("gpu_freq", 100)) }
    var netPriority by remember { mutableStateOf(prefs.getInt("net_priority", 1)) } // 0=Normal, 1=High, 2=Real-time
    var fpsTarget by remember { mutableStateOf(prefs.getInt("fps_target", 60)) }

    // Advantage toggles
    var aimbotEnabled by remember { mutableStateOf(prefs.getBoolean("aimbot_enabled", false)) }
    var aimSmooth by remember { mutableStateOf(prefs.getInt("aim_smooth", 40)) }
    var aimFov by remember { mutableStateOf(prefs.getInt("aim_fov", 150)) }
    var aimBone by remember { mutableStateOf(prefs.getString("aim_bone", "HEAD") ?: "HEAD") }

    var locationEnabled by remember { mutableStateOf(prefs.getBoolean("location_enabled", false)) }
    var locationMode by remember { mutableStateOf(prefs.getInt("location_mode", 2)) } // 0=Fixed, 1=Random, 2=Preset
    var fixedLat by remember { mutableStateOf(prefs.getString("fixed_lat", "") ?: "") }
    var fixedLon by remember { mutableStateOf(prefs.getString("fixed_lon", "") ?: "") }
    var randomRadius by remember { mutableStateOf(prefs.getInt("random_radius", 10)) }
    var selectedPreset by remember { mutableStateOf(prefs.getString("selected_preset", "Tokyo JP") ?: "Tokyo JP") }

    var speedMultiplier by remember { mutableStateOf(prefs.getFloat("speed_mult", 1.0f)) }

    val isRooted = GameBoostApplication.isRooted()
    val shizukuActive = remember { mutableStateOf(ShizukuManager.isAvailable() && ShizukuManager.hasPermission()) }

    val scope = rememberCoroutineScope()

    val presets = listOf("Tokyo JP", "London UK", "New York US", "Dubai UAE", "Singapore", "Mumbai IN")

    fun applyAll() {
        // Save prefs
        prefs.edit()
            .putInt("cpu_gov", cpuGov)
            .putInt("gpu_freq", gpuFreq)
            .putInt("net_priority", netPriority)
            .putInt("fps_target", fpsTarget)
            .putBoolean("aimbot_enabled", aimbotEnabled)
            .putInt("aim_smooth", aimSmooth)
            .putInt("aim_fov", aimFov)
            .putString("aim_bone", aimBone)
            .putBoolean("location_enabled", locationEnabled)
            .putInt("location_mode", locationMode)
            .putString("fixed_lat", fixedLat)
            .putString("fixed_lon", fixedLon)
            .putInt("random_radius", randomRadius)
            .putString("selected_preset", selectedPreset)
            .putFloat("speed_mult", speedMultiplier)
            .apply()

        // Apply performance
        scope.launch(Dispatchers.IO) {
            ShizukuManager.setCpuGovernor(if (cpuGov > 50) "performance" else "ondemand")
            ShizukuManager.setGpuFreqPercent(gpuFreq)
            when (netPriority) {
                0 -> ShizukuManager.setNetworkPriority("Normal")
                1 -> ShizukuManager.setNetworkPriority("High")
                2 -> ShizukuManager.setNetworkPriority("Real-time")
            }
        }

        // Apply advantage
        if (aimbotEnabled) {
            AdvantageEngine.instance?.applyAimSmooth(aimSmooth / 100f)
            // AimbotEngine would be started from floating service
        }

        if (locationEnabled) {
            when (locationMode) {
                0 -> LocationSpoofer.instance?.spoofFixed(fixedLat.toDoubleOrNull() ?: 0.0, fixedLon.toDoubleOrNull() ?: 0.0)
                1 -> LocationSpoofer.instance?.spoofRandom(0.0, 0.0, randomRadius.toDouble())
                2 -> LocationSpoofer.instance?.spoofPreset(selectedPreset)
            }
        }

        AdvantageEngine.instance?.applySpeedHack(speedMultiplier)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A.toLong()))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GameBoost Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3C00.toLong())
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(
                        text = if (isRooted) "ROOTED" else "NO ROOT",
                        color = if (isRooted) Color(0xFF69FF47.toLong()) else Color(0xFFFF3C00.toLong())
                    )
                    BadgeChip(
                        text = if (shizukuActive.value) "SHIZUKU" else "NO SHIZUKU",
                        color = if (shizukuActive.value) Color(0xFF69FF47.toLong()) else Color(0xFF888888.toLong())
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = stringResource(R.string.fps_cap), value = "$fpsTarget", valueColor = Color(0xFF00E5FF.toLong()))
                StatCard(title = stringResource(R.string.cpu_perf), value = "$cpuGov%", valueColor = Color(0xFF69FF47.toLong()))
                StatCard(title = stringResource(R.string.ping_label), value = "--", valueColor = Color(0xFFBB86FC.toLong()))
            }

            // Game picker
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                items(games) { game ->
                    GameChip(
                        label = game,
                        isSelected = games[selectedGame] == game,
                        onClick = { selectedGame = games.indexOf(game) }
                    )
                }
            }

            // PERFORMANCE section
            SectionCard(title = stringResource(R.string.performance_section)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SliderRow(
                        label = stringResource(R.string.cpu_governor),
                        value = cpuGov,
                        range = 0..100,
                        thumbColor = Color(0xFFFFB74D.toLong()),
                        trackColor = Color(0xFFFFB74D.toLong()),
                        onValueChange = { cpuGov = roundToInt(it) },
                        valueLabel = { "$it%" }
                    )
                    SliderRow(
                        label = stringResource(R.string.gpu_frequency),
                        value = gpuFreq,
                        range = 0..100,
                        thumbColor = Color(0xFF00E5FF.toLong()),
                        trackColor = Color(0xFF00E5FF.toLong()),
                        onValueChange = { gpuFreq = roundToInt(it) },
                        valueLabel = { "$it%" }
                    )
                    PriorityRow(
                        label = stringResource(R.string.network_priority),
                        options = listOf("Normal", "High", "Real-time"),
                        selected = netPriority,
                        onSelect = { netPriority = it }
                    )
                    SliderRow(
                        label = stringResource(R.string.fps_target),
                        value = fpsTarget,
                        range = 30..240,
                        step = 30,
                        thumbColor = Color(0xFFFF3C00.toLong()),
                        trackColor = Color(0xFFFF3C00.toLong()),
                        onValueChange = { fpsTarget = it },
                        valueLabel = { "$it" }
                    )
                }
            }

            // ADVANTAGE LAYER section
            SectionCard(title = stringResource(R.string.advantage_section)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Aimbot
                    ToggleRow(
                        label = stringResource(R.string.aimbot),
                        subLabel = stringResource(R.string.aimbot_sublabel),
                        checked = aimbotEnabled,
                        onCheckedChange = { aimbotEnabled = it },
                        expandedContent = {
                            if (aimbotEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SliderRow(
                                        label = stringResource(R.string.smoothness),
                                        value = aimSmooth,
                                        range = 0..80,
                                        thumbColor = Color(0xFFBB86FC.toLong()),
                                        trackColor = Color(0xFFBB86FC.toLong()),
                                        onValueChange = { aimSmooth = it },
                                        valueLabel = { "$it%" }
                                    )
                                    SliderRow(
                                        label = stringResource(R.string.fov),
                                        value = aimFov,
                                        range = 50..300,
                                        thumbColor = Color(0xFF00E5FF.toLong()),
                                        trackColor = Color(0xFF00E5FF.toLong()),
                                        onValueChange = { aimFov = it },
                                        valueLabel = { "${it}px" }
                                    )
                                    BoneSelectorRow(selectedBone = aimBone, onBoneChange = { aimBone = it })
                                }
                            }
                        }
                    )

                    // Location Spoof
                    ToggleRow(
                        label = stringResource(R.string.location_spoof),
                        subLabel = if (locationEnabled) {
                            when (locationMode) {
                                0 -> "Fixed: $fixedLat, $fixedLon"
                                1 -> "Random: ${randomRadius}km"
                                else -> "Preset: $selectedPreset"
                            }
                        } else "Off",
                        checked = locationEnabled,
                        onCheckedChange = { locationEnabled = it },
                        expandedContent = {
                            if (locationEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LocationModeRow(
                                        selectedMode = locationMode,
                                        onModeChange = { locationMode = it },
                                        fixedLat = fixedLat,
                                        fixedLon = fixedLon,
                                        onFixedLatChange = { fixedLat = it },
                                        onFixedLonChange = { fixedLon = it },
                                        randomRadius = randomRadius,
                                        onRandomRadiusChange = { randomRadius = it },
                                        selectedPreset = selectedPreset,
                                        onPresetChange = { selectedPreset = it },
                                        presets = presets
                                    )
                                }
                            }
                        }
                    )

                    // Speed multiplier
                    SliderRow(
                        label = stringResource(R.string.speed_multiplier),
                        value = roundToInt(speedMultiplier * 100),
                        range = 100..500,
                        thumbColor = Color(0xFFBB86FC.toLong()),
                        trackColor = Color(0xFFBB86FC.toLong()),
                        onValueChange = { speedMultiplier = it / 100f },
                        valueLabel = { "${it / 100f}x" }
                    )
                }
            }

            // ACTIVATE BOOST button
            Button(
                onClick = { applyAll() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3C00.toLong()),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.activate_boost),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}