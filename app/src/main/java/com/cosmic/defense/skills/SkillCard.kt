package com.cosmic.defense.skills

enum class SkillType {
    INCREASE_FIRE_RATE,
    LASER_CANNON,
    SKILL_DAMAGE_BOOST,
    BALL_DAMAGE_BOOST,
    HOMING_MISSILE,
    SKILL_HASTE
}

data class SkillCard(
    val type: SkillType,
    val star: Int,
    val title: String,
    val description: String,
    val currentValueStr: String,
    val nextValueStr: String
)
