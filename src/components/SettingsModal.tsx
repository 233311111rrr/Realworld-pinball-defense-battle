import React from 'react';
import { GameConfig } from '../GameConfig';
import { Settings, X, Check } from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetFps: number;
  onTargetFpsChange: (fps: number) => void;
  soundEnabled: boolean;
  onSoundChange: (val: boolean) => void;
  continuousShoot: boolean;
  onContinuousShootChange: (val: boolean) => void;
  showFps: boolean;
  onShowFpsChange: (val: boolean) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  targetFps,
  onTargetFpsChange,
  soundEnabled,
  onSoundChange,
  continuousShoot,
  onContinuousShootChange,
  showFps,
  onShowFpsChange,
}) => {
  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-[440px] w-full text-slate-100 flex flex-col max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-sky-400" />
            <h3 className="text-lg font-bold text-white">系統與畫面設定</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mb-5">
          <label className="text-xs font-semibold text-slate-300 mb-2 block">
            目標螢幕幀率 (物理 240Hz 步長分離)
          </label>
          <div className="grid grid-cols-3 gap-2">
            {GameConfig.SUPPORTED_FPS_OPTIONS.map((fps) => (
              <button
                key={fps}
                onClick={() => onTargetFpsChange(fps)}
                className={`py-2 px-3 text-xs font-bold rounded-lg border transition-colors flex items-center justify-between ${
                  targetFps === fps
                    ? 'bg-sky-600/30 border-sky-400 text-sky-200'
                    : 'bg-slate-950 border-slate-800 text-slate-300 hover:bg-slate-800'
                }`}
              >
                <span>{fps} FPS</span>
                {targetFps === fps && <Check className="w-3.5 h-3.5 text-sky-400" />}
              </button>
            ))}
          </div>
          <p className="text-[10px] text-slate-500 mt-1.5">
            Android 端會以 Window.setPreferredDisplayModeId 匹配硬體模式，不支援時自動降級。
          </p>
        </div>

        <div className="space-y-3 border-t border-slate-800 pt-4 text-xs">
          <label className="flex items-center justify-between cursor-pointer p-2 rounded-lg bg-slate-950 border border-slate-800/80">
            <span>🔊 遊戲音效與合成器</span>
            <input
              type="checkbox"
              checked={soundEnabled}
              onChange={(e) => onSoundChange(e.target.checked)}
              className="rounded accent-sky-500 w-4 h-4"
            />
          </label>

          <label className="flex items-center justify-between cursor-pointer p-2 rounded-lg bg-slate-950 border border-slate-800/80">
            <div>
              <span className="block font-medium">🎯 放開手指後持續射擊</span>
              <span className="text-[10px] text-slate-400">關閉後放開手指立即停止射球</span>
            </div>
            <input
              type="checkbox"
              checked={continuousShoot}
              onChange={(e) => onContinuousShootChange(e.target.checked)}
              className="rounded accent-sky-500 w-4 h-4"
            />
          </label>

          <label className="flex items-center justify-between cursor-pointer p-2 rounded-lg bg-slate-950 border border-slate-800/80">
            <span>📊 顯示即時 FPS 幀率</span>
            <input
              type="checkbox"
              checked={showFps}
              onChange={(e) => onShowFpsChange(e.target.checked)}
              className="rounded accent-sky-500 w-4 h-4"
            />
          </label>
        </div>

        <button
          onClick={onClose}
          className="mt-6 w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold transition-colors"
        >
          完成設定
        </button>
      </div>
    </div>
  );
};
