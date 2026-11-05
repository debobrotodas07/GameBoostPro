package com.gameboost.pro.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameboost.pro.GameBoostApplication
import com.gameboost.pro.R
import com.gameboost.pro.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBoostTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = GameBoostApplication.getPrefs()

    var startOnBoot by remember { mutableStateOf(prefs.getBoolean("start_on_boot", true)) }
    var autoDetectGames by remember { mutableStateOf(prefs.getBoolean("auto_detect_games", true)) }
    var showFloatingSidebar by remember { mutableStateOf(prefs.getBoolean("show_floating_sidebar", true)) }
    var autoPatchFF by remember { mutableStateOf(prefs.getBoolean("auto_patch", false)) }
    var rootModeOverride by remember { mutableStateOf(prefs.getBoolean("root_mode_override", false)) }
    var defaultSpeed by remember { mutableStateOf(prefs.getFloat("default_speed", 1.0f)) }
    var defaultAimSmooth by remember { mutableStateOf(prefs.getInt("default_aim_smooth", 40)) }

    val isRooted = GameBoostApplication.isRooted()
    val shizukuAvailable = ShizukuManager.isAvailable()
    val shizukuHasPerm = ShizukuManager.hasPermission()

    Column(
        modifier = Modifier.fillMaxSize().background(Color("#0A0A0A")).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = { (context as ComponentActivity).finish() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(text = stringResource(R.string.settings_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color("#FF3C00"))
            androidx.compose.foundation.layout.Spacer(Modifier.width(48.dp))
        }

        // General section
        SectionCard(title = "General") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingToggle(
                    label = stringResource(R.string.start_on_boot),
                    checked = startOnBoot,
                    onCheckedChange = { v ->
                        startOnBoot = v
                        prefs.edit().putBoolean("start_on_boot", v).apply()
                    }
                )
                SettingToggle(
                    label = stringResource(R.string.auto_detect_games),
                    checked = autoDetectGames,
                    onCheckedChange = { v ->
                        autoDetectGames = v
                        prefs.edit().putBoolean("auto_detect_games", v).apply()
                    }
                )
                SettingToggle(
                    label = stringResource(R.string.show_floating_sidebar),
                    checked = showFloatingSidebar,
                    onCheckedChange = { v ->
                        showFloatingSidebar = v
                        prefs.edit().putBoolean("show_floating_sidebar", v).apply()
                    }
                )
                SettingToggle(
                    label = stringResource(R.string.auto_patch_ff),
                    checked = autoPatchFF,
                    onCheckedChange = { v ->
                        autoPatchFF = v
                        prefs.edit().putBoolean("auto_patch", v).apply()
                    }
                )
                SettingToggle(
                    label = stringResource(R.string.root_mode),
                    checked = rootModeOverride,
                    onCheckedChange = { v ->
                        rootModeOverride = v
                        prefs.edit().putBoolean("root_mode_override", v).apply()
                    }
                )
            }
        }

        // Defaults section
        SectionCard(title = "Defaults") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DefaultSlider(
                    label = stringResource(R.string.default_speed),
                    value = (defaultSpeed * 100).roundToInt(),
                    range = 100..300,
                    onValueChange = { v ->
                        defaultSpeed = v / 100f
                        prefs.edit().putFloat("default_speed", defaultSpeed).apply()
                    },
                    valueLabel = { "${it / 100f}x" }
                )
                DefaultSlider(
                    label = stringResource(R.string.default_aim_smooth),
                    value = defaultAimSmooth,
                    range = 0..80,
                    onValueChange = { v ->
                        defaultAimSmooth = v
                        prefs.edit().putInt("default_aim_smooth", v).apply()
                    },
                    valueLabel = { "$it%" }
                )
            }
        }

        // Actions section
        SectionCard(title = "Actions") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    label = stringResource(R.string.revoke_patches),
                    onClick = {
                        // Revoke patches logic
                    }
                )
                ActionButton(
                    label = stringResource(R.string.request_overlay_perm),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            }
                        }
                    }
                )
                ActionButton(
                    label = stringResource(R.string.request_usage_perm),
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Info section
        SectionCard(title = "Info") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = stringResource(R.string.version), value = "1.0 (build 1)")
                InfoRow(label = stringResource(R.string.root_status), value = if (isRooted) "Yes" else "No", valueColor = if (isRooted) Color("#69FF47") else Color("#FF3C00"))
                InfoRow(label = stringResource(R.string.shizuku_status), value = if (shizukuAvailable && shizukuHasPerm) "Active" else "Inactive", valueColor = if (shizukuAvailable && shizukuHasPerm) Color("#69FF47") else Color("#FF3C00"))
                InfoRow(label = stringResource(R.string.ndk_version), value = "r26+")
            }
        }
    }
}