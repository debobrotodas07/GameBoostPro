package com.gameboost.pro.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gameboost.pro.R
import com.gameboost.pro.ShizukuManager
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color constants using Long literals to avoid ambiguity with android.graphics.Color
private val ColorBackground = Color(0xFF0A0A0A.toLong())
private val ColorSurface = Color(0xFF111111.toLong())
private val ColorAccent = Color(0xFFFF3C00.toLong())
private val ColorGreen = Color(0xFF69FF47.toLong())
private val ColorCyan = Color(0xFF00E5FF.toLong())
private val ColorWhite = Color(0xFFFFFFFF.toLong())
private val ColorTextMuted = Color(0xFFBBBBBB.toLong())
private val ColorCodeBg = Color(0xFF0A0A0A.toLong())
private val ColorCodeText = Color(0xFF00E5FF.toLong())
private val ColorAccentGreen = Color(0xFF69FF47.toLong())
private val ColorAccentCyan = Color(0xFF00E5FF.toLong())
private val ColorAccentPurple = Color(0xFFBB86FC.toLong())
private val ColorTrackGray = Color(0xFF333333.toLong())
private val ColorCheckGreen = Color(0xFF69FF47.toLong())
private val ColorAccentRed = Color(0xFFFF3C00.toLong())
private val ColorWhiteText = Color(0xFFFFFFFF.toLong())
private val ColorCodeTextCyan = Color(0xFF00E5FF.toLong())
private val ColorTrackGray2 = Color(0xFF333333.toLong())

class ShizukuSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShizukuSetupScreen(onFinish = {
                val intent = Intent(this@ShizukuSetupActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            })
        }
    }
}

@Composable
fun ShizukuSetupScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var step1Done by remember { mutableStateOf(false) }
    var step2Done by remember { mutableStateOf(false) }
    var step3Done by remember { mutableStateOf(false) }
    var step4Done by remember { mutableStateOf(false) }
    val shizukuActive by remember { mutableStateOf(false) }
    var autoPatch by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun checkShizuku() {
        scope.launch(Dispatchers.IO) {
            val ping = Shizuku.pingBinder()
            val perm = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            shizukuActive = ping && perm
            step3Done = shizukuActive
            if (shizukuActive) {
                step4Done = true
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        checkShizuku()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = "GameBoost Pro",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ColorAccent
            )
        }

        // Step 1
        SetupStepCard(
            number = 1,
            title = stringResource(R.string.shizuku_step1_title),
            body = stringResource(R.string.shizuku_step1_body),
            isDone = step1Done,
            actionButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    step1Done = true
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColorAccent)) {
                    Text(stringResource(R.string.open_play_store), color = ColorWhite)
                }
            }
        )

        // Step 2
        SetupStepCard(
            number = 2,
            title = stringResource(R.string.shizuku_step2_title),
            body = stringResource(R.string.shizuku_step2_body),
            isDone = step2Done,
            codeBlock = stringResource(R.string.shizuku_step2_adb),
            actionButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    step2Done = true
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColorAccent)) {
                    Text(stringResource(R.string.open_dev_options), color = ColorWhite)
                }
            }
        )

        // Step 3
        SetupStepCard(
            number = 3,
            title = stringResource(R.string.shizuku_step3_title),
            body = stringResource(R.string.shizuku_step3_body),
            isDone = step3Done,
            statusColor = if (shizukuActive) ColorAccentGreen else ColorAccentRed,
            actionButton = {
                Button(onClick = {
                    ShizukuManager.requestPermission { granted ->
                        if (granted) {
                            step3Done = true
                            step4Done = true
                            checkShizuku()
                        }
                    }
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColorAccent),
                    enabled = !shizukuActive) {
                    Text(stringResource(R.string.grant_permission), color = ColorWhite)
                }
            }
        )

        // Step 4
        SetupStepCard(
            number = 4,
            title = stringResource(R.string.shizuku_step4_title),
            body = stringResource(R.string.shizuku_step4_body),
            isDone = step4Done,
            extraContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.auto_patch_label), color = ColorWhite, fontSize = 14.sp)
                    androidx.compose.material3.Switch(
                        checked = autoPatch,
                        onCheckedChange = { autoPatch = it }
                    )
                }
            },
            actionButton = {
                Button(onClick = {
                    val prefs = context.getSharedPreferences("gb_prefs", 0)
                    prefs.edit().putBoolean("auto_patch", autoPatch).apply()
                    onFinish()
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColorAccent),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.continue_button), color = ColorWhite, fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
fun SetupStepCard(
    number: Int,
    title: String,
    body: String,
    isDone: Boolean,
    statusColor: Color = ColorAccent,
    codeBlock: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
    actionButton: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = ColorSurface,
            contentColor = ColorWhite
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorAccentGreen)
                    } else {
                        Text(
                            text = number.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorWhite)
                    Text(text = body, fontSize = 13.sp, color = ColorTextMuted)
                }
            }
            codeBlock?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorCodeBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = ColorCodeTextCyan,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            extraContent?.invoke()
            actionButton()
        }
    }
}