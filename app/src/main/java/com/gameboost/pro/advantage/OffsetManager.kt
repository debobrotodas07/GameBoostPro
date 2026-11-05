package com.gameboost.pro.advantage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.lang.reflect.Type

data class OffsetData(
    val target_lib: String,
    val target_pkg_1: String,
    val target_pkg_2: String,
    val offsets: Map<String, String>,
    val write_values: Map<String, String>
)

object OffsetManager {

    private var cachedOffsets: OffsetData? = null

    fun loadOffsets(context: Context): OffsetData {
        if (cachedOffsets != null) return cachedOffsets!!

        val gson = Gson()
        val type: Type = object : TypeToken<OffsetData>() {}.type

        try {
            val inputStream = context.assets.open("offsets.json")
            val reader = InputStreamReader(inputStream)
            cachedOffsets = gson.fromJson(reader, type)
            reader.close()
            return cachedOffsets!!
        } catch (e: Exception) {
            e.printStackTrace()
            return OffsetData(
                target_lib = "libil2cpp.so",
                target_pkg_1 = "com.dts.freefireth",
                target_pkg_2 = "com.dts.freefiremax",
                offsets = mapOf(
                    "player_list" to "0x067E4B20",
                    "local_player" to "0x067E4A80",
                    "entity_stride" to "0x38",
                    "pos_x" to "0x90",
                    "pos_y" to "0x94",
                    "pos_z" to "0x98",
                    "current_hp" to "0x1A8",
                    "max_hp" to "0x1AC",
                    "is_alive" to "0x1B0",
                    "team_id" to "0x1C4",
                    "bone_head" to "0x320",
                    "bone_chest" to "0x33C",
                    "view_matrix" to "0x06A1F2C0",
                    "recoil_x" to "0x03B2A140",
                    "recoil_y" to "0x03B2A144"
                ),
                write_values = mapOf(
                    "recoil_zero" to "0x00000000",
                    "nop_arm64" to "0x1F2003D5"
                )
            )
        }
    }

    fun getOffset(name: String, context: Context): Long {
        val offsets = loadOffsets(context).offsets
        val hex = offsets[name] ?: throw IllegalArgumentException("Offset not found: $name")
        return java.lang.Long.parseLong(hex.replace("0x", ""), 16)
    }

    fun getWriteValue(name: String, context: Context): String {
        val values = loadOffsets(context).write_values
        return values[name] ?: throw IllegalArgumentException("Write value not found: $name")
    }

    fun getTargetLib(context: Context): String = loadOffsets(context).target_lib

    fun getTargetPackages(context: Context): Pair<String, String> {
        val data = loadOffsets(context)
        return data.target_pkg_1 to data.target_pkg_2
    }
}