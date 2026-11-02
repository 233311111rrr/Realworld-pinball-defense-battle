package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class LaserCannon(val slotIndex: Int) {
    var x: Float = 0f
    var y: Float = 0f
    var cooldownTimer: Float = 0f
    var firingTimer: Float = 0f

    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(56, 189, 248)
        strokeWidth = 14f
        style = Paint.Style.STROKE
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val cannonBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 41, 59)
    }
    private val slotEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 56, 189, 248)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun update(
        dt: Float,
        cooldown: Float,
        damage: Float,
        monsters: List<Monster>,
        onFire: (slotX: Float, dmg: Float) -> Unit
    ) {
        if (firingTimer > 0f) {
            firingTimer -= dt
        }

        cooldownTimer += dt
        if (cooldownTimer >= cooldown) {
            cooldownTimer = 0f
            firingTimer = 0.22f

            val beamHalfWidth = 24f
            for (m in monsters) {
                if (m.active && Math.abs(m.x - x) <= (m.radius + beamHalfWidth)) {
                    m.takeDamage(damage)
                }
            }
            onFire(x, damage)
        }
    }

    fun draw(canvas: Canvas, equipped: Boolean) {
        if (!equipped) {
            canvas.drawCircle(x, y, 22f, slotEmptyPaint)
            return
        }

        cannonBasePaint.color = Color.rgb(15, 23, 42)
        canvas.drawCircle(x, y, 24f, cannonBasePaint)
        cannonBasePaint.color = Color.rgb(56, 189, 248)
        canvas.drawCircle(x, y, 14f, cannonBasePaint)

        if (firingTimer > 0f) {
            val alpha = (firingTimer / 0.22f).coerceIn(0f, 1f)
            beamPaint.alpha = (alpha * 255).toInt()
            corePaint.alpha = (alpha * 255).toInt()

            canvas.drawLine(x, y, x, 220f, beamPaint)
            canvas.drawLine(x, y, x, 220f, corePaint)
        }
    }
}
