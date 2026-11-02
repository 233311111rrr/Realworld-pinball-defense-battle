package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Missile {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var target: Monster? = null
    var damage: Float = 500f
    var speed: Float = 1400f

    private val missilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 63, 94)
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 251, 146, 60)
        strokeWidth = 6f
    }

    fun spawn(startX: Float, startY: Float, targetMonster: Monster, dmg: Float) {
        this.active = true
        this.x = startX
        this.y = startY
        this.target = targetMonster
        this.damage = dmg
        this.vx = 0f
        this.vy = -speed
    }

    fun update(dt: Float, onHit: (m: Monster, dmg: Float) -> Unit) {
        if (!active) return

        val tgt = target
        if (tgt == null || !tgt.active) {
            y += vy * dt
            if (y < 200f) active = false
            return
        }

        val dx = tgt.x - x
        val dy = tgt.y - y
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist <= tgt.radius + 18f) {
            tgt.takeDamage(damage)
            onHit(tgt, damage)
            active = false
            return
        }

        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val steerRate = 8.5f * dt
        vx += (cos(angle.toDouble()).toFloat() * speed - vx) * steerRate
        vy += (sin(angle.toDouble()).toFloat() * speed - vy) * steerRate

        x += vx * dt
        y += vy * dt
    }

    fun draw(canvas: Canvas) {
        if (!active) return
        canvas.drawLine(x, y, x - vx * 0.03f, y - vy * 0.03f, trailPaint)
        canvas.drawCircle(x, y, 9f, missilePaint)
    }
}
