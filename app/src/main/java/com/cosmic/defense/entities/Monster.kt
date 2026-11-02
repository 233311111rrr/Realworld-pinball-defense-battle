package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.cosmic.defense.config.GameConfig

enum class MonsterKind {
    OCTOPUS,
    CYCLOPS,
    DRONE,
    CRUISER,
    CHEST,
    BOSS
}

class Monster {
    var active: Boolean = false
    var kind: MonsterKind = MonsterKind.OCTOPUS
    var x: Float = 0f
    var y: Float = 0f
    var radius: Float = GameConfig.MONSTER_WIDTH / 2f
    var currentHp: Float = 100f
    var maxHp: Float = 100f
    var rowIndex: Int = 0
    var isChest: Boolean = false
    var isBoss: Boolean = false
    var pathProgress: Float = 0f

    var isTargetLocked: Boolean = false
    var lockTimer: Float = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
        color = Color.WHITE
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }
    private val hpBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 20, 24, 39)
    }
    private val hpBarFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 63, 94)
    }
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 63, 94)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val tempRect = RectF()
    private val tempPath = Path()

    fun init(kind: MonsterKind, startX: Float, startY: Float, hp: Float, row: Int, isChest: Boolean, isBoss: Boolean) {
        this.active = true
        this.kind = kind
        this.x = startX
        this.y = startY
        this.radius = if (isBoss) (GameConfig.MONSTER_WIDTH * 0.9f) else (GameConfig.MONSTER_WIDTH * 0.45f)
        this.currentHp = hp
        this.maxHp = hp
        this.rowIndex = row
        this.isChest = isChest
        this.isBoss = isBoss
        this.pathProgress = 0f
        this.isTargetLocked = false
        this.lockTimer = 0f
    }

    fun takeDamage(amount: Float): Boolean {
        currentHp -= amount
        if (currentHp <= 0f) {
            currentHp = 0f
            active = false
            return true
        }
        return false
    }

    fun formatHp(hp: Float): String {
        return when {
            hp >= 1_000_000f -> String.format("%.1fM", hp / 1_000_000f)
            hp >= 1_000f -> String.format("%.1fK", hp / 1_000f)
            else -> hp.toInt().toString()
        }
    }

    fun draw(canvas: Canvas, animTime: Float) {
        if (!active) return

        when (kind) {
            MonsterKind.CHEST -> drawChest(canvas, animTime)
            MonsterKind.BOSS -> drawBoss(canvas, animTime)
            MonsterKind.OCTOPUS -> drawOctopus(canvas, animTime)
            MonsterKind.CYCLOPS -> drawCyclops(canvas, animTime)
            MonsterKind.DRONE -> drawDrone(canvas, animTime)
            MonsterKind.CRUISER -> drawCruiser(canvas, animTime)
        }

        val hpBarWidth = radius * 2.2f
        val hpBarHeight = 12f
        val hpY = y - radius - 24f

        tempRect.set(x - hpBarWidth / 2, hpY, x + hpBarWidth / 2, hpY + hpBarHeight)
        canvas.drawRoundRect(tempRect, 6f, 6f, hpBarBgPaint)

        val healthRatio = (currentHp / maxHp).coerceIn(0f, 1f)
        tempRect.set(x - hpBarWidth / 2, hpY, x - hpBarWidth / 2 + (hpBarWidth * healthRatio), hpY + hpBarHeight)
        hpBarFgPaint.color = if (isChest) Color.rgb(251, 191, 36) else if (isBoss) Color.rgb(220, 38, 38) else Color.rgb(56, 189, 248)
        canvas.drawRoundRect(tempRect, 6f, 6f, hpBarFgPaint)

        textPaint.color = if (isChest) Color.rgb(254, 240, 138) else Color.WHITE
        canvas.drawText(formatHp(currentHp), x, hpY - 8f, textPaint)

        if (isTargetLocked) {
            drawLockReticle(canvas, animTime)
        }
    }

    private fun drawLockReticle(canvas: Canvas, animTime: Float) {
        val lockR = radius * 1.3f
        canvas.drawCircle(x, y, lockR, lockPaint)
        canvas.drawLine(x - lockR - 8f, y, x - lockR + 8f, y, lockPaint)
        canvas.drawLine(x + lockR - 8f, y, x + lockR + 8f, y, lockPaint)
        canvas.drawLine(x, y - lockR - 8f, x, y - lockR + 8f, lockPaint)
        canvas.drawLine(x, y + lockR - 8f, x, y + lockR + 8f, lockPaint)
    }

    private fun drawChest(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(245, 158, 11)
        tempRect.set(x - radius, y - radius * 0.7f, x + radius, y + radius * 0.7f)
        canvas.drawRoundRect(tempRect, 14f, 14f, bodyPaint)

        detailPaint.color = Color.rgb(254, 243, 199)
        canvas.drawCircle(x, y, radius * 0.28f, detailPaint)
        detailPaint.color = Color.rgb(217, 119, 6)
        canvas.drawRect(x - radius, y - 4f, x + radius, y + 4f, detailPaint)
    }

    private fun drawBoss(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(220, 38, 38)
        canvas.drawCircle(x, y, radius, bodyPaint)

        detailPaint.color = Color.rgb(245, 158, 11)
        canvas.drawCircle(x, y, radius * 0.55f, detailPaint)

        detailPaint.color = Color.rgb(254, 240, 138)
        canvas.drawCircle(x, y, radius * 0.25f, detailPaint)

        tempPath.reset()
        tempPath.moveTo(x - radius * 0.8f, y - radius * 0.5f)
        tempPath.lineTo(x, y - radius * 1.35f)
        tempPath.lineTo(x + radius * 0.8f, y - radius * 0.5f)
        tempPath.close()
        canvas.drawPath(tempPath, detailPaint)
    }

    private fun drawOctopus(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(225, 29, 72)
        canvas.drawCircle(x, y - 6f, radius * 0.85f, bodyPaint)

        detailPaint.color = Color.rgb(254, 240, 138)
        canvas.drawCircle(x - radius * 0.35f, y - radius * 0.2f, radius * 0.22f, detailPaint)
        canvas.drawCircle(x + radius * 0.35f, y - radius * 0.2f, radius * 0.22f, detailPaint)
        detailPaint.color = Color.rgb(15, 23, 42)
        canvas.drawCircle(x - radius * 0.35f, y - radius * 0.2f, radius * 0.1f, detailPaint)
        canvas.drawCircle(x + radius * 0.35f, y - radius * 0.2f, radius * 0.1f, detailPaint)

        detailPaint.color = Color.rgb(190, 18, 60)
        detailPaint.strokeWidth = 6f
        detailPaint.style = Paint.Style.STROKE
        for (i in -2..2) {
            if (i == 0) continue
            val tx = x + (i * radius * 0.35f)
            val ty = y + radius * 0.7f + (Math.sin((animTime * 6f + i).toDouble()).toFloat() * 8f)
            canvas.drawLine(x + (i * radius * 0.2f), y + radius * 0.4f, tx, ty, detailPaint)
        }
        detailPaint.style = Paint.Style.FILL
    }

    private fun drawCyclops(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(16, 185, 129)
        canvas.drawCircle(x, y, radius, bodyPaint)

        detailPaint.color = Color.WHITE
        canvas.drawCircle(x, y, radius * 0.5f, detailPaint)
        detailPaint.color = Color.rgb(5, 150, 105)
        canvas.drawCircle(x, y, radius * 0.3f, detailPaint)
        detailPaint.color = Color.rgb(15, 23, 42)
        canvas.drawCircle(x, y, radius * 0.15f, detailPaint)
    }

    private fun drawDrone(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(2, 132, 199)
        tempRect.set(x - radius * 0.8f, y - radius * 0.8f, x + radius * 0.8f, y + radius * 0.8f)
        canvas.drawRoundRect(tempRect, 10f, 10f, bodyPaint)

        detailPaint.color = Color.rgb(56, 189, 248)
        canvas.drawCircle(x, y, radius * 0.35f, detailPaint)
    }

    private fun drawCruiser(canvas: Canvas, animTime: Float) {
        bodyPaint.color = Color.rgb(217, 119, 6)
        tempPath.reset()
        tempPath.moveTo(x, y + radius)
        tempPath.lineTo(x - radius, y - radius * 0.7f)
        tempPath.lineTo(x, y - radius * 0.2f)
        tempPath.lineTo(x + radius, y - radius * 0.7f)
        tempPath.close()
        canvas.drawPath(tempPath, bodyPaint)

        detailPaint.color = Color.rgb(251, 191, 36)
        canvas.drawCircle(x, y, radius * 0.25f, detailPaint)
    }
}
