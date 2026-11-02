package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.cosmic.defense.config.GameConfig
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Ball {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var damage: Float = 100f
    var lifespan: Float = 0f
    var bounceCount: Int = 0
    var spawnTimestamp: Long = 0L

    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(254, 240, 138)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 56, 189, 248)
    }

    fun spawn(startX: Float, startY: Float, dirX: Float, dirY: Float, dmg: Float) {
        val len = sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
        val nx = if (len > 0.0001f) dirX / len else 0f
        val ny = if (len > 0.0001f) dirY / len else -1f

        this.x = startX
        this.y = startY
        this.vx = nx * GameConfig.BALL_SPEED
        this.vy = ny * GameConfig.BALL_SPEED
        this.damage = dmg
        this.lifespan = 0f
        this.bounceCount = 0
        this.spawnTimestamp = System.currentTimeMillis()
        this.active = true

        clampAngle()
    }

    fun clampAngle() {
        var angle = atan2(vy.toDouble(), vx.toDouble()).toFloat()
        val minRad = Math.toRadians(GameConfig.BALL_MIN_HORIZONTAL_ANGLE_DEG.toDouble()).toFloat()

        if (Math.abs(angle) < minRad) {
            angle = if (angle >= 0) minRad else -minRad
        }
        val pi = Math.PI.toFloat()
        if (Math.abs(angle - pi) < minRad) {
            angle = pi - minRad
        } else if (Math.abs(angle + pi) < minRad) {
            angle = -pi + minRad
        }

        val speed = GameConfig.BALL_SPEED
        this.vx = cos(angle.toDouble()).toFloat() * speed
        this.vy = sin(angle.toDouble()).toFloat() * speed
    }

    fun update(dt: Float, bottomLimitY: Float): Boolean {
        if (!active) return false

        lifespan += dt
        if (lifespan >= GameConfig.BALL_MAX_LIFESPAN_SEC || bounceCount >= GameConfig.BALL_MAX_BOUNCES) {
            active = false
            return false
        }

        x += vx * dt
        y += vy * dt

        if (x - GameConfig.BALL_RADIUS <= 0f) {
            x = GameConfig.BALL_RADIUS
            vx = -vx
            bounceCount++
            clampAngle()
        } else if (x + GameConfig.BALL_RADIUS >= GameConfig.LOGICAL_WIDTH) {
            x = GameConfig.LOGICAL_WIDTH - GameConfig.BALL_RADIUS
            vx = -vx
            bounceCount++
            clampAngle()
        }

        if (y - GameConfig.BALL_RADIUS <= 220f) {
            y = 220f + GameConfig.BALL_RADIUS
            vy = -vy
            bounceCount++
            clampAngle()
        }

        if (y + GameConfig.BALL_RADIUS >= bottomLimitY) {
            active = false
            return false
        }

        return true
    }

    fun draw(canvas: Canvas) {
        if (!active) return
        canvas.drawCircle(x, y, GameConfig.BALL_RADIUS * 1.5f, glowPaint)
        canvas.drawCircle(x, y, GameConfig.BALL_RADIUS, ballPaint)
    }
}
