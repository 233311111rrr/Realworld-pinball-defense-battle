package com.cosmic.defense.world

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.cosmic.defense.audio.AudioManager
import com.cosmic.defense.config.GameConfig
import com.cosmic.defense.data.DataManager
import com.cosmic.defense.entities.BallPool
import com.cosmic.defense.entities.DamageText
import com.cosmic.defense.entities.LaserCannon
import com.cosmic.defense.entities.Missile
import com.cosmic.defense.entities.Monster
import com.cosmic.defense.entities.Particle
import com.cosmic.defense.skills.SkillManager
import java.util.ArrayDeque
import kotlin.math.sqrt

class World(
    val dataManager: DataManager,
    val audioManager: AudioManager,
    val skillManager: SkillManager
) {
    val shipX = GameConfig.LOGICAL_WIDTH / 2f
    val shipY = 2050f
    val bottomBoundaryY = shipY + 40f

    var isTouching: Boolean = false
    var touchX: Float = shipX
    var touchY: Float = shipY - 400f
    var lastAimX: Float = 0f
    var lastAimY: Float = -1f
    var shootCooldownTimer: Float = 0f

    val snakeGrid = SnakeGrid()
    val ballPool = BallPool()

    val laserCannons = Array(4) { slot ->
        val cannon = LaserCannon(slot)
        cannon.y = shipY
        cannon.x = when (slot) {
            0 -> shipX - 220f
            1 -> shipX - 110f
            2 -> shipX + 110f
            else -> shipX + 220f
        }
        cannon
    }

    val missilePool = Array(12) { Missile() }
    var missileCooldownTimer: Float = 0f
    var missileLockingTimer: Float = 0f
    var isMissileLocking: Boolean = false
    val lockedTargets = mutableListOf<Monster>()

    private val damageTextPool = Array(60) { DamageText() }
    private val particlePool = Array(150) { Particle() }

    var freezeRemainingUses = GameConfig.ITEM_INITIAL_USES
    var freezeTimer = 0f

    var bombRemainingUses = GameConfig.ITEM_INITIAL_USES

    var doubleDamageRemainingUses = GameConfig.ITEM_INITIAL_USES
    var doubleDamageTimer = 0f

    var invincibleTimer = 0f

    var currentScore: Long = 0L
    var earnedCoinsThisRun: Int = 0
    var survivedRowsCount: Int = 0
    var revivesUsedCount: Int = 0

    val pendingLotteryChests = ArrayDeque<Int>()

    var onTriggerLottery: (() -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onShowReviveDialog: (() -> Unit)? = null

    private val shipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(224, 242, 254) }
    private val aimGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 254, 240, 138)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
    }
    private val aimPath = Path()

    init {
        resetGame()
    }

    fun resetGame() {
        currentScore = 0L
        earnedCoinsThisRun = 0
        survivedRowsCount = 0
        revivesUsedCount = 0
        freezeRemainingUses = GameConfig.ITEM_INITIAL_USES
        bombRemainingUses = GameConfig.ITEM_INITIAL_USES
        doubleDamageRemainingUses = GameConfig.ITEM_INITIAL_USES
        freezeTimer = 0f
        doubleDamageTimer = 0f
        invincibleTimer = 0f

        ballPool.clear()
        snakeGrid.monsters.clear()
        snakeGrid.waveCount = 1
        snakeGrid.isCatchingUp = false
        snakeGrid.spawnRow(0)

        skillManager.reset()
        pendingLotteryChests.clear()
    }

    fun update(dt: Float) {
        if (freezeTimer > 0f) freezeTimer -= dt
        if (doubleDamageTimer > 0f) doubleDamageTimer -= dt
        if (invincibleTimer > 0f) invincibleTimer -= dt

        updateShooting(dt)
        ballPool.updateAll(dt, bottomBoundaryY)
        checkBallMonsterCollisions()

        snakeGrid.update(dt, isFrozen = (freezeTimer > 0f)) {
            if (invincibleTimer <= 0f) {
                if (revivesUsedCount < GameConfig.REVIVE_MAX_TIMES) {
                    onShowReviveDialog?.invoke()
                } else {
                    onGameOver?.invoke()
                }
            }
        }

        val laserDmgMultiplier = if (doubleDamageTimer > 0f) 2f else 1f
        val laserDmg = skillManager.getEffectiveLaserDamage() * laserDmgMultiplier
        val laserCooldown = skillManager.getEffectiveLaserCooldown()

        for (i in 0 until skillManager.laserCannonCount) {
            laserCannons[i].update(dt, laserCooldown, laserDmg, snakeGrid.monsters) { slotX, dmg ->
                audioManager.playLaser()
                spawnDamageText(slotX, 400f, dmg, isCrit = true)
            }
        }

        if (skillManager.hasMissile) {
            updateMissiles(dt)
        }

        for (txt in damageTextPool) {
            if (txt.active) txt.update(dt)
        }
        for (p in particlePool) {
            if (p.active) p.update(dt)
        }
    }

    private fun updateShooting(dt: Float) {
        shootCooldownTimer += dt
        val fireInterval = 1f / skillManager.getEffectiveFireRate()

        val shouldFire = isTouching || (GameConfig.CONTINUOUS_SHOOT_ON_RELEASE && (lastAimX != 0f || lastAimY != 0f))

        if (shouldFire && shootCooldownTimer >= fireInterval) {
            shootCooldownTimer = 0f

            val targetX = if (isTouching) touchX else (shipX + lastAimX * 200f)
            val targetY = if (isTouching) touchY else (shipY + lastAimY * 200f)

            var dirX = targetX - shipX
            var dirY = targetY - shipY
            if (dirY >= -40f) dirY = -40f

            val len = sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
            if (len > 0.001f) {
                lastAimX = dirX / len
                lastAimY = dirY / len
            }

            val dmgMultiplier = if (doubleDamageTimer > 0f) 2f else 1f
            val dmg = skillManager.getEffectiveBallDamage() * dmgMultiplier
            ballPool.obtain(shipX, shipY - 30f, dirX, dirY, dmg)
            audioManager.playShoot()
        }
    }

    private fun checkBallMonsterCollisions() {
        val activeBalls = ballPool.getActiveBalls()

        for (ball in activeBalls) {
            for (m in snakeGrid.monsters) {
                if (!m.active || m.pathProgress < 0) continue

                val dx = ball.x - m.x
                val dy = ball.y - m.y
                val distSq = dx * dx + dy * dy
                val combinedR = GameConfig.BALL_RADIUS + m.radius

                if (distSq <= combinedR * combinedR) {
                    val dist = sqrt(distSq.toDouble()).toFloat()
                    val nx = if (dist > 0.001f) dx / dist else 0f
                    val ny = if (dist > 0.001f) dy / dist else -1f

                    val isDead = m.takeDamage(ball.damage)
                    spawnDamageText(m.x, m.y, ball.damage)
                    audioManager.playHit()

                    if (isDead) {
                        onMonsterDefeated(m)
                    }

                    if (GameConfig.BOUNCE_ON_MONSTER) {
                        val dot = ball.vx * nx + ball.vy * ny
                        ball.vx = ball.vx - 2 * dot * nx
                        ball.vy = ball.vy - 2 * dot * ny
                        ball.clampAngle()
                        ball.bounceCount++
                    } else {
                        ball.active = false
                    }
                    break
                }
            }
        }
    }

    private fun onMonsterDefeated(m: Monster) {
        spawnExplosionParticles(m.x, m.y, if (m.isChest) Color.rgb(251, 191, 36) else Color.rgb(244, 63, 94))

        if (m.isChest) {
            audioManager.playChestOpen()
            currentScore += GameConfig.SCORE_CHEST_BONUS
            pendingLotteryChests.add(1)
            onTriggerLottery?.invoke()
        } else if (m.isBoss) {
            audioManager.playExplode()
            currentScore += (m.maxHp * GameConfig.SCORE_MONSTER_KILL_HP_RATIO).toLong() + GameConfig.SCORE_BOSS_BONUS
            dataManager.addReviveTickets(1)
        } else {
            audioManager.playExplode()
            currentScore += (m.maxHp * GameConfig.SCORE_MONSTER_KILL_HP_RATIO).toLong().coerceAtLeast(10L)
        }

        dataManager.updateBestScore(currentScore)
    }

    private fun updateMissiles(dt: Float) {
        missileCooldownTimer += dt
        val cooldown = skillManager.getEffectiveMissileCooldown()

        if (!isMissileLocking && missileCooldownTimer >= (cooldown - GameConfig.MISSILE_LOCK_DELAY_SEC)) {
            isMissileLocking = true
            missileLockingTimer = 0f
            lockedTargets.clear()

            val candidates = snakeGrid.monsters.filter { it.active && it.pathProgress >= 0 }
                .sortedByDescending { it.pathProgress }
                .take(GameConfig.MISSILE_LOCK_COUNT)

            for (m in candidates) {
                m.isTargetLocked = true
                lockedTargets.add(m)
            }
        }

        if (isMissileLocking) {
            missileLockingTimer += dt
            if (missileLockingTimer >= GameConfig.MISSILE_LOCK_DELAY_SEC) {
                isMissileLocking = false
                missileCooldownTimer = 0f

                val dmgMultiplier = if (doubleDamageTimer > 0f) 2f else 1f
                val dmg = skillManager.getEffectiveMissileDamage() * dmgMultiplier

                for (target in lockedTargets) {
                    target.isTargetLocked = false
                    if (target.active) {
                        for (missile in missilePool) {
                            if (!missile.active) {
                                missile.spawn(shipX, shipY - 40f, target, dmg)
                                break
                            }
                        }
                    }
                }
                lockedTargets.clear()
            }
        }

        for (missile in missilePool) {
            if (missile.active) {
                missile.update(dt) { m, dmg ->
                    audioManager.playExplode()
                    spawnDamageText(m.x, m.y, dmg, isCrit = true)
                    if (m.currentHp <= 0f) {
                        onMonsterDefeated(m)
                    }
                }
            }
        }
    }

    fun useFreezeItem(): Boolean {
        if (freezeRemainingUses <= 0) return false
        freezeRemainingUses--
        freezeTimer = GameConfig.ITEM_FREEZE_DURATION_SEC
        audioManager.vibrate(50)
        return true
    }

    fun useBombItem(): Boolean {
        if (bombRemainingUses <= 0) return false
        bombRemainingUses--
        audioManager.playExplode()

        val dmg = GameConfig.ITEM_BOMB_DAMAGE * (if (doubleDamageTimer > 0f) 2f else 1f)
        for (m in snakeGrid.monsters) {
            if (m.active && m.pathProgress >= 0) {
                val isDead = m.takeDamage(dmg)
                spawnDamageText(m.x, m.y, dmg, isCrit = true)
                if (isDead) onMonsterDefeated(m)
            }
        }
        return true
    }

    fun useDoubleDamageItem(): Boolean {
        if (doubleDamageRemainingUses <= 0) return false
        doubleDamageRemainingUses--
        doubleDamageTimer = GameConfig.ITEM_DOUBLE_DAMAGE_DURATION_SEC
        audioManager.vibrate(50)
        return true
    }

    fun performRevive(useTicket: Boolean): Boolean {
        val success = if (useTicket) {
            dataManager.useReviveTicket()
        } else {
            dataManager.spendCoins(GameConfig.REVIVE_COST_COINS)
        }

        if (success) {
            revivesUsedCount++
            invincibleTimer = GameConfig.REVIVE_INVINCIBLE_DURATION_SEC
            snakeGrid.clearNearestRowsToEnd(GameConfig.REVIVE_CLEARED_ROWS)
            audioManager.vibrate(80)
            return true
        }
        return false
    }

    private fun spawnDamageText(x: Float, y: Float, amount: Float, isCrit: Boolean = false) {
        for (txt in damageTextPool) {
            if (!txt.active) {
                txt.spawn(x, y, amount, isCrit)
                return
            }
        }
    }

    private fun spawnExplosionParticles(x: Float, y: Float, color: Int) {
        var count = 0
        for (p in particlePool) {
            if (!p.active) {
                p.spawn(x, y, color)
                count++
                if (count >= 10) break
            }
        }
    }

    fun draw(canvas: Canvas, animTime: Float) {
        if (isTouching) {
            drawAimGuide(canvas)
        }

        snakeGrid.draw(canvas, animTime)

        for (i in 0 until 4) {
            laserCannons[i].draw(canvas, equipped = (i < skillManager.laserCannonCount))
        }

        for (m in missilePool) {
            if (m.active) m.draw(canvas)
        }

        ballPool.drawAll(canvas)
        drawPlayerShip(canvas, animTime)

        for (p in particlePool) {
            if (p.active) p.draw(canvas)
        }
        for (txt in damageTextPool) {
            if (txt.active) txt.draw(canvas)
        }
    }

    private fun drawAimGuide(canvas: Canvas) {
        var dirX = touchX - shipX
        var dirY = touchY - shipY
        if (dirY >= -40f) dirY = -40f
        val len = sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
        if (len < 0.001f) return
        var nx = dirX / len
        var ny = dirY / len

        aimPath.reset()
        aimPath.moveTo(shipX, shipY - 30f)

        var curX = shipX
        var curY = shipY - 30f

        val (hitX1, hitY1, normX1, normY1) = raycastWall(curX, curY, nx, ny)
        aimPath.lineTo(hitX1, hitY1)

        if (normX1 != 0f || normY1 != 0f) {
            val dot = nx * normX1 + ny * normY1
            nx = nx - 2 * dot * normX1
            ny = ny - 2 * dot * normY1
            val (hitX2, hitY2, _, _) = raycastWall(hitX1, hitY1, nx, ny)
            aimPath.lineTo(hitX2, hitY2)
        }

        canvas.drawPath(aimPath, aimGuidePaint)
    }

    private fun raycastWall(x: Float, y: Float, dx: Float, dy: Float): FloatArray {
        var minT = Float.MAX_VALUE
        var normX = 0f
        var normY = 0f

        if (dx < -0.0001f) {
            val t = (20f - x) / dx
            if (t > 0 && t < minT) {
                minT = t
                normX = 1f
                normY = 0f
            }
        }
        if (dx > 0.0001f) {
            val t = (GameConfig.LOGICAL_WIDTH - 20f - x) / dx
            if (t > 0 && t < minT) {
                minT = t
                normX = -1f
                normY = 0f
            }
        }
        if (dy < -0.0001f) {
            val t = (220f - y) / dy
            if (t > 0 && t < minT) {
                minT = t
                normX = 0f
                normY = 1f
            }
        }

        if (minT == Float.MAX_VALUE) minT = 1000f
        return floatArrayOf(x + dx * minT, y + dy * minT, normX, normY)
    }

    private fun drawPlayerShip(canvas: Canvas, animTime: Float) {
        val shipPath = Path()
        shipPath.moveTo(shipX, shipY - 50f)
        shipPath.lineTo(shipX - 44f, shipY + 36f)
        shipPath.lineTo(shipX, shipY + 16f)
        shipPath.lineTo(shipX + 44f, shipY + 36f)
        shipPath.close()

        shipPaint.color = Color.rgb(224, 242, 254)
        canvas.drawPath(shipPath, shipPaint)

        val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248)
        }
        val flameLen = 18f + Math.sin(animTime * 18.0).toFloat() * 6f
        canvas.drawCircle(shipX, shipY + 22f + flameLen * 0.5f, 10f, flamePaint)

        flamePaint.color = Color.rgb(14, 165, 233)
        canvas.drawCircle(shipX, shipY - 8f, 12f, flamePaint)

        if (invincibleTimer > 0f) {
            val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(140, 251, 191, 36)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawCircle(shipX, shipY, 70f, shieldPaint)
        }
    }
}
