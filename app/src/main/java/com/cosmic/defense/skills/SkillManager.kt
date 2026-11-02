package com.cosmic.defense.skills

import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager
import kotlin.random.Random

class SkillManager(private val dataManager: DataManager) {

    var extraFireRate: Float = 0f
    var laserCannonCount: Int = 0
    var skillDamageBonusRatio: Float = 0f
    var ballDamageBonusRatio: Float = 0f
    var hasMissile: Boolean = false
    var missileLevel: Int = 0
    var skillHasteBonus: Float = 0f

    fun reset() {
        extraFireRate = 0f
        laserCannonCount = 0
        skillDamageBonusRatio = 0f
        ballDamageBonusRatio = 0f
        hasMissile = false
        missileLevel = 0
        skillHasteBonus = 0f
    }

    fun getEffectiveBallDamage(): Float {
        val baseDamage = GameConfig.BASE_BALL_DAMAGE + (dataManager.getBallDamageLevel() * 15f)
        return baseDamage * (1f + ballDamageBonusRatio)
    }

    fun getEffectiveFireRate(): Float {
        val baseRate = 1f / GameConfig.BASE_FIRE_INTERVAL_SEC
        return baseRate + extraFireRate
    }

    fun getEffectiveLaserDamage(): Float {
        return GameConfig.LASER_INITIAL_DAMAGE * (1f + skillDamageBonusRatio)
    }

    fun getEffectiveLaserCooldown(): Float {
        val baseCooldown = 1f / GameConfig.LASER_FIRE_RATE
        val hasteMultiplier = 1f + skillHasteBonus
        return baseCooldown / hasteMultiplier
    }

    fun getEffectiveMissileDamage(): Float {
        val baseDmg = GameConfig.MISSILE_INITIAL_DAMAGE + (missileLevel * 150f)
        return baseDmg * (1f + skillDamageBonusRatio)
    }

    fun getEffectiveMissileCooldown(): Float {
        val baseCooldown = GameConfig.MISSILE_COOLDOWN_SEC
        val hasteMultiplier = 1f + skillHasteBonus
        return baseCooldown / hasteMultiplier
    }

    fun rollThreeCards(): List<SkillCard> {
        val probs = dataManager.getCurrentLuckProbabilities()
        val cards = mutableListOf<SkillCard>()

        while (cards.size < 3) {
            val star = rollStar(probs)
            val card = generateRandomCardForStar(star)
            if (cards.none { it.type == card.type }) {
                cards.add(card)
            }
        }
        return cards
    }

    private fun rollStar(probs: FloatArray): Int {
        val r = Random.nextFloat()
        return when {
            r < probs[0] -> 1
            r < probs[0] + probs[1] -> 2
            else -> 3
        }
    }

    private fun generateRandomCardForStar(star: Int): SkillCard {
        val possibleTypes = mutableListOf<SkillType>()

        when (star) {
            1 -> {
                possibleTypes.add(SkillType.INCREASE_FIRE_RATE)
                possibleTypes.add(SkillType.SKILL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.BALL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.SKILL_HASTE)
            }
            2 -> {
                possibleTypes.add(SkillType.INCREASE_FIRE_RATE)
                possibleTypes.add(SkillType.SKILL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.BALL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.HOMING_MISSILE)
                possibleTypes.add(SkillType.SKILL_HASTE)
            }
            3 -> {
                if (laserCannonCount < GameConfig.LASER_MAX_SLOTS) {
                    possibleTypes.add(SkillType.LASER_CANNON)
                }
                possibleTypes.add(SkillType.INCREASE_FIRE_RATE)
                possibleTypes.add(SkillType.SKILL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.BALL_DAMAGE_BOOST)
                possibleTypes.add(SkillType.SKILL_HASTE)
            }
        }

        val type = possibleTypes.random()
        return createCardInstance(type, star)
    }

