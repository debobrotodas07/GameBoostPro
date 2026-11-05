package com.gameboost.pro

data class Vector3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
)

data class Vector2(
    var x: Float = 0f,
    var y: Float = 0f
)

data class EntityData(
    var position: Vector3 = Vector3(),
    var boneHead: Vector3 = Vector3(),
    var boneChest: Vector3 = Vector3(),
    var currentHp: Int = 0,
    var maxHp: Int = 0,
    var isAlive: Boolean = false,
    var isTeammate: Boolean = false,
    var teamId: Int = 0,
    var screenPos: Vector2? = null
)