package com.gameboost.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.gameboost.pro.GameBoostApplication
import com.gameboost.pro.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameLaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pkg = intent.data?.schemeSpecificPart

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = GameBoostApplication.getPrefs()
            if (prefs.getBoolean("start_on_boot", true)) {
                // Start floating service or main activity
                val startIntent = Intent(context, com.gameboost.pro.ui.MainActivity::class.java)
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(startIntent)
            }
            return
        }

        if (pkg != "com.dts.freefireth" && pkg != "com.dts.freefiremax") return

        val prefs = GameBoostApplication.getPrefs()
        if (!prefs.getBoolean("auto_patch", false)) return

        CoroutineScope(Dispatchers.IO).launch {
            // Backup original recoil bytes once
            if (prefs.getString("backup_recoil", "").isEmpty()) {
                val lib = ShizukuManager.readGameFile(pkg, "files/../lib/arm64/libil2cpp.so")
                lib?.let {
                    val orig = it.copyOfRange(0x03B2A140, 0x03B2A144)
                    val hex = orig.joinToString("") { b -> "%02X".format(b) }
                    prefs.edit().putString("backup_recoil", hex).apply()
                }
            }

            // Patch recoil to zero
            ShizukuManager.patchLibOnDisk(pkg, 0x03B2A140L, byteArrayOf(0, 0, 0, 0))
            ShizukuManager.patchLibOnDisk(pkg, 0x03B2A144L, byteArrayOf(0, 0, 0, 0))

            // Grant mock location silently
            ShizukuManager.grantMockLocation(context.packageName)

            // Apply performance profile
            ShizukuManager.applyPerfProfile()
        }
    }
}