package com.cosmic.defense.config

/**
 * 太空彈球防衛戰 - 全局遊戲數值與配置集中管理
 * 所有參數均標註詳細用途、預設值與物理單位，方便策劃調整與平衡驗證。
 */
object GameConfig {

    // 1. 畫面與邏輯世界設定
    const val LOGICAL_WIDTH = 1080f
    const val LOGICAL_HEIGHT = 2340f
    const val PHYSICS_HZ = 240
    const val FIXED_DELTA_TIME = 1f / PHYSICS_HZ

    // 2. 玩家與主動彈球
    const val BASE_BALL_DAMAGE = 100f
    const val BASE_FIRE_INTERVAL_SEC = 0.8f
    const val BALL_SPEED = 1850f
    const val BALL_RADIUS = 16f
    const val BALL_MAX_LIFESPAN_SEC = 8f
    const val BALL_MAX_BOUNCES = 30
    const val BALL_MIN_HORIZONTAL_ANGLE_DEG = 8f
    const val BALL_MAX_COUNT = 150
    var BOUNCE_ON_MONSTER = true
    var CONTINUOUS_SHOOT_ON_RELEASE = true

    // 3. 怪物蛇形行軍與生成
    const val SNAKE_TOTAL_ROWS = 8
    const val MONSTERS_PER_ROW = 8
    const val MONSTER_WIDTH = 108f
    const val MONSTER_HEIGHT = 108f
    const val MONSTER_INITIAL_HP = 100f
    const val MONSTER_HP_ROW_GROWTH_RATE = 0.08f
    const val MONSTER_BASE_SPEED = 90f
    const val CHEST_SPAWN_CHANCE = 0.06f
    const val CHEST_HP_RATIO = 0.60f
    const val BOSS_ROW_INTERVAL = 10
    const val BOSS_HP_MULTIPLIER = 8.0f
    const val CATCH_UP_THRESHOLD_RATIO = 0.5f
    const val CATCH_UP_SPEED_MULTIPLIER = 2.4f

    // 4. 副武器與技能卡數值
    val SKILL_FIRE_RATE_TIERS = floatArrayOf(0.5f, 1.0f, 1.7f)
    const val LASER_INITIAL_DAMAGE = 250f
    const val LASER_FIRE_RATE = 0.5f
    const val LASER_MAX_SLOTS = 4
    val SKILL_DAMAGE_BONUS_TIERS = floatArrayOf(0.50f, 1.00f, 2.00f)
    val BALL_DAMAGE_BONUS_TIERS = floatArrayOf(0.50f, 1.00f, 2.00f)
    const val MISSILE_INITIAL_DAMAGE = 500f
    const val MISSILE_LOCK_COUNT = 3
    const val MISSILE_LOCK_DELAY_SEC = 0.7f
    const val MISSILE_COOLDOWN_SEC = 2.0f
    val SKILL_HASTE_TIERS = floatArrayOf(0.5f, 1.0f, 1.4f)

    // 5. 抽卡與永久養成
    const val LOTTERY_ALL_COST = 200
    const val LOTTERY_REROLL_COST = 50
    val LUCK_BASE_PROBABILITIES = floatArrayOf(0.70f, 0.25f, 0.05f)
    val LUCK_PROB_STEP_PER_LEVEL = floatArrayOf(-0.04f, 0.025f, 0.015f)
    const val LUCK_MAX_LEVEL = 10
    const val UPGRADE_BALL_DMG_BASE_COST = 100
    const val UPGRADE_BALL_DMG_COST_FACTOR = 0.45f
    const val UPGRADE_LUCK_BASE_COST = 200

    // 6. 倍速解鎖與幀率
    const val UNLOCK_2X_COST = 1000
    const val UNLOCK_5X_COST = 3000
    val SUPPORTED_FPS_OPTIONS = intArrayOf(30, 60, 90, 120, 165, 185)

    // 7. 結算、復活與戰術道具
    const val SCORE_MONSTER_KILL_HP_RATIO = 0.1f
    const val SCORE_CHEST_BONUS = 50
    const val SCORE_ROW_SURVIVAL_BONUS = 20
    const val SCORE_BOSS_BONUS = 500
    const val COIN_SCORE_DIVISOR = 10
    const val COIN_ROW_SURVIVAL_BONUS = 1
    const val REVIVE_COST_COINS = 300
    const val REVIVE_MAX_TIMES = 2
    const val REVIVE_CLEARED_ROWS = 2
    const val REVIVE_INVINCIBLE_DURATION_SEC = 3.0f
    const val ITEM_FREEZE_DURATION_SEC = 3.0f
    const val ITEM_BOMB_DAMAGE = 1800f
    const val ITEM_DOUBLE_DAMAGE_DURATION_SEC = 10.0f
    const val ITEM_INITIAL_USES = 3
}
