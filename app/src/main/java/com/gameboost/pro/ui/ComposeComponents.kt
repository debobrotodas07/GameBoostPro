package com.gameboost.pro.ui

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
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
fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                thumbColor = Color("#FF3C00"),
                trackColor = Color("#333333")
            )
        )
    }
}

@Composable
fun DefaultSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    valueLabel: (Int) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = Color.White)
            Text(text = valueLabel(value), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color("#FF3C00"))
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value.toFloat(),
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color("#FF3C00"),
                activeTrackColor = Color("#FF3C00"),
                inactiveTrackColor = Color("#333333")
            )
        )
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color("#1A1A1A"),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color("#BBBBBB")) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color("#888888"))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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