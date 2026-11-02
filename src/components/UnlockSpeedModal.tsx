import React from 'react';
import { FastForward } from 'lucide-react';

interface UnlockSpeedModalProps {
  isOpen: boolean;
  onClose: () => void;
  speed: number;
  cost: number;
  currentCoins: number;
  onConfirmUnlock: () => void;
}

export const UnlockSpeedModal: React.FC<UnlockSpeedModalProps> = ({
  isOpen,
  onClose,
  speed,
  cost,
  currentCoins,
  onConfirmUnlock,
}) => {
  if (!isOpen) return null;

  const canAfford = currentCoins >= cost;

  return (
    <div className="absolute inset-0 z-50 bg-black/85 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[380px] w-full text-center text-slate-100">
        <div className="w-12 h-12 bg-sky-950/60 border border-sky-800 rounded-full flex items-center justify-center mx-auto mb-3 text-sky-400">
          <FastForward className="w-6 h-6" />
        </div>
        <h3 className="text-lg font-bold text-white mb-1">永久解鎖 {speed}x 戰鬥倍速</h3>
        <p className="text-xs text-slate-400 leading-relaxed mb-4">
          解鎖後永久生效，可在戰鬥中隨時以 {speed} 倍速度極速作戰。<br />
          花費: <strong className="text-amber-400 font-bold">{cost} 金幣</strong>（目前持有: {currentCoins}）
        </p>

        <div className="flex items-center gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition-colors"
          >
            取消
          </button>
          <button
            disabled={!canAfford}
            onClick={() => {
              onConfirmUnlock();
              onClose();
            }}
            className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition-colors ${
              canAfford
                ? 'bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-md shadow-amber-500/20'
                : 'bg-slate-800 text-slate-500 cursor-not-allowed'
            }`}
          >
            {canAfford ? '確認解鎖' : '金幣不足'}
          </button>
        </div>
      </div>
    </div>
  );
};
