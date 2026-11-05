package com.gameboost.pro.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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

    Box(modifier = Modifier.fillMaxSize().background(Color("#0A0A0A"))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GameBoost Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color("#FF3C00")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BadgeChip(
                        text = if (isRooted) "ROOTED" else "NO ROOT",
                        color = if (isRooted) Color("#69FF47") else Color("#FF3C00")
                    )
                    BadgeChip(
                        text = if (shizukuActive.value) "SHIZUKU" else "NO SHIZUKU",
                        color = if (shizukuActive.value) Color("#69FF47") else Color("#888888")
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = stringResource(R.string.fps_cap), value = "$fpsTarget", valueColor = Color("#00E5FF"))
                StatCard(title = stringResource(R.string.cpu_perf), value = "$cpuGov%", valueColor = Color("#69FF47"))
                StatCard(title = stringResource(R.string.ping_label), value = "--", valueColor = Color("#BB86FC"))
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
                        thumbColor = Color("#FFB74D"),
                        trackColor = Color("#FFB74D"),
                        onValueChange = { cpuGov = it.roundToInt() },
                        valueLabel = { "$it%" }
                    )
                    SliderRow(
                        label = stringResource(R.string.gpu_frequency),
                        value = gpuFreq,
                        range = 0..100,
                        thumbColor = Color("#00E5FF"),
                        trackColor = Color("#00E5FF"),
                        onValueChange = { gpuFreq = it.roundToInt() },
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
                        thumbColor = Color("#FF3C00"),
                        trackColor = Color("#FF3C00"),
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
                                        thumbColor = Color("#BB86FC"),
                                        trackColor = Color("#BB86FC"),
                                        onValueChange = { aimSmooth = it },
                                        valueLabel = { "$it%" }
                                    )
                                    SliderRow(
                                        label = stringResource(R.string.fov),
                                        value = aimFov,
                                        range = 50..300,
                                        thumbColor = Color("#00E5FF"),
                                        trackColor = Color("#00E5FF"),
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
                        value = (speedMultiplier * 100).roundToInt(),
                        range = 100..500,
                        thumbColor = Color("#BB86FC"),
                        trackColor = Color("#BB86FC"),
                        onValueChange = { speedMultiplier = it / 100f },
                        valueLabel = { "${it / 100f}x" }
                    )
                }
            }

            // ACTIVATE BOOST button
            Button(
                onClick = applyAll,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color("#FF3C00"),
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

@Composable
fun BadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(color, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.33f)
            .height(72.dp),
        colors = CardDefaults.cardColors(containerColor = Color("#111111")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = Color("#888888"))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun GameChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(
                color = if (isSelected) Color("#FF3C00") else Color("#1A1A1A"),
                shape = RoundedCornerShape(20.dp)
            )
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color("#BBBBBB")
        )
    }.pointerInput(Unit) {
        detectTapGestures(onTap = onClick)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color("#111111")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color("#FF3C00"))
            content()
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    thumbColor: Color,
    trackColor: Color,
    onValueChange: (Int) -> Unit,
    valueLabel: (Int) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = Color.White)
            Text(text = valueLabel(value), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = thumbColor)
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value.toFloat(),
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            steps = ((range.endInclusive - range.start) / step).toInt(),
            onValueChange = { onValueChange(it.roundToInt()) },
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = Color("#333333")
            )
        )
    }
}

@Composable
fun PriorityRow(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 13.sp, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, opt ->
                val isSel = selected == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(36.dp)
                        .background(
                            color = if (isSel) Color("#FF3C00") else Color("#1A1A1A"),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.White else Color("#BBBBBB")
                    )
                }.pointerInput(Unit) {
                    detectTapGestures(onTap = { onSelect(index) })
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    subLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(checked) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text(text = subLabel, fontSize = 11.sp, color = Color("#888888"))
            }
            Switch(
                checked = checked,
                onCheckedChange = { newVal ->
                    onCheckedChange(newVal)
                    expanded = newVal
                },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    thumbColor = Color("#FF3C00"),
                    trackColor = Color("#333333")
                )
            )
        }
        if (expanded) {
            expandedContent()
        }
    }
}

