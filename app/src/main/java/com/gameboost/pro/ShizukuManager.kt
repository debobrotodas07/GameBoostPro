package com.gameboost.pro

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

object ShizukuManager {

    private const val TAG = "ShizukuManager"

    fun isAvailable(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean =
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission(cb: (Boolean) -> Unit) {
        Shizuku.addRequestPermissionResultListener { _, r ->
            cb(r == PackageManager.PERMISSION_GRANTED)
        }
        Shizuku.requestPermission(1001)
    }

    fun exec(cmd: String): String {
        if (!hasPermission()) return "NO_PERM"
        return withContext(Dispatchers.IO) {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                "ERROR: ${e.message}"
            }
        }
    }

    fun patchLibOnDisk(pkg: String, offset: Long, bytes: ByteArray): Boolean {
        val libPath = "files/../lib/arm64/libil2cpp.so"
        val tmp = "/data/local/tmp/gbpatch_${System.currentTimeMillis()}"
        val copyCmd = "run-as $pkg cp $libPath $tmp"
        val copyResult = exec(copyCmd)
        if (!copyResult.contains("OK") && !copyResult.isBlank()) return false

        val writeResult = withContext(Dispatchers.IO) {
            try {
                val file = File(tmp)
                if (!file.exists()) return@withContext false
                val raf = RandomAccessFile(file, "rw")
                raf.seek(offset)
                raf.write(bytes)
                raf.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        if (!writeResult) return false

        val writeBackCmd = "run-as $pkg cp $tmp $libPath && echo OK"
        val writeBackResult = exec(writeBackCmd)
        exec("rm -f $tmp")
        return writeBackResult.contains("OK")
    }

    fun readGameFile(pkg: String, path: String): ByteArray? {
        val tmp = "/data/local/tmp/gbread_${System.currentTimeMillis()}"
        val copyResult = exec("run-as $pkg cp $path $tmp && echo OK")
        if (!copyResult.contains("OK")) return null

        return withContext(Dispatchers.IO) {
            try {
                val file = File(tmp)
                if (!file.exists()) return@withContext null
                val bytes = file.readBytes()
                exec("rm -f $tmp")
                bytes
            } catch (e: Exception) {
                e.printStackTrace()
                exec("rm -f $tmp")
                null
            }
        }
    }

    fun grantMockLocation(ourPkg: String) {
        exec("appops set $ourPkg android:mock_location allow")
    }

    fun applyPerfProfile() {
        exec("for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$f 2>/dev/null; done")
        exec("setprop vendor.thermal.config game")
        exec("cmd thermalservice override-status 0")
        exec("cmd activity set-standby-bucket com.dts.freefireth active")
        exec("cmd activity set-standby-bucket com.dts.freefiremax active")
    }

    fun setCpuGovernor(governor: String) {
        exec("for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $governor > \$f 2>/dev/null; done")
    }

    fun setGpuFreqPercent(percent: Int) {
        exec("echo $percent > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq_percent 2>/dev/null")
    }

    fun setNetworkPriority(priority: String) {
        when (priority) {
            "High" -> exec("tc qdisc add dev wlan0 root handle 1: prio bands 3 priomap 1 2 2 2 1 2 0 0 1 1 1 1 1 1 1 1 2>/dev/null; tc class add dev wlan0 parent 1: classid 1:1 htb rate 100mbit ceil 100mbit 2>/dev/null")
            "Real-time" -> exec("tc qdisc add dev wlan0 root handle 1: prio bands 3 priomap 1 2 2 2 1 2 0 0 1 1 1 1 1 1 1 1 2>/dev/null; tc class add dev wlan0 parent 1: classid 1:1 htb rate 1000mbit ceil 1000mbit 2>/dev/null")
            else -> exec("tc qdisc del dev wlan0 root 2>/dev/null")
        }
    }
}