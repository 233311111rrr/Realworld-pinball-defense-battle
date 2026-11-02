package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.random.Random

class Particle {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var size: Float = 6f
    var lifespan: Float = 0f
    val maxLifespan: Float = 0.5f
    var color: Int = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun spawn(startX: Float, startY: Float, particleColor: Int) {
        this.active = true
        this.x = startX
        this.y = startY
        this.color = particleColor
        this.lifespan = 0f
        this.size = Random.nextFloat() * 6f + 4f

        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val speed = Random.nextFloat() * 320f + 80f
        this.vx = Math.cos(angle.toDouble()).toFloat() * speed
        this.vy = Math.sin(angle.toDouble()).toFloat() * speed
    }

    fun update(dt: Float): Boolean {
        if (!active) return false
        lifespan += dt
        x += vx * dt
        y += vy * dt
        if (lifespan >= maxLifespan) {
            active = false
            return false
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (!active) return
        val alpha = ((1f - lifespan / maxLifespan) * 255).toInt().coerceIn(0, 255)
        paint.color = color
        paint.alpha = alpha
        canvas.drawCircle(x, y, size, paint)
    }
}
