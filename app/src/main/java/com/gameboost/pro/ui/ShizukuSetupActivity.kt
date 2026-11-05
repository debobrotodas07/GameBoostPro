package com.gameboost.pro.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.tween
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
import androidx.compose.material3.TextField
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
import androidx.compose.ui.unit.dp
import com.gameboost.pro.R
import com.gameboost.pro.ShizukuManager
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val step1Done by remember { mutableStateOf(false) }
    val step2Done by remember { mutableStateOf(false) }
    val step3Done by remember { mutableStateOf(false) }
    val step4Done by remember { mutableStateOf(false) }
    val shizukuActive by remember { mutableStateOf(false) }
    val autoPatch by remember { mutableStateOf(false) }

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
            .background(Color("#0A0A0A"))
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
                color = Color("#FF3C00")
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
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color("#FF3C00"))) {
                    Text(stringResource(R.string.open_play_store), color = Color.White)
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
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color("#FF3C00"))) {
                    Text(stringResource(R.string.open_dev_options), color = Color.White)
                }
            }
        )

        // Step 3
        SetupStepCard(
            number = 3,
            title = stringResource(R.string.shizuku_step3_title),
            body = stringResource(R.string.shizuku_step3_body),
            isDone = step3Done,
            statusColor = if (shizukuActive) Color("#69FF47") else Color("#FF3C00"),
            actionButton = {
                Button(onClick = {
                    ShizukuManager.requestPermission { granted ->
                        if (granted) {
                            step3Done = true
                            step4Done = true
                            checkShizuku()
                        }
                    }
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color("#FF3C00")),
                    enabled = !shizukuActive) {
                    Text(stringResource(R.string.grant_permission), color = Color.White)
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
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.auto_patch_label), color = Color.White, fontSize = 14.sp)
                    androidx.compose.material3.Switch(
                        checked = autoPatch,
                        onCheckedChange = { autoPatch = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            thumbColor = Color("#FF3C00"),
                            trackColor = Color("#333333")
                        )
                    )
                }
            },
            actionButton = {
                Button(onClick = {
                    val prefs = context.getSharedPreferences("gb_prefs", 0)
                    prefs.edit().putBoolean("auto_patch", autoPatch).apply()
                    onFinish()
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color("#FF3C00")),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.continue_button), color = Color.White, fontSize = 16.sp)
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
    statusColor: Color = Color("#FF3C00"),
    codeBlock: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
    actionButton: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color("#111111"),
            contentColor = Color.White
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color("#69FF47"))
                    } else {
                        Text(
                            text = number.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = body, fontSize = 13.sp, color = Color("#BBBBBB"))
                }
            }
            codeBlock?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color("#0A0A0A"))
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = Color("#00E5FF"),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            extraContent?.invoke()
            actionButton()
        }
    }
}