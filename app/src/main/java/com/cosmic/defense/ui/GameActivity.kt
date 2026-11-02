package com.cosmic.defense.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cosmic.defense.audio.AudioManager
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager
import com.cosmic.defense.engine.GameView
import com.cosmic.defense.skills.SkillCard
import com.cosmic.defense.skills.SkillManager

class GameActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager
    private lateinit var audioManager: AudioManager
    private lateinit var skillManager: SkillManager
    private lateinit var gameView: GameView

    private var activeLotteryCards: List<SkillCard> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        dataManager = DataManager(this)
        audioManager = AudioManager(this, dataManager)
        skillManager = SkillManager(dataManager)

        applyTargetDisplayFps(dataManager.getTargetFps())

        gameView = GameView(this, dataManager, audioManager, skillManager)
        setContentView(gameView)

        setupGameCallbacks()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    private fun applyTargetDisplayFps(fps: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }

                val supportedModes = display?.supportedModes ?: return
                var bestMode: Display.Mode? = null
                var minDiff = Float.MAX_VALUE

                for (mode in supportedModes) {
                    val diff = Math.abs(mode.refreshRate - fps)
                    if (diff < minDiff) {
                        minDiff = diff
                        bestMode = mode
                    }
                }

                bestMode?.let {
                    val params = window.attributes
                    params.preferredDisplayModeId = it.modeId
                    window.attributes = params
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupGameCallbacks() {
        gameView.onPauseClicked = {
            runOnUiThread { showPauseDialog() }
        }

        gameView.onSpeedUnlockNeeded = { speed, cost ->
            runOnUiThread { showSpeedUnlockDialog(speed, cost) }
        }

        gameView.world.onTriggerLottery = {
            runOnUiThread { checkAndShowNextLottery() }
        }

        gameView.world.onShowReviveDialog = {
            runOnUiThread { showReviveDialog() }
        }

        gameView.world.onGameOver = {
            runOnUiThread { showGameOverDialog() }
        }
    }

    private fun checkAndShowNextLottery() {
        if (gameView.world.pendingLotteryChests.isNotEmpty()) {
            gameView.gameLoop?.isPaused = true
            gameView.world.pendingLotteryChests.poll()
            showLotteryDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showLotteryDialog() {
        activeLotteryCards = skillManager.rollThreeCards()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.rgb(15, 23, 42))
        }

        val titleView = TextView(this).apply {
            text = "✨ 獲得晶化星空寶箱！請選擇技能"
            textSize = 20f
            setTextColor(android.graphics.Color.rgb(251, 191, 36))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(titleView)

        val cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        var selectedIndex = 0
        val cardViews = mutableListOf<LinearLayout>()

        activeLotteryCards.forEachIndexed { index, card ->
            val cardBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(10, 0, 10, 0)
                }
                setPadding(20, 24, 20, 24)
                setBackgroundColor(if (index == 0) android.graphics.Color.rgb(30, 41, 59) else android.graphics.Color.rgb(20, 28, 48))

                val starView = TextView(context).apply {
                    text = "★".repeat(card.star)
                    textSize = 18f
                    setTextColor(if (card.star == 3) android.graphics.Color.rgb(251, 191, 36) else android.graphics.Color.rgb(56, 189, 248))
                }
                val cardTitle = TextView(context).apply {
                    text = card.title
                    textSize = 15f
                    setTextColor(android.graphics.Color.WHITE)
                }
                val cardDesc = TextView(context).apply {
                    text = card.description
                    textSize = 11f
                    setTextColor(android.graphics.Color.rgb(148, 163, 184))
                    setPadding(0, 10, 0, 10)
                }
                val cardCur = TextView(context).apply {
                    text = card.currentValueStr
                    textSize = 11f
                    setTextColor(android.graphics.Color.rgb(203, 213, 225))
                }
                val cardNext = TextView(context).apply {
                    text = card.nextValueStr
                    textSize = 11f
                    setTextColor(android.graphics.Color.rgb(74, 222, 128))
                }

                addView(starView)
                addView(cardTitle)
                addView(cardDesc)
                addView(cardCur)
                addView(cardNext)

                setOnClickListener {
                    selectedIndex = index
                    cardViews.forEachIndexed { i, v ->
                        v.setBackgroundColor(if (i == index) android.graphics.Color.rgb(45, 60, 90) else android.graphics.Color.rgb(20, 28, 48))
                    }
                }
            }
            cardViews.add(cardBox)
            cardsContainer.addView(cardBox)
        }
        layout.addView(cardsContainer)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 30, 0, 0)
        }

        val btnConfirm = Button(this).apply {
            text = "確定領取"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnSelectAll = Button(this).apply {
            text = "全選 (200金)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isEnabled = dataManager.getCoins() >= GameConfig.LOTTERY_ALL_COST
        }
        val btnReroll = Button(this).apply {
            text = "重選 (50金)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isEnabled = dataManager.getCoins() >= GameConfig.LOTTERY_REROLL_COST
        }

        buttonBar.addView(btnConfirm)
        buttonBar.addView(btnSelectAll)
        buttonBar.addView(btnReroll)
        layout.addView(buttonBar)

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(false)
            .create()

        btnConfirm.setOnClickListener {
            skillManager.applyCard(activeLotteryCards[selectedIndex])
            dialog.dismiss()
            resumeOrQueueNextLottery()
        }

        btnSelectAll.setOnClickListener {
            if (dataManager.spendCoins(GameConfig.LOTTERY_ALL_COST)) {
                activeLotteryCards.forEach { skillManager.applyCard(it) }
                dialog.dismiss()
                resumeOrQueueNextLottery()
            }
        }

        btnReroll.setOnClickListener {
            if (dataManager.spendCoins(GameConfig.LOTTERY_REROLL_COST)) {
                dialog.dismiss()
                showLotteryDialog()
            }
        }

        dialog.show()
    }

    private fun resumeOrQueueNextLottery() {
        if (gameView.world.pendingLotteryChests.isNotEmpty()) {
            checkAndShowNextLottery()
        } else {
            gameView.gameLoop?.isPaused = false
            hideSystemUI()
        }
    }

    private fun showReviveDialog() {
        gameView.gameLoop?.isPaused = true
        val remainingRevives = GameConfig.REVIVE_MAX_TIMES - gameView.world.revivesUsedCount
        val coins = dataManager.getCoins()
        val tickets = dataManager.getReviveTickets()

        val canUseTicket = tickets > 0

        AlertDialog.Builder(this)
            .setTitle("⚠️ 防線告急！是否復活？")
            .setMessage("怪物已抵達終點！可消除最靠近終點的 2 排怪物並獲得 3 秒無敵。\n" +
                    "剩餘復活次數: $remainingRevives 次\n" +
                    "目前持有: $coins 金幣 / $tickets 張復活券")
            .setPositiveButton(if (canUseTicket) "使用復活券 (剩 $tickets 張)" else "花費 300 金幣") { _, _ ->
                val success = gameView.world.performRevive(useTicket = canUseTicket)
                if (success) {
                    gameView.gameLoop?.isPaused = false
                    hideSystemUI()
                } else {
                    Toast.makeText(this, "金幣或復活券不足！", Toast.LENGTH_SHORT).show()
                    showGameOverDialog()
                }
            }
            .setNegativeButton("放棄防守 (進入結算)") { _, _ ->
                showGameOverDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGameOverDialog() {
        gameView.gameLoop?.isPaused = true
        val finalScore = gameView.world.currentScore
        val isNewRecord = dataManager.updateBestScore(finalScore)
        val bestScore = dataManager.getBestScore()

        val earnedCoins = (finalScore / GameConfig.COIN_SCORE_DIVISOR).toInt() +
                (gameView.world.survivedRowsCount * GameConfig.COIN_ROW_SURVIVAL_BONUS)
        dataManager.addCoins(earnedCoins)

        val msg = """
            本局得分: $finalScore ${if (isNewRecord) "🎉 新紀錄！" else ""}
            歷史最佳: $bestScore
            存活排數: ${gameView.world.survivedRowsCount} 排
            獲得金幣: +$earnedCoins 金幣
            目前金幣總數: ${dataManager.getCoins()}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🏆 戰役結算")
            .setMessage(msg)
            .setPositiveButton("再來一局") { _, _ ->
                gameView.world.resetGame()
                gameView.gameLoop?.isPaused = false
                hideSystemUI()
            }
            .setNegativeButton("回主選單") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPauseDialog() {
        gameView.gameLoop?.isPaused = true
        AlertDialog.Builder(this)
            .setTitle("遊戲暫停")
            .setMessage("防線運作中，隨時整裝待發。")
            .setPositiveButton("繼續遊戲") { _, _ ->
                gameView.gameLoop?.isPaused = false
                hideSystemUI()
            }
            .setNeutralButton("音效/音樂設定") { _, _ ->
                toggleAudioSettings()
            }
            .setNegativeButton("退出至主選單") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun toggleAudioSettings() {
        val sound = !dataManager.isSoundEnabled()
        dataManager.setSoundEnabled(sound)
        Toast.makeText(this, "音效已切換為: ${if (sound) "開啟" else "關閉"}", Toast.LENGTH_SHORT).show()
        showPauseDialog()
    }

    private fun showSpeedUnlockDialog(speed: Int, cost: Int) {
        val currentCoins = dataManager.getCoins()
        AlertDialog.Builder(this)
            .setTitle("永久解鎖 ${speed} 倍速")
            .setMessage("需要消耗 $cost 金幣永久解鎖 ${speed}x 戰鬥加速。\n目前擁有金幣: $currentCoins")
            .setPositiveButton("立即解鎖") { _, _ ->
                val ok = if (speed == 2) dataManager.unlock2x() else dataManager.unlock5x()
                if (ok) {
                    Toast.makeText(this, "已成功解鎖 ${speed}x 倍速！", Toast.LENGTH_SHORT).show()
                    gameView.gameLoop?.gameSpeedMultiplier = speed.toFloat()
                } else {
                    Toast.makeText(this, "金幣不足！", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        showPauseDialog()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
