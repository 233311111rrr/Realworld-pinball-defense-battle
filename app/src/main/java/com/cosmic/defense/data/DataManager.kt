package com.cosmic.defense.data

import android.content.Context
import android.content.SharedPreferences
import com.cosmic.defense.config.GameConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "cosmic_ball_defense_prefs"

        private const val KEY_COINS = "key_coins"
        private const val KEY_BEST_SCORE = "key_best_score"
        private const val KEY_BALL_DMG_LVL = "key_ball_dmg_lvl"
        private const val KEY_LUCK_LVL = "key_luck_lvl"
        private const val KEY_2X_UNLOCKED = "key_2x_unlocked"
        private const val KEY_5X_UNLOCKED = "key_5x_unlocked"
        private const val KEY_REVIVE_TICKETS = "key_revive_tickets"
        private const val KEY_LAST_LOGIN_DATE = "key_last_login_date"

        private const val KEY_TARGET_FPS = "key_target_fps"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_MUSIC_ENABLED = "key_music_enabled"
        private const val KEY_VIBRATE_ENABLED = "key_vibrate_enabled"
        private const val KEY_CONTINUOUS_SHOOT = "key_continuous_shoot"
        private const val KEY_SHOW_FPS = "key_show_fps"
    }

    init {
        checkDailyReward()
    }

    private fun checkDailyReward() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastLogin = prefs.getString(KEY_LAST_LOGIN_DATE, "")
        if (lastLogin != todayStr) {
            prefs.edit()
                .putString(KEY_LAST_LOGIN_DATE, todayStr)
                .putInt(KEY_REVIVE_TICKETS, getReviveTickets() + 1)
                .apply()
        }
    }

    fun getCoins(): Int = prefs.getInt(KEY_COINS, 300)

    fun addCoins(amount: Int) {
        val updated = (getCoins() + amount).coerceAtLeast(0)
        prefs.edit().putInt(KEY_COINS, updated).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        if (current >= amount) {
            prefs.edit().putInt(KEY_COINS, current - amount).apply()
            return true
        }
        return false
    }

    fun getBestScore(): Long = prefs.getLong(KEY_BEST_SCORE, 0L)

    fun updateBestScore(newScore: Long): Boolean {
        if (newScore > getBestScore()) {
            prefs.edit().putLong(KEY_BEST_SCORE, newScore).apply()
            return true
        }
        return false
    }

    fun getBallDamageLevel(): Int = prefs.getInt(KEY_BALL_DMG_LVL, 0)

    fun upgradeBallDamage(): Boolean {
        val currentLvl = getBallDamageLevel()
        val cost = (GameConfig.UPGRADE_BALL_DMG_BASE_COST * (1 + currentLvl * GameConfig.UPGRADE_BALL_DMG_COST_FACTOR)).toInt()
        if (spendCoins(cost)) {
            prefs.edit().putInt(KEY_BALL_DMG_LVL, currentLvl + 1).apply()
            return true
        }
        return false
    }

    fun getLuckLevel(): Int = prefs.getInt(KEY_LUCK_LVL, 0)

    fun upgradeLuck(): Boolean {
        val currentLvl = getLuckLevel()
        if (currentLvl >= GameConfig.LUCK_MAX_LEVEL) return false
        val cost = GameConfig.UPGRADE_LUCK_BASE_COST * (currentLvl + 1)
        if (spendCoins(cost)) {
            prefs.edit().putInt(KEY_LUCK_LVL, currentLvl + 1).apply()
            return true
        }
        return false
    }

    fun getCurrentLuckProbabilities(): FloatArray {
        val lvl = getLuckLevel()
        val p1 = (GameConfig.LUCK_BASE_PROBABILITIES[0] + lvl * GameConfig.LUCK_PROB_STEP_PER_LEVEL[0]).coerceIn(0.10f, 0.90f)
        val p2 = (GameConfig.LUCK_BASE_PROBABILITIES[1] + lvl * GameConfig.LUCK_PROB_STEP_PER_LEVEL[1]).coerceIn(0.05f, 0.60f)
        val p3 = (1.0f - p1 - p2).coerceAtLeast(0.01f)
        return floatArrayOf(p1, p2, p3)
    }

    fun is2xUnlocked(): Boolean = prefs.getBoolean(KEY_2X_UNLOCKED, false)

    fun unlock2x(): Boolean {
        if (is2xUnlocked()) return true
        if (spendCoins(GameConfig.UNLOCK_2X_COST)) {
            prefs.edit().putBoolean(KEY_2X_UNLOCKED, true).apply()
            return true
        }
        return false
    }

    fun is5xUnlocked(): Boolean = prefs.getBoolean(KEY_5X_UNLOCKED, false)

    fun unlock5x(): Boolean {
        if (is5xUnlocked()) return true
        if (spendCoins(GameConfig.UNLOCK_5X_COST)) {
            prefs.edit().putBoolean(KEY_5X_UNLOCKED, true).apply()
            return true
        }
        return false
    }

    fun getReviveTickets(): Int = prefs.getInt(KEY_REVIVE_TICKETS, 1)

    fun useReviveTicket(): Boolean {
        val current = getReviveTickets()
        if (current > 0) {
            prefs.edit().putInt(KEY_REVIVE_TICKETS, current - 1).apply()
            return true
        }
        return false
    }

    fun addReviveTickets(count: Int) {
        prefs.edit().putInt(KEY_REVIVE_TICKETS, getReviveTickets() + count).apply()
    }

    fun getTargetFps(): Int = prefs.getInt(KEY_TARGET_FPS, 60)
    fun setTargetFps(fps: Int) = prefs.edit().putInt(KEY_TARGET_FPS, fps).apply()

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()

    fun isMusicEnabled(): Boolean = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    fun setMusicEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()

    fun isVibrateEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
    fun setVibrateEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply()

    fun isContinuousShoot(): Boolean = prefs.getBoolean(KEY_CONTINUOUS_SHOOT, true)
    fun setContinuousShoot(enabled: Boolean) = prefs.edit().putBoolean(KEY_CONTINUOUS_SHOOT, enabled).apply()

    fun isShowFps(): Boolean = prefs.getBoolean(KEY_SHOW_FPS, true)
    fun setShowFps(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_FPS, show).apply()
}