    private fun createCardInstance(type: SkillType, star: Int): SkillCard {
        return when (type) {
            SkillType.INCREASE_FIRE_RATE -> {
                val addRate = GameConfig.SKILL_FIRE_RATE_TIERS[star - 1]
                val current = getEffectiveFireRate()
                SkillCard(
                    type = type,
                    star = star,
                    title = "發球加速",
                    description = "提高主動彈球射擊速率 +${addRate} 顆/秒",
                    currentValueStr = "目前: ${String.format("%.2f", current)} 顆/秒",
                    nextValueStr = "升級後: ${String.format("%.2f", current + addRate)} 顆/秒"
                )
            }
            SkillType.LASER_CANNON -> {
                SkillCard(
                    type = type,
                    star = 3,
                    title = "高能雷射砲",
                    description = "在戰機側翼安裝貫穿直線雷射砲，貫穿整行敵軍",
                    currentValueStr = "目前槽位: ${laserCannonCount}/${GameConfig.LASER_MAX_SLOTS}",
                    nextValueStr = "升級後: ${laserCannonCount + 1}/${GameConfig.LASER_MAX_SLOTS} 座"
                )
            }
            SkillType.SKILL_DAMAGE_BOOST -> {
                val bonus = GameConfig.SKILL_DAMAGE_BONUS_TIERS[star - 1]
                val currentPct = (skillDamageBonusRatio * 100).toInt()
                val nextPct = ((skillDamageBonusRatio + bonus) * 100).toInt()
                SkillCard(
                    type = type,
                    star = star,
                    title = "技能增強",
                    description = "提升副武器（雷射與飛彈）傷害 +${(bonus * 100).toInt()}%",
                    currentValueStr = "目前加成: +${currentPct}%",
                    nextValueStr = "升級後: +${nextPct}%"
                )
            }
            SkillType.BALL_DAMAGE_BOOST -> {
                val bonus = GameConfig.BALL_DAMAGE_BONUS_TIERS[star - 1]
                val currentPct = (ballDamageBonusRatio * 100).toInt()
                val nextPct = ((ballDamageBonusRatio + bonus) * 100).toInt()
                SkillCard(
                    type = type,
                    star = star,
                    title = "球傷害增強",
                    description = "提升彈球基礎碰撞傷害 +${(bonus * 100).toInt()}%",
                    currentValueStr = "目前加成: +${currentPct}%",
                    nextValueStr = "升級後: +${nextPct}%"
                )
            }
            SkillType.HOMING_MISSILE -> {
                SkillCard(
                    type = type,
                    star = 2,
                    title = if (!hasMissile) "導引飛彈系統" else "飛彈強化",
                    description = "鎖定前排 3 隻敵人，延遲 0.7 秒後發射追蹤高爆飛彈",
                    currentValueStr = if (!hasMissile) "未裝備" else "傷害: ${getEffectiveMissileDamage().toInt()}",
                    nextValueStr = if (!hasMissile) "初始傷害: 500" else "傷害: ${(getEffectiveMissileDamage() + 150).toInt()}"
                )
            }
            SkillType.SKILL_HASTE -> {
                val hasteAdd = GameConfig.SKILL_HASTE_TIERS[star - 1]
                val currentHaste = 1f + skillHasteBonus
                SkillCard(
                    type = type,
                    star = star,
                    title = "技能加速",
                    description = "縮短副武器技能冷卻時間，加速比率 +${hasteAdd}x",
                    currentValueStr = "冷卻倍速: ${String.format("%.1f", currentHaste)}x",
                    nextValueStr = "冷卻倍速: ${String.format("%.1f", currentHaste + hasteAdd)}x"
                )
            }
        }
    }

    fun applyCard(card: SkillCard) {
        when (card.type) {
            SkillType.INCREASE_FIRE_RATE -> {
                extraFireRate += GameConfig.SKILL_FIRE_RATE_TIERS[card.star - 1]
            }
            SkillType.LASER_CANNON -> {
                if (laserCannonCount < GameConfig.LASER_MAX_SLOTS) {
                    laserCannonCount++
                }
            }
            SkillType.SKILL_DAMAGE_BOOST -> {
                skillDamageBonusRatio += GameConfig.SKILL_DAMAGE_BONUS_TIERS[card.star - 1]
            }
            SkillType.BALL_DAMAGE_BOOST -> {
                ballDamageBonusRatio += GameConfig.BALL_DAMAGE_BONUS_TIERS[card.star - 1]
            }
            SkillType.HOMING_MISSILE -> {
                if (!hasMissile) {
                    hasMissile = true
                } else {
                    missileLevel++
                }
            }
            SkillType.SKILL_HASTE -> {
                skillHasteBonus += GameConfig.SKILL_HASTE_TIERS[card.star - 1]
            }
        }
    }
}
