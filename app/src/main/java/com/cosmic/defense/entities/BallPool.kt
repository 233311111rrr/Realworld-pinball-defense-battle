package com.cosmic.defense.entities

import android.graphics.Canvas
import com.cosmic.defense.config.GameConfig

class BallPool {
    private val pool = Array(GameConfig.BALL_MAX_COUNT) { Ball() }

    fun obtain(startX: Float, startY: Float, dirX: Float, dirY: Float, damage: Float): Ball {
        for (ball in pool) {
            if (!ball.active) {
                ball.spawn(startX, startY, dirX, dirY, damage)
                return ball
            }
        }

        var oldestBall = pool[0]
        for (i in 1 until pool.size) {
            if (pool[i].spawnTimestamp < oldestBall.spawnTimestamp) {
                oldestBall = pool[i]
            }
        }

        oldestBall.spawn(startX, startY, dirX, dirY, damage)
        return oldestBall
    }

    fun updateAll(dt: Float, bottomLimitY: Float) {
        for (ball in pool) {
            if (ball.active) {
                ball.update(dt, bottomLimitY)
            }
        }
    }

    fun drawAll(canvas: Canvas) {
        for (ball in pool) {
            if (ball.active) {
                ball.draw(canvas)
            }
        }
    }

    fun getActiveBalls(): List<Ball> {
        val list = mutableListOf<Ball>()
        for (ball in pool) {
            if (ball.active) list.add(ball)
        }
        return list
    }

    fun getActiveCount(): Int {
        var count = 0
        for (ball in pool) {
            if (ball.active) count++
        }
        return count
    }

    fun clear() {
        for (ball in pool) {
            ball.active = false
        }
    }
}
