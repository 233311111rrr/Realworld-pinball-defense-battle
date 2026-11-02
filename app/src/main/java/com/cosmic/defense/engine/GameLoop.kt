package com.cosmic.defense.engine

import android.graphics.Canvas
import android.view.SurfaceHolder
import com.cosmic.defense.config.GameConfig

class GameLoop(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread("GameLoopThread") {

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var isPaused: Boolean = false

    @Volatile
    var gameSpeedMultiplier: Float = 1.0f

    var currentFps: Int = 60
    private var frameCount: Int = 0
    private var lastFpsTimestamp: Long = 0L

    override fun run() {
        var lastTime = System.nanoTime()
        val fixedDeltaTimeNs = (GameConfig.FIXED_DELTA_TIME * 1_000_000_000L).toLong()
        var accumulator = 0L

        lastFpsTimestamp = System.currentTimeMillis()

        while (isRunning) {
            val now = System.nanoTime()
            var elapsed = now - lastTime
            lastTime = now

            if (elapsed > 250_000_000L) {
                elapsed = 250_000_000L
            }

            if (!isPaused) {
                accumulator += (elapsed * gameSpeedMultiplier).toLong()

                while (accumulator >= fixedDeltaTimeNs) {
                    gameView.fixedUpdate(GameConfig.FIXED_DELTA_TIME)
                    accumulator -= fixedDeltaTimeNs
                }
            }

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.render(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            frameCount++
            val curTimeMs = System.currentTimeMillis()
            if (curTimeMs - lastFpsTimestamp >= 1000L) {
                currentFps = frameCount
                frameCount = 0
                lastFpsTimestamp = curTimeMs
            }
        }
    }
}
