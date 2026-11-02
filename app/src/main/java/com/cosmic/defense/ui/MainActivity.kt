package com.cosmic.defense.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager

class MainActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager

    private lateinit var tvCoins: TextView
    private lateinit var tvBestScore: TextView
    private lateinit var tvTickets: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataManager = DataManager(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(6, 8, 20))
            setPadding(60, 100, 60, 60)
        }

        val tvTitle = TextView(this).apply {
            text = "太空彈球防衛戰"
            textSize = 32f
            isFakeBoldText = true
            setTextColor(Color.rgb(248, 250, 252))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        val tvSub = TextView(this).apply {
            text = "COSMIC BALL DEFENSE"
            textSize = 14f
            setTextColor(Color.rgb(56, 189, 248))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 8, 0, 80)
        }
        rootLayout.addView(tvTitle)
        rootLayout.addView(tvSub)

        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 23, 42))
            setPadding(40, 30, 40, 30)
        }

        tvCoins = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.rgb(251, 191, 36))
        }
        tvBestScore = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 10)
        }
        tvTickets = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(74, 222, 128))
        }

        statsCard.addView(tvCoins)
        statsCard.addView(tvBestScore)
        statsCard.addView(tvTickets)
        rootLayout.addView(statsCard)

        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 80, 0, 0)
        }

        val btnStart = Button(this).apply {
            text = "🚀 開始防禦戰鬥"
            textSize = 18f
            setOnClickListener {
                startActivity(Intent(this@MainActivity, GameActivity::class.java))
            }
        }

        val btnUpgrade = Button(this).apply {
            text = "⭐ 永久戰力養成"
            textSize = 18f
            setOnClickListener {
                showUpgradeDialog()
            }
        }

        val btnSettings = Button(this).apply {
            text = "⚙️ 系統與畫面設定"
            textSize = 18f
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        val btnAbout = Button(this).apply {
            text = "ℹ️ 關於與 CC0 授權"
            textSize = 18f
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            }
        }

        buttonsLayout.addView(btnStart)
        buttonsLayout.addView(btnUpgrade)
        buttonsLayout.addView(btnSettings)
        buttonsLayout.addView(btnAbout)

        rootLayout.addView(buttonsLayout)
        setContentView(rootLayout)

        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshStats() {
        tvCoins.text = "💰 目前持有金幣: ${dataManager.getCoins()}"
        tvBestScore.text = "🏆 歷史最佳成績: ${dataManager.getBestScore()}"
        tvTickets.text = "🎫 剩餘復活券: ${dataManager.getReviveTickets()} 張 (每日登入贈送 1 張)"
    }

    @SuppressLint("SetTextI18n")
    private fun showUpgradeDialog() {
        val currentDmgLvl = dataManager.getBallDamageLevel()
        val dmgCost = (GameConfig.UPGRADE_BALL_DMG_BASE_COST * (1 + currentDmgLvl * GameConfig.UPGRADE_BALL_DMG_COST_FACTOR)).toInt()

        val currentLuckLvl = dataManager.getLuckLevel()
        val luckCost = GameConfig.UPGRADE_LUCK_BASE_COST * (currentLuckLvl + 1)
        val luckProbs = dataManager.getCurrentLuckProbabilities()

        val message = """
            【1. 基礎球傷害等級: Lv.$currentDmgLvl】
            - 目前基礎傷害: ${(GameConfig.BASE_BALL_DAMAGE + currentDmgLvl * 15f).toInt()}
            - 升級費用: $dmgCost 金幣 (+15 基礎傷害)

            【2. 幸運機率等級: Lv.$currentLuckLvl / ${GameConfig.LUCK_MAX_LEVEL}】
            - 當前星級機率:
              ★☆☆ (1星): ${String.format("%.1f", luckProbs[0] * 100)}%
              ★★☆ (2星): ${String.format("%.1f", luckProbs[1] * 100)}%
              ★★★ (3星): ${String.format("%.1f", luckProbs[2] * 100)}%
            - 升級費用: ${if (currentLuckLvl < GameConfig.LUCK_MAX_LEVEL) "$luckCost 金幣" else "已達上限"}
            - 每級效果: 1星 -4%, 2星 +2.5%, 3星 +1.5%
        """.trimIndent()

        val builder = AlertDialog.Builder(this)
            .setTitle("戰艦永久強化中心")
            .setMessage(message)
            .setPositiveButton("升級球傷害 ($dmgCost 金幣)") { _, _ ->
                if (dataManager.upgradeBallDamage()) {
                    Toast.makeText(this, "球傷害提升成功！", Toast.LENGTH_SHORT).show()
                    refreshStats()
                    showUpgradeDialog()
                } else {
                    Toast.makeText(this, "金幣不足！", Toast.LENGTH_SHORT).show()
                }
            }

        if (currentLuckLvl < GameConfig.LUCK_MAX_LEVEL) {
            builder.setNeutralButton("升級幸運 ($luckCost 金幣)") { _, _ ->
                if (dataManager.upgradeLuck()) {
                    Toast.makeText(this, "幸運等級提升成功！高星卡機率增加！", Toast.LENGTH_SHORT).show()
                    refreshStats()
                    showUpgradeDialog()
                } else {
                    Toast.makeText(this, "金幣不足！", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("關閉", null).show()
    }
}
