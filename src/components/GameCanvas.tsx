import React, { useRef, useEffect, useState, useCallback } from 'react';
import { GameConfig } from '../GameConfig';
import { webAudio } from '../audio/WebAudio';
import { MonsterEntity, BallEntity, LaserCannonEntity, MissileEntity, DamageTextEntity, ParticleEntity, MonsterKind, SkillCard, SkillType } from '../types';

interface GameCanvasProps {
  coins: number;
  bestScore: number;
  ballDamageLevel: number;
  luckLevel: number;
  is2xUnlocked: boolean;
  is5xUnlocked: boolean;
  reviveTickets: number;
  targetFps: number;
  soundEnabled: boolean;
  continuousShoot: boolean;
  showFps: boolean;
  onCoinsChange: (newCoins: number) => void;
  onBestScoreChange: (newBest: number) => void;
  onReviveTicketsChange: (newTickets: number) => void;
  onRequestUnlockSpeed: (speed: number, cost: number) => void;
  onOpenPause: () => void;
  onOpenUpgrades: () => void;
}

export const GameCanvas: React.FC<GameCanvasProps> = ({
  coins,
  bestScore,
  ballDamageLevel,
  luckLevel,
  is2xUnlocked,
  is5xUnlocked,
  reviveTickets,
  soundEnabled,
  continuousShoot,
  showFps,
  onCoinsChange,
  onBestScoreChange,
  onReviveTicketsChange,
  onRequestUnlockSpeed,
  onOpenPause,
  onOpenUpgrades,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const [currentScore, setCurrentScore] = useState<number>(0);
  const [speedMultiplier, setSpeedMultiplier] = useState<number>(1);
  const [freezeUses, setFreezeUses] = useState<number>(GameConfig.ITEM_INITIAL_USES);
  const [bombUses, setBombUses] = useState<number>(GameConfig.ITEM_INITIAL_USES);
  const [doubleDmgUses, setDoubleDmgUses] = useState<number>(GameConfig.ITEM_INITIAL_USES);
  const [freezeTimer, setFreezeTimer] = useState<number>(0);
  const [doubleDmgTimer, setDoubleDmgTimer] = useState<number>(0);
  const [fpsDisplay, setFpsDisplay] = useState<number>(60);

  const [lotteryCards, setLotteryCards] = useState<SkillCard[] | null>(null);
  const [showReviveModal, setShowReviveModal] = useState<boolean>(false);
  const [showGameOverModal, setShowGameOverModal] = useState<boolean>(false);
  const [earnedCoinsFinal, setEarnedCoinsFinal] = useState<number>(0);
  const [survivedRowsCount, setSurvivedRowsCount] = useState<number>(0);
  const [revivesUsedCount, setRevivesUsedCount] = useState<number>(0);

  const skillsRef = useRef({
    extraFireRate: 0,
    laserCount: 0,
    skillDmgBonus: 0,
    ballDmgBonus: 0,
    hasMissile: false,
    missileLevel: 0,
    skillHaste: 0,
  });

  const entitiesRef = useRef({
    monsters: [] as MonsterEntity[],
    balls: [] as BallEntity[],
    laserCannons: [
      { slotIndex: 0, x: 540 - 220, y: 2050, cooldownTimer: 0, firingTimer: 0 },
      { slotIndex: 1, x: 540 - 110, y: 2050, cooldownTimer: 0, firingTimer: 0 },
      { slotIndex: 2, x: 540 + 110, y: 2050, cooldownTimer: 0, firingTimer: 0 },
      { slotIndex: 3, x: 540 + 220, y: 2050, cooldownTimer: 0, firingTimer: 0 },
    ] as LaserCannonEntity[],
    missiles: [] as MissileEntity[],
    damageTexts: [] as DamageTextEntity[],
    particles: [] as ParticleEntity[],
    stars: Array.from({ length: 120 }, () => ({
      x: Math.random() * GameConfig.LOGICAL_WIDTH,
      y: Math.random() * GameConfig.LOGICAL_HEIGHT,
      speed: Math.random() * 45 + 15,
      radius: Math.random() * 2.2 + 0.8,
    })),
    waveCount: 1,
    isCatchingUp: false,
    catchUpWaitingRow1Mid: false,
    pendingChests: 0,
    isPaused: false,
    isTouching: false,
    touchX: 540,
    touchY: 1600,
    lastAimX: 0,
    lastAimY: -1,
    shootCooldownTimer: 0,
    invincibleTimer: 0,
    missileCooldownTimer: 0,
    missileLockingTimer: 0,
    isMissileLocking: false,
    lockedTargetIds: [] as number[],
    nextEntityId: 1,
  });

  useEffect(() => {
    webAudio.setMuted(!soundEnabled);
  }, [soundEnabled]);

  const formatHp = (hp: number): string => {
    if (hp >= 1_000_000) return (hp / 1_000_000).toFixed(1) + 'M';
    if (hp >= 1_000) return (hp / 1_000).toFixed(1) + 'K';
    return Math.floor(hp).toString();
  };

  const spawnMonsterRow = useCallback(() => {
    const e = entitiesRef.current;
    const baseHp = GameConfig.MONSTER_INITIAL_HP * Math.pow(1 + GameConfig.MONSTER_HP_ROW_GROWTH_RATE, e.waveCount);
    const isBossWave = e.waveCount % GameConfig.BOSS_ROW_INTERVAL === 0;

    const startY = 320;
    const leftBound = 90;
    const rightBound = GameConfig.LOGICAL_WIDTH - 90;
    const rowLen = rightBound - leftBound;
    const spacing = rowLen / GameConfig.MONSTERS_PER_ROW;

    for (let i = 0; i < GameConfig.MONSTERS_PER_ROW; i++) {
      const isBoss = isBossWave && i === Math.floor(GameConfig.MONSTERS_PER_ROW / 2);
      const isChest = !isBoss && Math.random() < GameConfig.CHEST_SPAWN_CHANCE;

      const hp = isBoss ? baseHp * GameConfig.BOSS_HP_MULTIPLIER : isChest ? baseHp * GameConfig.CHEST_HP_RATIO : baseHp;

      let kind: MonsterKind = 'OCTOPUS';
      if (isBoss) kind = 'BOSS';
      else if (isChest) kind = 'CHEST';
      else {
        const kinds: MonsterKind[] = ['OCTOPUS', 'CYCLOPS', 'DRONE', 'CRUISER'];
        kind = kinds[Math.floor(Math.random() * kinds.length)];
      }

      const m: MonsterEntity = {
        id: e.nextEntityId++,
        active: true,
        kind,
        x: rightBound,
        y: startY,
        radius: isBoss ? GameConfig.MONSTER_WIDTH * 0.9 : GameConfig.MONSTER_WIDTH * 0.45,
        currentHp: hp,
        maxHp: hp,
        rowIndex: 0,
        isChest,
        isBoss,
        pathProgress: -(i * spacing),
        isTargetLocked: false,
      };
      e.monsters.push(m);
    }
    e.waveCount++;
    setSurvivedRowsCount(e.waveCount - 1);
  }, []);

  const resetGame = useCallback(() => {
    const e = entitiesRef.current;
    e.monsters = [];
    e.balls = [];
    e.missiles = [];
    e.damageTexts = [];
    e.particles = [];
    e.waveCount = 1;
    e.isCatchingUp = false;
    e.catchUpWaitingRow1Mid = false;
    e.pendingChests = 0;
    e.isPaused = false;
    e.shootCooldownTimer = 0;
    e.invincibleTimer = 0;
    e.missileCooldownTimer = 0;
    e.missileLockingTimer = 0;
    e.isMissileLocking = false;
    e.lockedTargetIds = [];

    skillsRef.current = {
      extraFireRate: 0,
      laserCount: 0,
      skillDmgBonus: 0,
      ballDmgBonus: 0,
      hasMissile: false,
      missileLevel: 0,
      skillHaste: 0,
    };

    setCurrentScore(0);
    setFreezeUses(GameConfig.ITEM_INITIAL_USES);
    setBombUses(GameConfig.ITEM_INITIAL_USES);
    setDoubleDmgUses(GameConfig.ITEM_INITIAL_USES);
    setFreezeTimer(0);
    setDoubleDmgTimer(0);
    setRevivesUsedCount(0);
    setShowGameOverModal(false);
    setShowReviveModal(false);
    setLotteryCards(null);

    spawnMonsterRow();
  }, [spawnMonsterRow]);

  useEffect(() => {
    resetGame();
  }, [resetGame]);

  const getCoordinatesAtDistance = (dist: number): [number, number, number] => {
    const startY = 320;
    const rowSpacing = 160;
    const leftBound = 90;
    const rightBound = GameConfig.LOGICAL_WIDTH - 90;
    const rowLen = rightBound - leftBound;
    const turnLen = rowSpacing;

    let remaining = dist;
    let curY = startY;

    for (let r = 0; r < GameConfig.SNAKE_TOTAL_ROWS; r++) {
      const isEven = r % 2 === 0;
      const startX = isEven ? rightBound : leftBound;
      const endX = isEven ? leftBound : rightBound;

      if (remaining <= rowLen) {
        const ratio = Math.max(0, Math.min(1, remaining / rowLen));
        const x = startX + (endX - startX) * ratio;
        return [x, curY, r];
      }
      remaining -= rowLen;

      if (r < GameConfig.SNAKE_TOTAL_ROWS - 1) {
        if (remaining <= turnLen) {
          const ratio = Math.max(0, Math.min(1, remaining / turnLen));
          const y = curY + turnLen * ratio;
          return [endX, y, r];
        }
        remaining -= turnLen;
        curY += rowSpacing;
      }
    }
    return [rightBound, startY + (GameConfig.SNAKE_TOTAL_ROWS - 1) * rowSpacing, GameConfig.SNAKE_TOTAL_ROWS - 1];
  };

  const spawnDamageText = (x: number, y: number, amount: number, color = '#FFFFFF') => {
    const e = entitiesRef.current;
    if (e.damageTexts.length < 50) {
      e.damageTexts.push({
        active: true,
        x,
        y,
        text: `-${Math.round(amount)}`,
        lifespan: 0,
        color,
      });
    }
  };

  const spawnParticles = (x: number, y: number, color: string) => {
    const e = entitiesRef.current;
    for (let i = 0; i < 10; i++) {
      if (e.particles.length < 120) {
        const angle = Math.random() * Math.PI * 2;
        const speed = Math.random() * 260 + 80;
        e.particles.push({
          active: true,
          x,
          y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          size: Math.random() * 6 + 3,
          lifespan: 0,
          color,
        });
      }
    }
  };

  const getLuckProbs = useCallback(() => {
    const p1 = Math.min(0.9, Math.max(0.1, GameConfig.LUCK_BASE_PROBABILITIES[0] + luckLevel * GameConfig.LUCK_PROB_STEP_PER_LEVEL[0]));
    const p2 = Math.min(0.6, Math.max(0.05, GameConfig.LUCK_BASE_PROBABILITIES[1] + luckLevel * GameConfig.LUCK_PROB_STEP_PER_LEVEL[1]));
    const p3 = Math.max(0.01, 1.0 - p1 - p2);
    return [p1, p2, p3];
  }, [luckLevel]);

  const rollThreeCards = useCallback((): SkillCard[] => {
    const probs = getLuckProbs();
    const cards: SkillCard[] = [];
    const s = skillsRef.current;

    const rollStar = () => {
      const r = Math.random();
      if (r < probs[0]) return 1;
      if (r < probs[0] + probs[1]) return 2;
      return 3;
    };

    while (cards.length < 3) {
      const star = rollStar();
      const possibleTypes: SkillType[] = ['INCREASE_FIRE_RATE', 'SKILL_DAMAGE_BOOST', 'BALL_DAMAGE_BOOST', 'SKILL_HASTE'];
      if (star >= 2) possibleTypes.push('HOMING_MISSILE');
      if (star === 3 && s.laserCount < GameConfig.LASER_MAX_SLOTS) possibleTypes.push('LASER_CANNON');

      const type = possibleTypes[Math.floor(Math.random() * possibleTypes.length)];
      if (cards.some((c) => c.type === type)) continue;

      let card: SkillCard;
      if (type === 'INCREASE_FIRE_RATE') {
        const add = GameConfig.SKILL_FIRE_RATE_TIERS[star - 1];
        const cur = 1 / GameConfig.BASE_FIRE_INTERVAL_SEC + s.extraFireRate;
        card = {
          type,
          star,
          title: '發球加速',
          description: `主動彈球射擊速率增加 +${add} 顆/秒`,
          currentValueStr: `目前: ${cur.toFixed(2)} 顆/秒`,
          nextValueStr: `升級後: ${(cur + add).toFixed(2)} 顆/秒`,
        };
      } else if (type === 'LASER_CANNON') {
        card = {
          type,
          star: 3,
          title: '高能貫穿雷射砲',
          description: '在戰機兩翼安裝貫穿直線雷射，每2秒掃蕩整行敵軍',
          currentValueStr: `目前槽位: ${s.laserCount}/${GameConfig.LASER_MAX_SLOTS}`,
          nextValueStr: `升級後: ${s.laserCount + 1}/${GameConfig.LASER_MAX_SLOTS} 座`,
        };
      } else if (type === 'SKILL_DAMAGE_BOOST') {
        const add = GameConfig.SKILL_DAMAGE_BONUS_TIERS[star - 1];
        card = {
          type,
          star,
          title: '技能增強',
          description: `提升副武器（雷射與飛彈）傷害 +${Math.round(add * 100)}%`,
          currentValueStr: `加成: +${Math.round(s.skillDmgBonus * 100)}%`,
          nextValueStr: `升級後: +${Math.round((s.skillDmgBonus + add) * 100)}%`,
        };
      } else if (type === 'BALL_DAMAGE_BOOST') {
        const add = GameConfig.BALL_DAMAGE_BONUS_TIERS[star - 1];
        card = {
          type,
          star,
          title: '球傷害增強',
          description: `提升彈球基礎碰撞傷害 +${Math.round(add * 100)}%`,
          currentValueStr: `加成: +${Math.round(s.ballDmgBonus * 100)}%`,
          nextValueStr: `升級後: +${Math.round((s.ballDmgBonus + add) * 100)}%`,
        };
      } else if (type === 'HOMING_MISSILE') {
        const curDmg = (GameConfig.MISSILE_INITIAL_DAMAGE + s.missileLevel * 150) * (1 + s.skillDmgBonus);
        card = {
          type,
          star: 2,
          title: s.hasMissile ? '飛彈強化' : '導引飛彈系統',
          description: '鎖定前排 3 隻敵人，延遲 0.7 秒發射高爆追蹤飛彈',
          currentValueStr: s.hasMissile ? `傷害: ${Math.round(curDmg)}` : '未裝備',
          nextValueStr: s.hasMissile ? `傷害: ${Math.round(curDmg + 150 * (1 + s.skillDmgBonus))}` : '初始傷害: 500',
        };
      } else {
        const haste = GameConfig.SKILL_HASTE_TIERS[star - 1];
        card = {
          type,
          star,
          title: '技能加速',
          description: `縮短雷射與飛彈技能冷卻，加速比率 +${haste}x`,
          currentValueStr: `冷卻倍速: ${(1 + s.skillHaste).toFixed(1)}x`,
          nextValueStr: `升級後: ${(1 + s.skillHaste + haste).toFixed(1)}x`,
        };
      }
      cards.push(card);
    }
    return cards;
  }, [getLuckProbs]);

  const applySkillCard = (card: SkillCard) => {
    const s = skillsRef.current;
    if (card.type === 'INCREASE_FIRE_RATE') {
      s.extraFireRate += GameConfig.SKILL_FIRE_RATE_TIERS[card.star - 1];
    } else if (card.type === 'LASER_CANNON') {
      if (s.laserCount < GameConfig.LASER_MAX_SLOTS) s.laserCount++;
    } else if (card.type === 'SKILL_DAMAGE_BOOST') {
      s.skillDmgBonus += GameConfig.SKILL_DAMAGE_BONUS_TIERS[card.star - 1];
    } else if (card.type === 'BALL_DAMAGE_BOOST') {
      s.ballDmgBonus += GameConfig.BALL_DAMAGE_BONUS_TIERS[card.star - 1];
    } else if (card.type === 'HOMING_MISSILE') {
      if (!s.hasMissile) s.hasMissile = true;
      else s.missileLevel++;
    } else if (card.type === 'SKILL_HASTE') {
      s.skillHaste += GameConfig.SKILL_HASTE_TIERS[card.star - 1];
    }
  };

  const onMonsterDefeated = (m: MonsterEntity) => {
    spawnParticles(m.x, m.y, m.isChest ? '#FBBF24' : '#F43F5E');

    if (m.isChest) {
      webAudio.playChestOpen();
      setCurrentScore((prev) => {
        const next = prev + GameConfig.SCORE_CHEST_BONUS;
        if (next > bestScore) onBestScoreChange(next);
        return next;
      });
      entitiesRef.current.pendingChests++;
    } else if (m.isBoss) {
      webAudio.playExplode();
      setCurrentScore((prev) => {
        const bonus = Math.floor(m.maxHp * GameConfig.SCORE_MONSTER_KILL_HP_RATIO) + GameConfig.SCORE_BOSS_BONUS;
        const next = prev + bonus;
        if (next > bestScore) onBestScoreChange(next);
        return next;
      });
      onReviveTicketsChange(reviveTickets + 1);
    } else {
      webAudio.playExplode();
      setCurrentScore((prev) => {
        const killScore = Math.max(10, Math.floor(m.maxHp * GameConfig.SCORE_MONSTER_KILL_HP_RATIO));
        const next = prev + killScore;
        if (next > bestScore) onBestScoreChange(next);
        return next;
      });
    }
  };

  const clampBallAngle = (ball: BallEntity) => {
    let angle = Math.atan2(ball.vy, ball.vx);
    const minRad = (GameConfig.BALL_MIN_HORIZONTAL_ANGLE_DEG * Math.PI) / 180;
    if (Math.abs(angle) < minRad) {
      angle = angle >= 0 ? minRad : -minRad;
    }
    const pi = Math.PI;
    if (Math.abs(angle - pi) < minRad) {
      angle = pi - minRad;
    } else if (Math.abs(angle + pi) < minRad) {
      angle = -pi + minRad;
    }
    ball.vx = Math.cos(angle) * GameConfig.BALL_SPEED;
    ball.vy = Math.sin(angle) * GameConfig.BALL_SPEED;
  };

  const fixedUpdate = (dt: number) => {
    const e = entitiesRef.current;
    const s = skillsRef.current;

    if (e.invincibleTimer > 0) e.invincibleTimer -= dt;

    const effectiveFireRate = 1 / GameConfig.BASE_FIRE_INTERVAL_SEC + s.extraFireRate;
    const fireInterval = 1 / effectiveFireRate;
    e.shootCooldownTimer += dt;

    const shouldShoot = e.isTouching || (continuousShoot && (e.lastAimX !== 0 || e.lastAimY !== 0));

    if (shouldShoot && e.shootCooldownTimer >= fireInterval) {
      e.shootCooldownTimer = 0;
      const targetX = e.isTouching ? e.touchX : 540 + e.lastAimX * 200;
      const targetY = e.isTouching ? e.touchY : 2050 + e.lastAimY * 200;

      let dirX = targetX - 540;
      let dirY = targetY - 2050;
      if (dirY >= -40) dirY = -40;

      const len = Math.sqrt(dirX * dirX + dirY * dirY);
      if (len > 0.001) {
        e.lastAimX = dirX / len;
        e.lastAimY = dirY / len;
      }

      let ballToUse: BallEntity | undefined = e.balls.find((b) => !b.active);
      if (!ballToUse && e.balls.length >= GameConfig.BALL_MAX_COUNT) {
        e.balls.sort((a, b) => a.spawnTimestamp - b.spawnTimestamp);
        ballToUse = e.balls[0];
      } else if (!ballToUse) {
        ballToUse = {
          active: true,
          x: 540,
          y: 2020,
          vx: 0,
          vy: 0,
          damage: 100,
          lifespan: 0,
          bounceCount: 0,
          spawnTimestamp: Date.now(),
        };
        e.balls.push(ballToUse);
      }

      const dmgMult = doubleDmgTimer > 0 ? 2 : 1;
      const baseDmg = GameConfig.BASE_BALL_DAMAGE + ballDamageLevel * 15;
      const ballDmg = baseDmg * (1 + s.ballDmgBonus) * dmgMult;

      ballToUse.active = true;
      ballToUse.x = 540;
      ballToUse.y = 2020;
      ballToUse.vx = (dirX / len) * GameConfig.BALL_SPEED;
      ballToUse.vy = (dirY / len) * GameConfig.BALL_SPEED;
      ballToUse.damage = ballDmg;
      ballToUse.lifespan = 0;
      ballToUse.bounceCount = 0;
      ballToUse.spawnTimestamp = Date.now();
      clampBallAngle(ballToUse);

      webAudio.playShoot();
    }

    for (const b of e.balls) {
      if (!b.active) continue;
      b.lifespan += dt;
      if (b.lifespan >= GameConfig.BALL_MAX_LIFESPAN_SEC || b.bounceCount >= GameConfig.BALL_MAX_BOUNCES) {
        b.active = false;
        continue;
      }

      b.x += b.vx * dt;
      b.y += b.vy * dt;

      if (b.x - GameConfig.BALL_RADIUS <= 0) {
        b.x = GameConfig.BALL_RADIUS;
        b.vx = -b.vx;
        b.bounceCount++;
        clampBallAngle(b);
      } else if (b.x + GameConfig.BALL_RADIUS >= GameConfig.LOGICAL_WIDTH) {
        b.x = GameConfig.LOGICAL_WIDTH - GameConfig.BALL_RADIUS;
        b.vx = -b.vx;
        b.bounceCount++;
        clampBallAngle(b);
      }
      if (b.y - GameConfig.BALL_RADIUS <= 220) {
        b.y = 220 + GameConfig.BALL_RADIUS;
        b.vy = -b.vy;
        b.bounceCount++;
        clampBallAngle(b);
      }
      if (b.y + GameConfig.BALL_RADIUS >= 2090) {
        b.active = false;
      }
    }

    for (const b of e.balls) {
      if (!b.active) continue;
      for (const m of e.monsters) {
        if (!m.active || m.pathProgress < 0) continue;

        const dx = b.x - m.x;
        const dy = b.y - m.y;
        const distSq = dx * dx + dy * dy;
        const combinedR = GameConfig.BALL_RADIUS + m.radius;

        if (distSq <= combinedR * combinedR) {
          const dist = Math.sqrt(distSq);
          const nx = dist > 0.001 ? dx / dist : 0;
          const ny = dist > 0.001 ? dy / dist : -1;

          m.currentHp -= b.damage;
          spawnDamageText(m.x, m.y, b.damage);
          webAudio.playHit();

          if (m.currentHp <= 0) {
            m.active = false;
            onMonsterDefeated(m);
          }

          if (GameConfig.BOUNCE_ON_MONSTER) {
            const dot = b.vx * nx + b.vy * ny;
            b.vx = b.vx - 2 * dot * nx;
            b.vy = b.vy - 2 * dot * ny;
            clampBallAngle(b);
            b.bounceCount++;
          } else {
            b.active = false;
          }
          break;
        }
      }
    }

    if (freezeTimer <= 0) {
      const row0Count = e.monsters.filter((m) => m.active && m.rowIndex === 0).length;
      const halfCapacity = GameConfig.MONSTERS_PER_ROW / 2;

      if (row0Count <= halfCapacity && !e.isCatchingUp && !e.catchUpWaitingRow1Mid) {
        e.isCatchingUp = true;
        e.catchUpWaitingRow1Mid = true;
      }

      if (e.catchUpWaitingRow1Mid) {
        const rowLen = GameConfig.LOGICAL_WIDTH - 180;
        const midDist = rowLen + 160 + rowLen * 0.5;
        const reached = e.monsters.some((m) => m.active && m.pathProgress >= midDist);
        if (reached) {
          e.isCatchingUp = false;
          e.catchUpWaitingRow1Mid = false;
        }
      }

      const speed = e.isCatchingUp
        ? GameConfig.MONSTER_BASE_SPEED * GameConfig.CATCH_UP_SPEED_MULTIPLIER
        : GameConfig.MONSTER_BASE_SPEED;

      const totalPathLen =
        GameConfig.SNAKE_TOTAL_ROWS * (GameConfig.LOGICAL_WIDTH - 180) +
        (GameConfig.SNAKE_TOTAL_ROWS - 1) * 160;

      for (const m of e.monsters) {
        if (!m.active) continue;
        m.pathProgress += speed * dt;

        if (m.pathProgress >= 0) {
          const [nx, ny, row] = getCoordinatesAtDistance(m.pathProgress);
          m.x = nx;
          m.y = ny;
          m.rowIndex = row;

          if (m.pathProgress >= totalPathLen) {
            if (e.invincibleTimer <= 0) {
              if (revivesUsedCount < GameConfig.REVIVE_MAX_TIMES) {
                e.isPaused = true;
                setShowReviveModal(true);
              } else {
                e.isPaused = true;
                triggerGameOver();
              }
              return;
            }
          }
        }
      }

      const activeCount = e.monsters.filter((m) => m.active).length;
      if (activeCount <= GameConfig.MONSTERS_PER_ROW / 2) {
        spawnMonsterRow();
      }
    }

    const laserDmgMult = doubleDmgTimer > 0 ? 2 : 1;
    const laserDmg = GameConfig.LASER_INITIAL_DAMAGE * (1 + s.skillDmgBonus) * laserDmgMult;
    const laserCooldown = 2.0 / (1 + s.skillHaste);

    for (let i = 0; i < s.laserCount; i++) {
      const cannon = e.laserCannons[i];
      if (cannon.firingTimer > 0) cannon.firingTimer -= dt;

      cannon.cooldownTimer += dt;
      if (cannon.cooldownTimer >= laserCooldown) {
        cannon.cooldownTimer = 0;
        cannon.firingTimer = 0.22;
        webAudio.playLaser();

        const beamHalf = 24;
        for (const m of e.monsters) {
          if (m.active && Math.abs(m.x - cannon.x) <= m.radius + beamHalf) {
            m.currentHp -= laserDmg;
            spawnDamageText(m.x, m.y, laserDmg, '#38BDF8');
            if (m.currentHp <= 0) {
              m.active = false;
              onMonsterDefeated(m);
            }
          }
        }
      }
    }

    if (s.hasMissile) {
      e.missileCooldownTimer += dt;
      const missileCooldown = GameConfig.MISSILE_COOLDOWN_SEC / (1 + s.skillHaste);

      if (!e.isMissileLocking && e.missileCooldownTimer >= missileCooldown - GameConfig.MISSILE_LOCK_DELAY_SEC) {
        e.isMissileLocking = true;
        e.missileLockingTimer = 0;
        e.lockedTargetIds = [];

        const targets = e.monsters
          .filter((m) => m.active && m.pathProgress >= 0)
          .sort((a, b) => b.pathProgress - a.pathProgress)
          .slice(0, GameConfig.MISSILE_LOCK_COUNT);

        targets.forEach((m) => {
          m.isTargetLocked = true;
          e.lockedTargetIds.push(m.id);
        });
      }

      if (e.isMissileLocking) {
        e.missileLockingTimer += dt;
        if (e.missileLockingTimer >= GameConfig.MISSILE_LOCK_DELAY_SEC) {
          e.isMissileLocking = false;
          e.missileCooldownTimer = 0;

          const mDmgMult = doubleDmgTimer > 0 ? 2 : 1;
          const missileDmg = (GameConfig.MISSILE_INITIAL_DAMAGE + s.missileLevel * 150) * (1 + s.skillDmgBonus) * mDmgMult;

          e.lockedTargetIds.forEach((targetId) => {
            const m = e.monsters.find((item) => item.id === targetId);
            if (m) m.isTargetLocked = false;
            if (m && m.active) {
              e.missiles.push({
                active: true,
                x: 540,
                y: 2010,
                vx: 0,
                vy: -1400,
                targetId: m.id,
                damage: missileDmg,
              });
            }
          });
          e.lockedTargetIds = [];
        }
      }

      for (const missile of e.missiles) {
        if (!missile.active) continue;
        const tgt = e.monsters.find((m) => m.id === missile.targetId);
        if (!tgt || !tgt.active) {
          missile.y += missile.vy * dt;
          if (missile.y < 200) missile.active = false;
          continue;
        }

        const dx = tgt.x - missile.x;
        const dy = tgt.y - missile.y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= tgt.radius + 18) {
          tgt.currentHp -= missile.damage;
          spawnDamageText(tgt.x, tgt.y, missile.damage, '#F43F5E');
          webAudio.playExplode();
          if (tgt.currentHp <= 0) {
            tgt.active = false;
            onMonsterDefeated(tgt);
          }
          missile.active = false;
          continue;
        }

        const angle = Math.atan2(dy, dx);
        const steer = 8.5 * dt;
        const spd = 1400;
        missile.vx += (Math.cos(angle) * spd - missile.vx) * steer;
        missile.vy += (Math.sin(angle) * spd - missile.vy) * steer;
        missile.x += missile.vx * dt;
        missile.y += missile.vy * dt;
      }
    }

    for (const txt of e.damageTexts) {
      if (!txt.active) continue;
      txt.lifespan += dt;
      txt.y -= 90 * dt;
      if (txt.lifespan >= 0.65) txt.active = false;
    }
    for (const p of e.particles) {
      if (!p.active) continue;
      p.lifespan += dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      if (p.lifespan >= 0.5) p.active = false;
    }

    if (e.pendingChests > 0 && !lotteryCards && !showReviveModal && !showGameOverModal) {
      e.pendingChests--;
      e.isPaused = true;
      setLotteryCards(rollThreeCards());
    }
  };

  const triggerGameOver = () => {
    const earned = Math.floor(currentScore / GameConfig.COIN_SCORE_DIVISOR) + survivedRowsCount * GameConfig.COIN_ROW_SURVIVAL_BONUS;
    setEarnedCoinsFinal(earned);
    onCoinsChange(coins + earned);
    setShowGameOverModal(true);
  };

  const renderCanvas = useCallback(
    (ctx: CanvasRenderingContext2D, animTime: number) => {
      const e = entitiesRef.current;
      const s = skillsRef.current;

      ctx.clearRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);

      ctx.fillStyle = '#060814';
      ctx.fillRect(0, 0, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT);

      e.stars.forEach((star, i) => {
        const alpha = 0.5 + 0.5 * Math.sin(animTime * 3 + i);
        ctx.fillStyle = `rgba(255, 255, 255, ${alpha.toFixed(2)})`;
        ctx.beginPath();
        ctx.arc(star.x, star.y, star.radius, 0, Math.PI * 2);
        ctx.fill();
      });

      ctx.strokeStyle = 'rgba(56, 189, 248, 0.15)';
      ctx.lineWidth = 3;
      ctx.setLineDash([16, 12]);
      ctx.beginPath();
      const startY = 320;
      const rowSpacing = 160;
      const leftBound = 90;
      const rightBound = GameConfig.LOGICAL_WIDTH - 90;

      for (let r = 0; r < GameConfig.SNAKE_TOTAL_ROWS; r++) {
        const fromX = r % 2 === 0 ? rightBound : leftBound;
        const toX = r % 2 === 0 ? leftBound : rightBound;
        const curY = startY + r * rowSpacing;
        if (r === 0) ctx.moveTo(fromX, curY);
        ctx.lineTo(toX, curY);
        if (r < GameConfig.SNAKE_TOTAL_ROWS - 1) {
          ctx.lineTo(toX, curY + rowSpacing);
        }
      }
      ctx.stroke();
      ctx.setLineDash([]);

      if (e.isTouching) {
        let dirX = e.touchX - 540;
        let dirY = e.touchY - 2050;
        if (dirY >= -40) dirY = -40;
        const len = Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0.001) {
          let nx = dirX / len;
          let ny = dirY / len;

          ctx.strokeStyle = 'rgba(254, 240, 138, 0.65)';
          ctx.lineWidth = 4;
          ctx.setLineDash([14, 10]);
          ctx.beginPath();
          ctx.moveTo(540, 2020);

          let curX = 540;
          let curY = 2020;
          let minT = 2000;
          let normX = 0;
          let normY = 0;

          if (nx < -0.0001) {
            const t = (20 - curX) / nx;
            if (t > 0 && t < minT) {
              minT = t;
              normX = 1;
              normY = 0;
            }
          }
          if (nx > 0.0001) {
            const t = (GameConfig.LOGICAL_WIDTH - 20 - curX) / nx;
            if (t > 0 && t < minT) {
              minT = t;
              normX = -1;
              normY = 0;
            }
          }
          if (ny < -0.0001) {
            const t = (220 - curY) / ny;
            if (t > 0 && t < minT) {
              minT = t;
              normX = 0;
              normY = 1;
            }
          }

          const hitX1 = curX + nx * minT;
          const hitY1 = curY + ny * minT;
          ctx.lineTo(hitX1, hitY1);

          if (normX !== 0 || normY !== 0) {
            const dot = nx * normX + ny * normY;
            nx = nx - 2 * dot * normX;
            ny = ny - 2 * dot * normY;
            let minT2 = 1200;
            if (nx < -0.0001) {
              const t = (20 - hitX1) / nx;
              if (t > 0 && t < minT2) minT2 = t;
            }
            if (nx > 0.0001) {
              const t = (GameConfig.LOGICAL_WIDTH - 20 - hitX1) / nx;
              if (t > 0 && t < minT2) minT2 = t;
            }
            if (ny < -0.0001) {
              const t = (220 - hitY1) / ny;
              if (t > 0 && t < minT2) minT2 = t;
            }
            ctx.lineTo(hitX1 + nx * minT2, hitY1 + ny * minT2);
          }
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }

      for (const m of e.monsters) {
        if (!m.active || m.pathProgress < 0) continue;

        ctx.save();
        ctx.translate(m.x, m.y);

        if (m.kind === 'CHEST') {
          ctx.fillStyle = '#F59E0B';
          ctx.beginPath();
          ctx.roundRect(-m.radius, -m.radius * 0.7, m.radius * 2, m.radius * 1.4, 12);
          ctx.fill();
          ctx.fillStyle = '#FEF3C7';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.3, 0, Math.PI * 2);
          ctx.fill();
        } else if (m.kind === 'BOSS') {
          ctx.fillStyle = '#DC2626';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#F59E0B';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.55, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#FEF08A';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.25, 0, Math.PI * 2);
          ctx.fill();
          ctx.beginPath();
          ctx.moveTo(-m.radius * 0.8, -m.radius * 0.5);
          ctx.lineTo(0, -m.radius * 1.35);
          ctx.lineTo(m.radius * 0.8, -m.radius * 0.5);
          ctx.closePath();
          ctx.fill();
        } else if (m.kind === 'OCTOPUS') {
          ctx.fillStyle = '#E11D48';
          ctx.beginPath();
          ctx.arc(0, -6, m.radius * 0.85, 0, Math.PI * 2);
          ctx.fill();
          ctx.strokeStyle = '#BE123C';
          ctx.lineWidth = 6;
          for (let i = -2; i <= 2; i++) {
            if (i === 0) continue;
            ctx.beginPath();
            ctx.moveTo(i * m.radius * 0.25, m.radius * 0.4);
            const sway = Math.sin(animTime * 6 + i) * 8;
            ctx.lineTo(i * m.radius * 0.38, m.radius * 0.75 + sway);
            ctx.stroke();
          }
          ctx.fillStyle = '#FEF08A';
          ctx.beginPath();
          ctx.arc(-m.radius * 0.32, -m.radius * 0.2, m.radius * 0.2, 0, Math.PI * 2);
          ctx.arc(m.radius * 0.32, -m.radius * 0.2, m.radius * 0.2, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#0F172A';
          ctx.beginPath();
          ctx.arc(-m.radius * 0.32, -m.radius * 0.2, m.radius * 0.09, 0, Math.PI * 2);
          ctx.arc(m.radius * 0.32, -m.radius * 0.2, m.radius * 0.09, 0, Math.PI * 2);
          ctx.fill();
        } else if (m.kind === 'CYCLOPS') {
          ctx.fillStyle = '#10B981';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#FFFFFF';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.5, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#059669';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.28, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#0F172A';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.14, 0, Math.PI * 2);
          ctx.fill();
        } else if (m.kind === 'DRONE') {
          ctx.fillStyle = '#0284C7';
          ctx.beginPath();
          ctx.roundRect(-m.radius * 0.8, -m.radius * 0.8, m.radius * 1.6, m.radius * 1.6, 10);
          ctx.fill();
          ctx.fillStyle = '#38BDF8';
          ctx.beginPath();
          ctx.arc(0, 0, m.radius * 0.35, 0, Math.PI * 2);
          ctx.fill();
        } else {
          ctx.fillStyle = '#D97706';
          ctx.beginPath();
          ctx.moveTo(0, m.radius);
          ctx.lineTo(-m.radius, -m.radius * 0.7);
          ctx.lineTo(0, -m.radius * 0.2);
          ctx.lineTo(m.radius, -m.radius * 0.7);
          ctx.closePath();
          ctx.fill();
        }

        ctx.restore();

        const barW = m.radius * 2.2;
        const barH = 10;
        const barY = m.y - m.radius - 22;

        ctx.fillStyle = 'rgba(15, 23, 42, 0.8)';
        ctx.beginPath();
        ctx.roundRect(m.x - barW / 2, barY, barW, barH, 5);
        ctx.fill();

        const ratio = Math.max(0, Math.min(1, m.currentHp / m.maxHp));
        ctx.fillStyle = m.isChest ? '#FBBF24' : m.isBoss ? '#DC2626' : '#38BDF8';
        ctx.beginPath();
        ctx.roundRect(m.x - barW / 2, barY, barW * ratio, barH, 5);
        ctx.fill();

        ctx.fillStyle = m.isChest ? '#FEF08A' : '#FFFFFF';
        ctx.font = 'bold 24px monospace';
        ctx.textAlign = 'center';
        ctx.fillText(formatHp(m.currentHp), m.x, barY - 6);

        if (m.isTargetLocked) {
          ctx.strokeStyle = '#F43F5E';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.arc(m.x, m.y, m.radius * 1.3, 0, Math.PI * 2);
          ctx.stroke();
          ctx.beginPath();
          ctx.moveTo(m.x - m.radius * 1.5, m.y);
          ctx.lineTo(m.x + m.radius * 1.5, m.y);
          ctx.moveTo(m.x, m.y - m.radius * 1.5);
          ctx.lineTo(m.x, m.y + m.radius * 1.5);
          ctx.stroke();
        }
      }

      for (let i = 0; i < 4; i++) {
        const cannon = e.laserCannons[i];
        const equipped = i < s.laserCount;

        if (!equipped) {
          ctx.strokeStyle = 'rgba(56, 189, 248, 0.3)';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.arc(cannon.x, cannon.y, 20, 0, Math.PI * 2);
          ctx.stroke();
        } else {
          ctx.fillStyle = '#0F172A';
          ctx.beginPath();
          ctx.arc(cannon.x, cannon.y, 22, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#38BDF8';
          ctx.beginPath();
          ctx.arc(cannon.x, cannon.y, 12, 0, Math.PI * 2);
          ctx.fill();

          if (cannon.firingTimer > 0) {
            const alpha = Math.min(1, cannon.firingTimer / 0.22);
            ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`;
            ctx.lineWidth = 14;
            ctx.beginPath();
            ctx.moveTo(cannon.x, cannon.y);
            ctx.lineTo(cannon.x, 220);
            ctx.stroke();

            ctx.strokeStyle = `rgba(255, 255, 255, ${alpha})`;
            ctx.lineWidth = 6;
            ctx.beginPath();
            ctx.moveTo(cannon.x, cannon.y);
            ctx.lineTo(cannon.x, 220);
            ctx.stroke();
          }
        }
      }

      for (const missile of e.missiles) {
        if (!missile.active) continue;
        ctx.strokeStyle = 'rgba(251, 146, 60, 0.7)';
        ctx.lineWidth = 6;
        ctx.beginPath();
        ctx.moveTo(missile.x, missile.y);
        ctx.lineTo(missile.x - missile.vx * 0.03, missile.y - missile.vy * 0.03);
        ctx.stroke();

        ctx.fillStyle = '#F43F5E';
        ctx.beginPath();
        ctx.arc(missile.x, missile.y, 9, 0, Math.PI * 2);
        ctx.fill();
      }

      for (const b of e.balls) {
        if (!b.active) continue;
        ctx.fillStyle = 'rgba(56, 189, 248, 0.35)';
        ctx.beginPath();
        ctx.arc(b.x, b.y, GameConfig.BALL_RADIUS * 1.5, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#FEF08A';
        ctx.beginPath();
        ctx.arc(b.x, b.y, GameConfig.BALL_RADIUS, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.save();
      ctx.translate(540, 2050);

      const flame = 18 + Math.sin(animTime * 18) * 6;
      ctx.fillStyle = '#38BDF8';
      ctx.beginPath();
      ctx.arc(0, 22 + flame * 0.5, 10, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = '#E0F2FE';
      ctx.beginPath();
      ctx.moveTo(0, -50);
      ctx.lineTo(-44, 36);
      ctx.lineTo(0, 16);
      ctx.lineTo(44, 36);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = '#0EA5E9';
      ctx.beginPath();
      ctx.arc(0, -8, 12, 0, Math.PI * 2);
      ctx.fill();

      if (e.invincibleTimer > 0) {
        ctx.strokeStyle = 'rgba(251, 191, 36, 0.75)';
        ctx.lineWidth = 6;
        ctx.beginPath();
        ctx.arc(0, 0, 72, 0, Math.PI * 2);
        ctx.stroke();
      }
      ctx.restore();

      for (const p of e.particles) {
        if (!p.active) continue;
        const alpha = Math.max(0, 1 - p.lifespan / 0.5);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1.0;
      }

      for (const txt of e.damageTexts) {
        if (!txt.active) continue;
        const alpha = Math.max(0, 1 - txt.lifespan / 0.65);
        ctx.fillStyle = txt.color;
        ctx.globalAlpha = alpha;
        ctx.font = 'bold 30px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(txt.text, txt.x, txt.y);
        ctx.globalAlpha = 1.0;
      }
    },
    []
  );

  useEffect(() => {
    let animationFrameId: number;
    let lastTime = performance.now();
    let animClock = 0;
    let frameCounter = 0;
    let fpsTimestamp = performance.now();

    const loop = (currentTime: number) => {
      let elapsed = (currentTime - lastTime) / 1000;
      lastTime = currentTime;
      if (elapsed > 0.25) elapsed = 0.25;

      const e = entitiesRef.current;

      if (!e.isPaused) {
        const scaledElapsed = elapsed * speedMultiplier;
        animClock += scaledElapsed;

        e.stars.forEach((star) => {
          star.y += star.speed * scaledElapsed;
          if (star.y > GameConfig.LOGICAL_HEIGHT) {
            star.y = 0;
            star.x = Math.random() * GameConfig.LOGICAL_WIDTH;
          }
        });

        if (freezeTimer > 0) {
          setFreezeTimer((t) => Math.max(0, t - scaledElapsed));
        }
        if (doubleDmgTimer > 0) {
          setDoubleDmgTimer((t) => Math.max(0, t - scaledElapsed));
        }

        const fixedDt = GameConfig.FIXED_DELTA_TIME;
        let acc = scaledElapsed;
        while (acc >= fixedDt) {
          fixedUpdate(fixedDt);
          acc -= fixedDt;
        }
      }

      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) renderCanvas(ctx, animClock);
      }

      frameCounter++;
      if (currentTime - fpsTimestamp >= 1000) {
        setFpsDisplay(frameCounter);
        frameCounter = 0;
        fpsTimestamp = currentTime;
      }

      animationFrameId = requestAnimationFrame(loop);
    };

    animationFrameId = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(animationFrameId);
  }, [fixedUpdate, renderCanvas, speedMultiplier, freezeTimer, doubleDmgTimer]);

  const handlePointerDown = (ev: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const virtX = ((ev.clientX - rect.left) / rect.width) * GameConfig.LOGICAL_WIDTH;
    const virtY = ((ev.clientY - rect.top) / rect.height) * GameConfig.LOGICAL_HEIGHT;

    entitiesRef.current.isTouching = true;
    entitiesRef.current.touchX = virtX;
    entitiesRef.current.touchY = virtY;
  };

  const handlePointerMove = (ev: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas || !entitiesRef.current.isTouching) return;
    const rect = canvas.getBoundingClientRect();
    const virtX = ((ev.clientX - rect.left) / rect.width) * GameConfig.LOGICAL_WIDTH;
    const virtY = ((ev.clientY - rect.top) / rect.height) * GameConfig.LOGICAL_HEIGHT;

    entitiesRef.current.touchX = virtX;
    entitiesRef.current.touchY = virtY;
  };

  const handlePointerUp = () => {
    entitiesRef.current.isTouching = false;
  };

  const handleUseFreeze = () => {
    if (freezeUses > 0 && freezeTimer <= 0) {
      setFreezeUses((u) => u - 1);
      setFreezeTimer(GameConfig.ITEM_FREEZE_DURATION_SEC);
      webAudio.vibrate(50);
    }
  };

  const handleUseBomb = () => {
    if (bombUses > 0) {
      setBombUses((u) => u - 1);
      webAudio.playExplode();
      const mult = doubleDmgTimer > 0 ? 2 : 1;
      const dmg = GameConfig.ITEM_BOMB_DAMAGE * mult;
      entitiesRef.current.monsters.forEach((m) => {
        if (m.active && m.pathProgress >= 0) {
          m.currentHp -= dmg;
          spawnDamageText(m.x, m.y, dmg, '#F43F5E');
          if (m.currentHp <= 0) {
            m.active = false;
            onMonsterDefeated(m);
          }
        }
      });
    }
  };

  const handleUseDoubleDmg = () => {
    if (doubleDmgUses > 0 && doubleDmgTimer <= 0) {
      setDoubleDmgUses((u) => u - 1);
      setDoubleDmgTimer(GameConfig.ITEM_DOUBLE_DAMAGE_DURATION_SEC);
      webAudio.vibrate(50);
    }
  };

  const handleSelectSpeed = (speed: number) => {
    if (speed === 1) setSpeedMultiplier(1);
    else if (speed === 2) {
      if (is2xUnlocked) setSpeedMultiplier(2);
      else onRequestUnlockSpeed(2, GameConfig.UNLOCK_2X_COST);
    } else if (speed === 5) {
      if (is5xUnlocked) setSpeedMultiplier(5);
      else onRequestUnlockSpeed(5, GameConfig.UNLOCK_5X_COST);
    }
  };

  const handleRevive = (useTicket: boolean) => {
    if (useTicket) {
      if (reviveTickets > 0) {
        onReviveTicketsChange(reviveTickets - 1);
        performReviveSuccess();
      }
    } else {
      if (coins >= GameConfig.REVIVE_COST_COINS) {
        onCoinsChange(coins - GameConfig.REVIVE_COST_COINS);
        performReviveSuccess();
      }
    }
  };

  const performReviveSuccess = () => {
    setRevivesUsedCount((c) => c + 1);
    entitiesRef.current.invincibleTimer = GameConfig.REVIVE_INVINCIBLE_DURATION_SEC;
    const targetMinRow = Math.max(0, GameConfig.SNAKE_TOTAL_ROWS - GameConfig.REVIVE_CLEARED_ROWS);
    entitiesRef.current.monsters.forEach((m) => {
      if (m.active && m.rowIndex >= targetMinRow) m.active = false;
    });
    setShowReviveModal(false);
    entitiesRef.current.isPaused = false;
    webAudio.vibrate(80);
  };

  const s = skillsRef.current;
  const currentBallDmg = Math.round((GameConfig.BASE_BALL_DAMAGE + ballDamageLevel * 15) * (1 + s.ballDmgBonus));
  const currentBallBonus = Math.round(s.ballDmgBonus * 100);
  const currentSkillDmg = Math.round(GameConfig.LASER_INITIAL_DAMAGE * (1 + s.skillDmgBonus));
  const currentSkillBonus = Math.round(s.skillDmgBonus * 100);

  return (
    <div className="relative w-full h-full flex flex-col items-center justify-center bg-black overflow-hidden select-none">
      <div className="absolute top-0 left-0 right-0 z-10 bg-slate-950/85 backdrop-blur border-b border-slate-800 px-4 py-2.5 flex flex-col gap-1 max-w-[540px] mx-auto">
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-3">
            <span className="font-bold text-amber-400">💰 {coins}</span>
            <span className="font-semibold text-slate-100">分數: {currentScore}</span>
            <span className="text-slate-400">最佳: {bestScore}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={onOpenUpgrades}
              className="px-2 py-1 text-[11px] font-medium bg-amber-600/30 text-amber-300 border border-amber-500/50 rounded hover:bg-amber-600/40"
            >
              養成
            </button>
            <button
              onClick={onOpenPause}
              className="px-2 py-1 text-[11px] font-medium bg-slate-800 text-slate-200 border border-slate-700 rounded hover:bg-slate-700"
            >
              暫停
            </button>
          </div>
        </div>
        <div className="flex items-center justify-between text-[11px] text-slate-300">
          <div className="flex items-center gap-3">
            <span className="text-amber-300 font-medium">球 {currentBallDmg} (+{currentBallBonus}%)</span>
            <span className="text-sky-400 font-medium">技能 {currentSkillDmg} (+{currentSkillBonus}%)</span>
          </div>
          {showFps && <span className="text-emerald-400 font-mono text-[10px]">{fpsDisplay} FPS</span>}
        </div>
      </div>

      <div className="relative flex-1 w-full max-w-[540px] flex items-center justify-center">
        <canvas
          ref={canvasRef}
          width={GameConfig.LOGICAL_WIDTH}
          height={GameConfig.LOGICAL_HEIGHT}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          className="w-full h-full object-contain cursor-crosshair touch-none"
        />
      </div>

      <div className="absolute bottom-0 left-0 right-0 z-10 bg-slate-950/90 backdrop-blur border-t border-slate-800 px-4 py-2.5 flex items-center justify-between max-w-[540px] mx-auto">
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => handleSelectSpeed(1)}
            className={`px-2.5 py-1.5 text-xs font-bold rounded ${
              speedMultiplier === 1 ? 'bg-sky-500 text-slate-950' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            1x
          </button>
          <button
            onClick={() => handleSelectSpeed(2)}
            className={`px-2.5 py-1.5 text-xs font-bold rounded flex items-center gap-1 ${
              speedMultiplier === 2
                ? 'bg-sky-500 text-slate-950'
                : is2xUnlocked
                ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                : 'bg-slate-900 text-slate-500 border border-slate-800'
            }`}
          >
            2x {!is2xUnlocked && '🔒'}
          </button>
          <button
            onClick={() => handleSelectSpeed(5)}
            className={`px-2.5 py-1.5 text-xs font-bold rounded flex items-center gap-1 ${
              speedMultiplier === 5
                ? 'bg-sky-500 text-slate-950'
                : is5xUnlocked
                ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                : 'bg-slate-900 text-slate-500 border border-slate-800'
            }`}
          >
            5x {!is5xUnlocked && '🔒'}
          </button>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleUseFreeze}
            disabled={freezeUses <= 0 || freezeTimer > 0}
            className={`flex flex-col items-center px-2.5 py-1 rounded text-[11px] font-medium border ${
              freezeTimer > 0
                ? 'bg-sky-600/50 border-sky-400 text-sky-200 animate-pulse'
                : freezeUses > 0
                ? 'bg-slate-800 hover:bg-slate-700 border-slate-700 text-slate-200'
                : 'bg-slate-900 border-slate-800 text-slate-600'
            }`}
          >
            <span>❄️ 冰凍</span>
            <span className="text-[9px] opacity-80">{freezeTimer > 0 ? `${freezeTimer.toFixed(1)}s` : `${freezeUses}次`}</span>
          </button>

          <button
            onClick={handleUseBomb}
            disabled={bombUses <= 0}
            className={`flex flex-col items-center px-2.5 py-1 rounded text-[11px] font-medium border ${
              bombUses > 0
                ? 'bg-slate-800 hover:bg-slate-700 border-slate-700 text-slate-200'
                : 'bg-slate-900 border-slate-800 text-slate-600'
            }`}
          >
            <span>💣 炸藥</span>
            <span className="text-[9px] opacity-80">{bombUses}次</span>
          </button>

          <button
            onClick={handleUseDoubleDmg}
            disabled={doubleDmgUses <= 0 || doubleDmgTimer > 0}
            className={`flex flex-col items-center px-2.5 py-1 rounded text-[11px] font-medium border ${
              doubleDmgTimer > 0
                ? 'bg-amber-600/50 border-amber-400 text-amber-200 animate-pulse'
                : doubleDmgUses > 0
                ? 'bg-slate-800 hover:bg-slate-700 border-slate-700 text-slate-200'
                : 'bg-slate-900 border-slate-800 text-slate-600'
            }`}
          >
            <span>⚡ 雙倍</span>
            <span className="text-[9px] opacity-80">{doubleDmgTimer > 0 ? `${doubleDmgTimer.toFixed(1)}s` : `${doubleDmgUses}次`}</span>
          </button>
        </div>
      </div>

      {lotteryCards && (
        <div className="absolute inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[500px] w-full text-center">
            <h3 className="text-lg font-bold text-amber-400 mb-1">✨ 擊破星空寶箱！獲得技能抽取</h3>
            <p className="text-xs text-slate-400 mb-4">請選擇一項技能卡強化戰機</p>

            <div className="grid grid-cols-3 gap-2.5 mb-5">
              {lotteryCards.map((card, idx) => (
                <div
                  key={idx}
                  className="bg-slate-950 border border-slate-800 p-3 rounded-xl flex flex-col text-left hover:border-sky-500/80 transition-colors"
                >
                  <span className="text-amber-400 text-xs font-bold">{'★'.repeat(card.star)}</span>
                  <span className="text-white font-bold text-xs mt-1">{card.title}</span>
                  <span className="text-slate-400 text-[10px] mt-1 leading-tight flex-1">{card.description}</span>
                  <div className="mt-2 text-[10px] text-slate-300">{card.currentValueStr}</div>
                  <div className="text-[10px] text-emerald-400 font-semibold">{card.nextValueStr}</div>
                  <button
                    onClick={() => {
                      applySkillCard(card);
                      setLotteryCards(null);
                      entitiesRef.current.isPaused = false;
                    }}
                    className="mt-3 w-full py-1.5 text-xs font-medium bg-sky-600 hover:bg-sky-500 text-white rounded-lg transition-colors"
                  >
                    確定領取
                  </button>
                </div>
              ))}
            </div>

            <div className="flex items-center gap-3">
              <button
                disabled={coins < GameConfig.LOTTERY_ALL_COST}
                onClick={() => {
                  if (coins >= GameConfig.LOTTERY_ALL_COST) {
                    onCoinsChange(coins - GameConfig.LOTTERY_ALL_COST);
                    lotteryCards.forEach(applySkillCard);
                    setLotteryCards(null);
                    entitiesRef.current.isPaused = false;
                  }
                }}
                className={`flex-1 py-2 rounded-lg text-xs font-bold transition-colors ${
                  coins >= GameConfig.LOTTERY_ALL_COST
                    ? 'bg-amber-500 hover:bg-amber-400 text-slate-950'
                    : 'bg-slate-800 text-slate-500 cursor-not-allowed'
                }`}
              >
                全選領取 (200 金幣)
              </button>
              <button
                disabled={coins < GameConfig.LOTTERY_REROLL_COST}
                onClick={() => {
                  if (coins >= GameConfig.LOTTERY_REROLL_COST) {
                    onCoinsChange(coins - GameConfig.LOTTERY_REROLL_COST);
                    setLotteryCards(rollThreeCards());
                  }
                }}
                className={`flex-1 py-2 rounded-lg text-xs font-bold transition-colors ${
                  coins >= GameConfig.LOTTERY_REROLL_COST
                    ? 'bg-slate-800 hover:bg-slate-700 text-slate-200'
                    : 'bg-slate-900 text-slate-600 cursor-not-allowed'
                }`}
              >
                重新抽取 (50 金幣)
              </button>
            </div>
          </div>
        </div>
      )}

      {showReviveModal && (
        <div className="absolute inset-0 z-50 bg-black/85 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-rose-900/50 rounded-2xl p-6 max-w-[420px] w-full text-center">
            <h3 className="text-xl font-bold text-rose-400 mb-2">⚠️ 防線失守！是否復活？</h3>
            <p className="text-xs text-slate-300 leading-relaxed mb-4">
              怪物已越過終點線！立即消除最靠近終點的 2 排怪物並獲得 3 秒無敵。<br />
              本局剩餘復活次數: {GameConfig.REVIVE_MAX_TIMES - revivesUsedCount} 次
            </p>

            <div className="flex flex-col gap-2.5 mb-4">
              {reviveTickets > 0 && (
                <button
                  onClick={() => handleRevive(true)}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-xs transition-colors"
                >
                  使用復活券 (持有: {reviveTickets} 張)
                </button>
              )}
              <button
                disabled={coins < GameConfig.REVIVE_COST_COINS}
                onClick={() => handleRevive(false)}
                className={`w-full py-2.5 rounded-xl text-xs font-bold transition-colors ${
                  coins >= GameConfig.REVIVE_COST_COINS
                    ? 'bg-amber-500 hover:bg-amber-400 text-slate-950'
                    : 'bg-slate-800 text-slate-500 cursor-not-allowed'
                }`}
              >
                花費 300 金幣復活 (持有: {coins})
              </button>
            </div>

            <button
              onClick={() => {
                setShowReviveModal(false);
                triggerGameOver();
              }}
              className="text-xs text-slate-400 hover:text-white underline"
            >
              放棄防守，進入戰役結算
            </button>
          </div>
        </div>
      )}

      {showGameOverModal && (
        <div className="absolute inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[420px] w-full text-center">
            <h3 className="text-2xl font-black text-white mb-1">戰役結算</h3>
            <p className="text-xs text-slate-400 mb-6">星際防衛任務結束</p>

            <div className="bg-slate-950 border border-slate-800/80 rounded-xl p-4 mb-6 text-xs text-left space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">本局得分:</span>
                <span className="text-white font-bold">{currentScore}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">歷史最佳:</span>
                <span className="text-amber-400 font-bold">{bestScore}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">存活排數:</span>
                <span className="text-sky-400 font-bold">{survivedRowsCount} 排</span>
              </div>
              <div className="flex justify-between border-t border-slate-800 pt-2">
                <span className="text-slate-300 font-medium">獲得金幣獎勵:</span>
                <span className="text-amber-300 font-bold">+{earnedCoinsFinal} 金幣</span>
              </div>
            </div>

            <button
              onClick={resetGame}
              className="w-full py-3 bg-sky-600 hover:bg-sky-500 text-white font-bold rounded-xl text-sm transition-colors shadow-lg shadow-sky-600/25"
            >
              再來一局
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
