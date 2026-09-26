package com.zolarm.app.data

enum class ChallengeType { STEPS, CAMERA }

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val challenge: ChallengeType,
    val stepTarget: Int = 10,
    val enabled: Boolean = true
) {
    val timeText: String
        get() = String.format("%02d:%02d", hour, minute)
}