@Composable
fun BoneSelectorRow(selectedBone: String, onBoneChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("HEAD", "CHEST").forEach { bone ->
            val isSel = selectedBone == bone
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(32.dp)
                    .background(
                        color = if (isSel) Color("#FF3C00") else Color("#333333"),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bone,
                    fontSize = 10.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White
                )
            }.pointerInput(Unit) {
                detectTapGestures(onTap = { onBoneChange(bone) })
            }
        }
    }
}

@Composable
fun LocationModeRow(
    selectedMode: Int,
    onModeChange: (Int) -> Unit,
    fixedLat: String,
    fixedLon: String,
    onFixedLatChange: (String) -> Unit,
    onFixedLonChange: (String) -> Unit,
    randomRadius: Int,
    onRandomRadiusChange: (Int) -> Unit,
    selectedPreset: String,
    onPresetChange: (String) -> Unit,
    presets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mode selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Fixed", "Random", "Preset").forEachIndexed { idx, mode ->
                val isSel = selectedMode == idx
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(32.dp)
                        .background(
                            color = if (isSel) Color("#FF3C00") else Color("#333333"),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }.pointerInput(Unit) {
                    detectTapGestures(onTap = { onModeChange(idx) })
                }
            }
        }

        // Fixed inputs
        if (selectedMode == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.TextField(
                    value = fixedLat,
                    onValueChange = onFixedLatChange,
                    label = { Text("Lat", color = Color("#888888"), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = Color("#1A1A1A"),
                        unfocusedContainerColor = Color("#1A1A1A"),
                        focusedContainerColor = Color("#1A1A1A"),
                        textColor = Color.White,
                        labelTextColor = Color("#888888"),
                        cursorColor = Color("#FF3C00"),
                        focusedIndicatorColor = Color("#FF3C00"),
                        unfocusedIndicatorColor = Color("#222222")
                    )
                )
                androidx.compose.material3.TextField(
                    value = fixedLon,
                    onValueChange = onFixedLonChange,
                    label = { Text("Lon", color = Color("#888888"), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = Color("#1A1A1A"),
                        unfocusedContainerColor = Color("#1A1A1A"),
                        focusedContainerColor = Color("#1A1A1A"),
                        textColor = Color.White,
                        labelTextColor = Color("#888888"),
                        cursorColor = Color("#FF3C00"),
                        focusedIndicatorColor = Color("#FF3C00"),
                        unfocusedIndicatorColor = Color("#222222")
                    )
                )
            }
        }

        // Random radius
        if (selectedMode == 1) {
            SliderRow(
                label = "Radius (km)",
                value = randomRadius,
                range = 1..50,
                thumbColor = Color("#00E5FF"),
                trackColor = Color("#00E5FF"),
                onValueChange = onRandomRadiusChange,
                valueLabel = { "${it}km" }
            )
        }

        // Preset dropdown
        if (selectedMode == 2) {
            androidx.compose.material3.ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                expanded = false,
                onExpandedChange = {}
            ) {
                androidx.compose.material3.TextField(
                    value = selectedPreset,
                    onValueChange = {},
                    label = { Text("Preset", color = Color("#888888"), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    readOnly = true,
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = Color("#1A1A1A"),
                        unfocusedContainerColor = Color("#1A1A1A"),
                        focusedContainerColor = Color("#1A1A1A"),
                        textColor = Color.White,
                        labelTextColor = Color("#888888"),
                        focusedIndicatorColor = Color("#FF3C00"),
                        unfocusedIndicatorColor = Color("#222222"),
                        trailingIconColor = Color("#FF3C00")
                    ),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color("#FF3C00")) }
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = false,
                    onDismissRequest = {}
                ) {
                    presets.forEach { preset ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(preset, color = Color.White) },
                            onClick = { onPresetChange(preset) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoostTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color("#FF3C00"),
            secondary = Color("#00E5FF"),
            tertiary = Color("#BB86FC"),
            background = Color("#0A0A0A"),
            surface = Color("#111111"),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        typography = androidx.compose.material3.Typography(),
        shapes = androidx.compose.material3.Shapes(),
        content = content
    )
}