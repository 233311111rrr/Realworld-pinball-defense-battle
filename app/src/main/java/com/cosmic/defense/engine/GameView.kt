package com.cosmic.defense.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cosmic.defense.audio.AudioManager
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager
import com.cosmic.defense.skills.SkillManager
import com.cosmic.defense.world.World
import kotlin.random.Random

class GameView(
    context: Context,
    val dataManager: DataManager,
    val audioManager: AudioManager,
    val skillManager: SkillManager
) : SurfaceView(context), SurfaceHolder.Callback {

    val world = World(dataManager, audioManager, skillManager)
    var gameLoop: GameLoop? = null

    private var scaleXFactor = 1f
    private var scaleYFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val starCount = 120
    private val starX = FloatArray(starCount) { Random.nextFloat() * GameConfig.LOGICAL_WIDTH }
    private val starY = FloatArray(starCount) { Random.nextFloat() * GameConfig.LOGICAL_HEIGHT }
    private val starSpeed = FloatArray(starCount) { Random.nextFloat() * 40f + 15f }
    private val starRadius = FloatArray(starCount) { Random.nextFloat() * 2.2f + 0.8f }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 10, 14, 28)
    }
    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        isFakeBoldText = true
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(148, 163, 184)
        textSize = 28f
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempRect = RectF()

    var animTime: Float = 0f

    var onPauseClicked: (() -> Unit)? = null
    var onSpeedUnlockNeeded: ((speed: Int, cost: Int) -> Unit)? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameLoop = GameLoop(holder, this).apply {
            isRunning = true
            start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val aspectScreen = width.toFloat() / height.toFloat()
        val aspectVirtual = GameConfig.LOGICAL_WIDTH / GameConfig.LOGICAL_HEIGHT

        if (aspectScreen > aspectVirtual) {
            scaleYFactor = height.toFloat() / GameConfig.LOGICAL_HEIGHT
            scaleXFactor = scaleYFactor
            offsetX = (width - GameConfig.LOGICAL_WIDTH * scaleXFactor) / 2f
            offsetY = 0f
        } else {
            scaleXFactor = width.toFloat() / GameConfig.LOGICAL_WIDTH
            scaleYFactor = scaleXFactor
            offsetX = 0f
            offsetY = (height - GameConfig.LOGICAL_HEIGHT * scaleYFactor) / 2f
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameLoop?.isRunning = false
        while (retry) {
            try {
                gameLoop?.join()
                retry = false
            } catch (e: InterruptedException) {
            }
        }
    }

    fun fixedUpdate(dt: Float) {
        animTime += dt

        for (i in 0 until starCount) {
            starY[i] += starSpeed[i] * dt
            if (starY[i] > GameConfig.LOGICAL_HEIGHT) {
                starY[i] = 0f
                starX[i] = Random.nextFloat() * GameConfig.LOGICAL_WIDTH
            }
        }

        world.update(dt)
    }

    fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleXFactor, scaleYFactor)

        canvas.drawColor(Color.rgb(6, 8, 20))
        for (i in 0 until starCount) {
            val alpha = (140 + Math.sin((animTime * 3f + i).toDouble()).toFloat() * 90).toInt().coerceIn(50, 255)
            starPaint.alpha = alpha
            canvas.drawCircle(starX[i], starY[i], starRadius[i], starPaint)
        }

        world.draw(canvas, animTime)
        drawTopHud(canvas)
        drawBottomBar(canvas)

        canvas.restore()
    }

    private fun drawTopHud(canvas: Canvas) {
        tempRect.set(0f, 0f, GameConfig.LOGICAL_WIDTH, 200f)
        canvas.drawRect(tempRect, hudBgPaint)

        hudTextPaint.color = Color.rgb(251, 191, 36)
        canvas.drawText("金幣: ${dataManager.getCoins()}", 40f, 75f, hudTextPaint)

        hudTextPaint.color = Color.WHITE
        canvas.drawText("分數: ${world.currentScore}", 380f, 75f, hudTextPaint)

        subTextPaint.color = Color.rgb(148, 163, 184)
        canvas.drawText("最佳: ${dataManager.getBestScore()}", 700f, 75f, subTextPaint)

        buttonPaint.color = Color.rgb(30, 41, 59)
        tempRect.set(940f, 35f, 1040f, 95f)
        canvas.drawRoundRect(tempRect, 12f, 12f, buttonPaint)
        hudTextPaint.textSize = 28f
        hudTextPaint.color = Color.WHITE
        canvas.drawText("暫停", 960f, 76f, hudTextPaint)
        hudTextPaint.textSize = 34f

        val ballDmg = world.skillManager.getEffectiveBallDamage().toInt()
        val ballBonusPct = (world.skillManager.ballDamageBonusRatio * 100).toInt()
        val skillDmg = world.skillManager.getEffectiveLaserDamage().toInt()
        val skillBonusPct = (world.skillManager.skillDamageBonusRatio * 100).toInt()

        subTextPaint.color = Color.rgb(254, 240, 138)
        canvas.drawText("球 $ballDmg (+$ballBonusPct%)", 40f, 150f, subTextPaint)

        subTextPaint.color = Color.rgb(56, 189, 248)
        canvas.drawText("技能 $skillDmg (+$skillBonusPct%)", 420f, 150f, subTextPaint)

        if (dataManager.isShowFps()) {
            subTextPaint.color = Color.rgb(74, 222, 128)
            canvas.drawText("FPS: ${gameLoop?.currentFps ?: 60}", 860f, 150f, subTextPaint)
        }
    }

    private fun drawBottomBar(canvas: Canvas) {
        val barY = 2160f
        val barHeight = 160f

        tempRect.set(0f, barY, GameConfig.LOGICAL_WIDTH, barY + barHeight)
        canvas.drawRect(tempRect, hudBgPaint)

        val currentSpeed = (gameLoop?.gameSpeedMultiplier ?: 1f).toInt()
        buttonPaint.color = Color.rgb(30, 41, 59)

        drawSpeedButton(canvas, 40f, barY + 30f, "1x", currentSpeed == 1, unlocked = true)
        drawSpeedButton(canvas, 130f, barY + 30f, "2x", currentSpeed == 2, unlocked = dataManager.is2xUnlocked())
        drawSpeedButton(canvas, 220f, barY + 30f, "5x", currentSpeed == 5, unlocked = dataManager.is5xUnlocked())

        drawItemButton(canvas, 400f, barY + 20f, "❄️ 冰凍", world.freezeRemainingUses, world.freezeTimer > 0f)
        drawItemButton(canvas, 630f, barY + 20f, "💣 炸藥", world.bombRemainingUses, active = false)
        drawItemButton(canvas, 860f, barY + 20f, "⚡ 雙倍", world.doubleDamageRemainingUses, world.doubleDamageTimer > 0f)
    }

    private fun drawSpeedButton(canvas: Canvas, x: Float, y: Float, label: String, selected: Boolean, unlocked: Boolean) {
        tempRect.set(x, y, x + 76f, y + 80f)
        buttonPaint.color = if (selected) Color.rgb(56, 189, 248) else if (unlocked) Color.rgb(30, 41, 59) else Color.rgb(15, 23, 42)
        canvas.drawRoundRect(tempRect, 10f, 10f, buttonPaint)

        hudTextPaint.textSize = 28f
        hudTextPaint.color = if (selected) Color.rgb(15, 23, 42) else if (unlocked) Color.WHITE else Color.rgb(100, 116, 139)
        canvas.drawText(label, x + 18f, y + 50f, hudTextPaint)
        hudTextPaint.textSize = 34f
    }

    private fun drawItemButton(canvas: Canvas, x: Float, y: Float, name: String, uses: Int, active: Boolean) {
        tempRect.set(x, y, x + 190f, y + 100f)
        buttonPaint.color = if (active) Color.rgb(245, 158, 11) else if (uses > 0) Color.rgb(30, 41, 59) else Color.rgb(15, 23, 42)
        canvas.drawRoundRect(tempRect, 14f, 14f, buttonPaint)

        hudTextPaint.textSize = 28f
        hudTextPaint.color = if (uses > 0) Color.WHITE else Color.rgb(100, 116, 139)
        canvas.drawText(name, x + 24f, y + 46f, hudTextPaint)

        subTextPaint.color = Color.rgb(148, 163, 184)
        canvas.drawText("剩餘: $uses", x + 44f, y + 82f, subTextPaint)
        hudTextPaint.textSize = 34f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val virtX = (event.x - offsetX) / scaleXFactor
        val virtY = (event.y - offsetY) / scaleYFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (virtX in 920f..1060f && virtY in 20f..110f) {
                    onPauseClicked?.invoke()
                    return true
                }

                if (virtY in 2180f..2280f) {
                    if (virtX in 40f..120f) {
                        gameLoop?.gameSpeedMultiplier = 1.0f
                        return true
                    } else if (virtX in 130f..210f) {
                        if (dataManager.is2xUnlocked()) {
                            gameLoop?.gameSpeedMultiplier = 2.0f
                        } else {
                            onSpeedUnlockNeeded?.invoke(2, GameConfig.UNLOCK_2X_COST)
                        }
                        return true
                    } else if (virtX in 220f..300f) {
                        if (dataManager.is5xUnlocked()) {
                            gameLoop?.gameSpeedMultiplier = 5.0f
                        } else {
                            onSpeedUnlockNeeded?.invoke(5, GameConfig.UNLOCK_5X_COST)
                        }
                        return true
                    }

                    if (virtX in 400f..590f) {
                        world.useFreezeItem()
                        return true
                    } else if (virtX in 630f..820f) {
                        world.useBombItem()
                        return true
                    } else if (virtX in 860f..1050f) {
                        world.useDoubleDamageItem()
                        return true
                    }
                }

                world.isTouching = true
                world.touchX = virtX
                world.touchY = virtY
            }

            MotionEvent.ACTION_MOVE -> {
                world.touchX = virtX
                world.touchY = virtY
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                world.isTouching = false
            }
        }
        return true
    }
}
