export type MonsterKind = 'OCTOPUS' | 'CYCLOPS' | 'DRONE' | 'CRUISER' | 'CHEST' | 'BOSS';

export interface MonsterEntity {
  id: number;
  active: boolean;
  kind: MonsterKind;
  x: number;
  y: number;
  radius: number;
  currentHp: number;
  maxHp: number;
  rowIndex: number;
  isChest: boolean;
  isBoss: boolean;
  pathProgress: number;
  isTargetLocked: boolean;
}

export interface BallEntity {
  active: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  damage: number;
  lifespan: number;
  bounceCount: number;
  spawnTimestamp: number;
}

export interface LaserCannonEntity {
  slotIndex: number;
  x: number;
  y: number;
  cooldownTimer: number;
  firingTimer: number;
}

export interface MissileEntity {
  active: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  targetId: number | null;
  damage: number;
}

export interface DamageTextEntity {
  active: boolean;
  x: number;
  y: number;
  text: string;
  lifespan: number;
  color: string;
}

export interface ParticleEntity {
  active: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  lifespan: number;
  color: string;
}

export type SkillType =
  | 'INCREASE_FIRE_RATE'
  | 'LASER_CANNON'
  | 'SKILL_DAMAGE_BOOST'
  | 'BALL_DAMAGE_BOOST'
  | 'HOMING_MISSILE'
  | 'SKILL_HASTE';

export interface SkillCard {
  type: SkillType;
  star: number;
  title: string;
  description: string;
  currentValueStr: string;
  nextValueStr: string;
}
