package com.cosmic.defense.world

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.entities.Monster
import com.cosmic.defense.entities.MonsterKind
import kotlin.random.Random

/**
 * 8 排蛇形怪物路線與行軍邏輯
 * 偶數排佈局：第 1 排自右往左、第 2 排自左往右... 終點準確抵達最右下角。
 */
class SnakeGrid {

    val monsters = mutableListOf<Monster>()
    var waveCount: Int = 1
    var isCatchingUp: Boolean = false
    private var catchUpWaitingTargetRow1Mid: Boolean = false

    // 路徑幾何定義
    val startY = 320f
    val rowSpacing = 160f
    val endY = startY + (GameConfig.SNAKE_TOTAL_ROWS - 1) * rowSpacing
    val leftBound = 90f
    val rightBound = GameConfig.LOGICAL_WIDTH - 90f

    // 總路徑段長度快取
    private val rowLength = rightBound - leftBound
    private val turnLength = rowSpacing
    private val totalPathLength = (GameConfig.SNAKE_TOTAL_ROWS * rowLength) + ((GameConfig.SNAKE_TOTAL_ROWS - 1) * turnLength)

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 56, 189, 248)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }
    private val debugPath = Path()

    init {
        buildPathMesh()
    }

    private fun buildPathMesh() {
        debugPath.reset()
        var curY = startY
        for (r in 0 until GameConfig.SNAKE_TOTAL_ROWS) {
            val fromX = if (r % 2 == 0) rightBound else leftBound
            val toX = if (r % 2 == 0) leftBound else rightBound
            if (r == 0) debugPath.moveTo(fromX, curY)
            debugPath.lineTo(toX, curY)
            if (r < GameConfig.SNAKE_TOTAL_ROWS - 1) {
                curY += rowSpacing
                debugPath.lineTo(toX, curY)
            }
        }
    }

    /** 根據累積進度計算在蛇形軌跡上的 (x, y) 座標與所在排 */
    fun getCoordinatesAtDistance(dist: Float): Triple<Float, Float, Int> {
        var remaining = dist
        var curY = startY

        for (r in 0 until GameConfig.SNAKE_TOTAL_ROWS) {
            val isEven = (r % 2 == 0) // 第 1 排 (r=0) 從右向左
            val startX = if (isEven) rightBound else leftBound
            val endX = if (isEven) leftBound else rightBound

            if (remaining <= rowLength) {
                val ratio = (remaining / rowLength).coerceIn(0f, 1f)
                val x = startX + (endX - startX) * ratio
                return Triple(x, curY, r)
            }
            remaining -= rowLength

            // 行與行之間的下移轉彎
            if (r < GameConfig.SNAKE_TOTAL_ROWS - 1) {
                if (remaining <= turnLength) {
                    val ratio = (remaining / turnLength).coerceIn(0f, 1f)
                    val y = curY + turnLength * ratio
                    return Triple(endX, y, r)
                }
                remaining -= turnLength
                curY += rowSpacing
            }
        }

        // 超過終點 (落在最右下角)
        return Triple(rightBound, endY, GameConfig.SNAKE_TOTAL_ROWS - 1)
    }

    /**
     * 生成新一輪怪物隊列（預先計算總血量成長、寶箱與 Boss）
     */
    fun spawnRow(targetRowIndex: Int) {
        val baseHp = GameConfig.MONSTER_INITIAL_HP * Math.pow(1.0 + GameConfig.MONSTER_HP_ROW_GROWTH_RATE, waveCount.toDouble()).toFloat()
        val isBossWave = (waveCount % GameConfig.BOSS_ROW_INTERVAL == 0)

        val count = GameConfig.MONSTERS_PER_ROW
        val spacing = rowLength / count

        for (i in 0 until count) {
            val isBoss = isBossWave && (i == count / 2)
            val isChest = !isBoss && (Random.nextFloat() < GameConfig.CHEST_SPAWN_CHANCE)

            val hp = when {
                isBoss -> baseHp * GameConfig.BOSS_HP_MULTIPLIER
                isChest -> baseHp * GameConfig.CHEST_HP_RATIO
                else -> baseHp
            }

            val kind = when {
                isBoss -> MonsterKind.BOSS
                isChest -> MonsterKind.CHEST
                else -> {
                    val r = Random.nextInt(4)
                    when (r) {
                        0 -> MonsterKind.OCTOPUS
                        1 -> MonsterKind.CYCLOPS
                        2 -> MonsterKind.DRONE
                        else -> MonsterKind.CRUISER
                    }
                }
            }

            val monster = Monster()
            monster.init(
                kind = kind,
                startX = rightBound,
                startY = startY,
                hp = hp,
                row = 0,
                isChest = isChest,
                isBoss = isBoss
            )
            // 怪物初始分散在路徑起點之前
            monster.pathProgress = - (i * spacing)
            monsters.add(monster)
        }
        waveCount++
    }

    /**
     * 更新怪物行軍進度，並執行第一排追趕急行軍機制
     */
    fun update(dt: Float, isFrozen: Boolean, onReachedEnd: () -> Unit) {
        if (isFrozen) return

        // 檢查第一排存活怪物數量
        val row0Count = monsters.count { it.active && it.rowIndex == 0 }
        val halfCapacity = GameConfig.MONSTERS_PER_ROW / 2

        if (row0Count <= halfCapacity && !isCatchingUp && !catchUpWaitingTargetRow1Mid) {
            isCatchingUp = true
            catchUpWaitingTargetRow1Mid = true
        }

        // 若正處於急行軍，檢查是否有新怪物已經抵達第二排中間
        if (catchUpWaitingTargetRow1Mid) {
            // 第 2 排 (r=1) 中間的累積距離
            val row1MidDist = rowLength + turnLength + (rowLength * 0.5f)
            val hasReached = monsters.any { it.active && it.pathProgress >= row1MidDist }
            if (hasReached) {
                isCatchingUp = false
                catchUpWaitingTargetRow1Mid = false
            }
        }

        val speed = if (isCatchingUp) {
            GameConfig.MONSTER_BASE_SPEED * GameConfig.CATCH_UP_SPEED_MULTIPLIER
        } else {
            GameConfig.MONSTER_BASE_SPEED
        }

        for (m in monsters) {
            if (!m.active) continue

            m.pathProgress += speed * dt
            if (m.pathProgress >= 0) {
                val (newX, newY, row) = getCoordinatesAtDistance(m.pathProgress)
                m.x = newX
                m.y = newY
                m.rowIndex = row

                // 走到最右下角終點
                if (m.pathProgress >= totalPathLength) {
                    onReachedEnd()
                    return
                }
            }
        }

        // 若場上怪物少於一排的一半，補充新一排進入行軍隊伍
        val activeCount = monsters.count { it.active }
        if (activeCount <= GameConfig.MONSTERS_PER_ROW / 2) {
            spawnRow(0)
        }
    }

    /** 清除最靠近終點的指定排數怪物 (復活機制調用) */
    fun clearNearestRowsToEnd(rowCount: Int) {
        val targetMinRow = (GameConfig.SNAKE_TOTAL_ROWS - rowCount).coerceAtLeast(0)
        for (m in monsters) {
            if (m.active && m.rowIndex >= targetMinRow) {
                m.active = false
            }
        }
    }

    fun draw(canvas: Canvas, animTime: Float) {
        // 繪製蛇形預覽路徑導引虛線
        canvas.drawPath(debugPath, pathPaint)

        // 繪製所有怪物
        for (m in monsters) {
            if (m.active && m.pathProgress >= 0) {
                m.draw(canvas, animTime)
            }
        }
    }
}
