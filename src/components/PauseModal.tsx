import React from 'react';
import { Play, Volume2, VolumeX, RotateCcw } from 'lucide-react';

interface PauseModalProps {
  isOpen: boolean;
  onClose: () => void;
  soundEnabled: boolean;
  onToggleSound: () => void;
  onRestart: () => void;
}

export const PauseModal: React.FC<PauseModalProps> = ({
  isOpen,
  onClose,
  soundEnabled,
  onToggleSound,
  onRestart,
}) => {
  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-50 bg-black/85 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[380px] w-full text-center text-slate-100">
        <h3 className="text-xl font-bold text-white mb-2">遊戲暫停</h3>
        <p className="text-xs text-slate-400 mb-6">星際戰機防禦整備中</p>

        <div className="flex flex-col gap-3">
          <button
            onClick={onClose}
            className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-colors"
          >
            <Play className="w-4 h-4" />
            <span>繼續防守</span>
          </button>

          <button
            onClick={onToggleSound}
            className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-medium flex items-center justify-center gap-2 transition-colors"
          >
            {soundEnabled ? <Volume2 className="w-4 h-4 text-emerald-400" /> : <VolumeX className="w-4 h-4 text-slate-500" />}
            <span>音效: {soundEnabled ? '開啟' : '靜音'}</span>
          </button>

          <button
            onClick={onRestart}
            className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-medium flex items-center justify-center gap-2 transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            <span>重新開始戰役</span>
          </button>
        </div>
      </div>
    </div>
  );
};
