package com.cosmic.defense.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var dataManager: DataManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataManager = DataManager(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(6, 8, 20))
            setPadding(60, 80, 60, 60)
        }

        val tvTitle = TextView(this).apply {
            text = "⚙️ 系統與畫面設定"
            textSize = 26f
            isFakeBoldText = true
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        rootLayout.addView(tvTitle)

        val hardwareRates = mutableListOf<Float>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else @Suppress("DEPRECATION") windowManager.defaultDisplay
            display?.supportedModes?.forEach {
                if (!hardwareRates.contains(it.refreshRate)) hardwareRates.add(it.refreshRate)
            }
        }

        val tvFpsLabel = TextView(this).apply {
            text = "🎯 目標幀率選擇 (物理與渲染分離):"
            textSize = 16f
            setTextColor(Color.rgb(56, 189, 248))
            setPadding(0, 10, 0, 10)
        }
        rootLayout.addView(tvFpsLabel)

        val fpsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val currentTargetFps = dataManager.getTargetFps()

        GameConfig.SUPPORTED_FPS_OPTIONS.forEach { fps ->
            val isSupported = hardwareRates.any { Math.abs(it - fps) < 2f }
            val btn = Button(this).apply {
                val statusStr = if (isSupported) "✅ 硬體原生支援" else "⚠️ 自動降至最接近刷新率"
                text = "${fps} FPS  ($statusStr)"
                setTextColor(if (currentTargetFps == fps) Color.rgb(251, 191, 36) else Color.WHITE)
                setOnClickListener {
                    dataManager.setTargetFps(fps)
                    Toast.makeText(this@SettingsActivity, "目標幀率已設為 $fps FPS", Toast.LENGTH_SHORT).show()
                    recreate()
                }
            }
            fpsContainer.addView(btn)
        }
        rootLayout.addView(fpsContainer)

        val optionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 40, 0, 0)
        }

        val cbSound = CheckBox(this).apply {
            text = "🔊 遊戲音效"
            setTextColor(Color.WHITE)
            isChecked = dataManager.isSoundEnabled()
            setOnCheckedChangeListener { _, isChecked -> dataManager.setSoundEnabled(isChecked) }
        }

        val cbMusic = CheckBox(this).apply {
            text = "🎵 背景音樂"
            setTextColor(Color.WHITE)
            isChecked = dataManager.isMusicEnabled()
            setOnCheckedChangeListener { _, isChecked -> dataManager.setMusicEnabled(isChecked) }
        }

        val cbVibrate = CheckBox(this).apply {
            text = "📳 觸覺震動回饋"
            setTextColor(Color.WHITE)
            isChecked = dataManager.isVibrateEnabled()
            setOnCheckedChangeListener { _, isChecked -> dataManager.setVibrateEnabled(isChecked) }
        }

        val cbContinuous = CheckBox(this).apply {
            text = "🎯 放開手指後持續射擊 (預設開啟)"
            setTextColor(Color.WHITE)
            isChecked = dataManager.isContinuousShoot()
            setOnCheckedChangeListener { _, isChecked ->
                dataManager.setContinuousShoot(isChecked)
                GameConfig.CONTINUOUS_SHOOT_ON_RELEASE = isChecked
            }
        }

        val cbShowFps = CheckBox(this).apply {
            text = "📊 顯示即時 FPS 計數器"
            setTextColor(Color.WHITE)
            isChecked = dataManager.isShowFps()
            setOnCheckedChangeListener { _, isChecked -> dataManager.setShowFps(isChecked) }
        }

        optionsLayout.addView(cbSound)
        optionsLayout.addView(cbMusic)
        optionsLayout.addView(cbVibrate)
        optionsLayout.addView(cbContinuous)
        optionsLayout.addView(cbShowFps)

        rootLayout.addView(optionsLayout)
        setContentView(rootLayout)
    }
}
