import React from 'react';
import { GameConfig } from '../GameConfig';
import { Sparkles, X, ChevronRight } from 'lucide-react';

interface UpgradeDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  coins: number;
  ballDamageLevel: number;
  luckLevel: number;
  onUpgradeBallDamage: () => void;
  onUpgradeLuck: () => void;
}

export const UpgradeDrawer: React.FC<UpgradeDrawerProps> = ({
  isOpen,
  onClose,
  coins,
  ballDamageLevel,
  luckLevel,
  onUpgradeBallDamage,
  onUpgradeLuck,
}) => {
  if (!isOpen) return null;

  const dmgCost = Math.round(
    GameConfig.UPGRADE_BALL_DMG_BASE_COST * (1 + ballDamageLevel * GameConfig.UPGRADE_BALL_DMG_COST_FACTOR)
  );
  const currentBaseDmg = Math.round(GameConfig.BASE_BALL_DAMAGE + ballDamageLevel * 15);

  const luckCost = GameConfig.UPGRADE_LUCK_BASE_COST * (luckLevel + 1);
  const p1 = Math.min(0.9, Math.max(0.1, GameConfig.LUCK_BASE_PROBABILITIES[0] + luckLevel * GameConfig.LUCK_PROB_STEP_PER_LEVEL[0]));
  const p2 = Math.min(0.6, Math.max(0.05, GameConfig.LUCK_BASE_PROBABILITIES[1] + luckLevel * GameConfig.LUCK_PROB_STEP_PER_LEVEL[1]));
  const p3 = Math.max(0.01, 1.0 - p1 - p2);

  return (
    <div className="absolute inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[460px] w-full text-slate-100 flex flex-col max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-amber-400" />
            <h3 className="text-lg font-bold text-white">永久戰力強化中心</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="bg-slate-950 p-3 rounded-xl mb-4 border border-slate-800/80 flex items-center justify-between text-xs">
          <span className="text-slate-400">目前持有金幣</span>
          <span className="text-amber-400 font-bold text-sm">💰 {coins}</span>
        </div>

        <div className="bg-slate-950/80 border border-slate-800 p-4 rounded-xl mb-4 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-bold text-white">基礎球傷害強化</span>
            <span className="text-xs text-sky-400 font-mono">Lv.{ballDamageLevel}</span>
          </div>
          <p className="text-xs text-slate-400">每級提升 +15 基礎彈球碰撞傷害。</p>
          <div className="flex items-center justify-between text-xs text-slate-300 mt-1">
            <span>目前傷害: {currentBaseDmg}</span>
            <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
            <span className="text-emerald-400 font-semibold">升級後: {currentBaseDmg + 15}</span>
          </div>
          <button
            disabled={coins < dmgCost}
            onClick={onUpgradeBallDamage}
            className={`mt-2 py-2 rounded-lg text-xs font-bold transition-colors ${
              coins >= dmgCost
                ? 'bg-sky-600 hover:bg-sky-500 text-white'
                : 'bg-slate-800 text-slate-500 cursor-not-allowed'
            }`}
          >
            升級 ({dmgCost} 金幣)
          </button>
        </div>

        <div className="bg-slate-950/80 border border-slate-800 p-4 rounded-xl flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-bold text-white">幸運星級加護</span>
            <span className="text-xs text-amber-400 font-mono">Lv.{luckLevel} / {GameConfig.LUCK_MAX_LEVEL}</span>
          </div>
          <p className="text-xs text-slate-400">提高寶箱抽獎 2、3 星卡機率，降低 1 星機率（上限 10 級）。</p>

          <div className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/80 text-[11px] space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-400">★☆☆ (1星):</span>
              <span className="text-white font-mono">{(p1 * 100).toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sky-300">★★☆ (2星):</span>
              <span className="text-sky-300 font-mono">{(p2 * 100).toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-amber-400">★★★ (3星):</span>
              <span className="text-amber-400 font-mono font-bold">{(p3 * 100).toFixed(1)}%</span>
            </div>
          </div>

          {luckLevel < GameConfig.LUCK_MAX_LEVEL ? (
            <button
              disabled={coins < luckCost}
              onClick={onUpgradeLuck}
              className={`mt-2 py-2 rounded-lg text-xs font-bold transition-colors ${
                coins >= luckCost
                  ? 'bg-amber-500 hover:bg-amber-400 text-slate-950'
                  : 'bg-slate-800 text-slate-500 cursor-not-allowed'
              }`}
            >
              提升幸運等級 ({luckCost} 金幣)
            </button>
          ) : (
            <div className="mt-2 text-center text-xs text-amber-400 font-semibold py-1.5 bg-slate-900 rounded-lg">
              ★ 幸運已達最高上限
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
