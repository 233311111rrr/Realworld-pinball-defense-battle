package com.cosmic.defense.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class DamageText {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var text: String = ""
    var lifespan: Float = 0f
    val maxLifespan: Float = 0.65f
    var color: Int = Color.WHITE

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 34f
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    fun spawn(startX: Float, startY: Float, amount: Float, isCrit: Boolean = false) {
        this.active = true
        this.x = startX
        this.y = startY
        this.text = "-${amount.toInt()}"
        this.lifespan = 0f
        this.color = if (isCrit) Color.rgb(251, 191, 36) else Color.WHITE
    }

    fun update(dt: Float): Boolean {
        if (!active) return false
        lifespan += dt
        y -= 90f * dt
        if (lifespan >= maxLifespan) {
            active = false
            return false
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (!active) return
        val alpha = ((1f - lifespan / maxLifespan) * 255).toInt().coerceIn(0, 255)
        textPaint.color = color
        textPaint.alpha = alpha
        canvas.drawText(text, x, y, textPaint)
    }
}
