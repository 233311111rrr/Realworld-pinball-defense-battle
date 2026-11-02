import React, { useState, useEffect } from 'react';
import { GameCanvas } from './components/GameCanvas';
import { AndroidProjectExplorer } from './components/AndroidProjectExplorer';
import { UpgradeDrawer } from './components/UpgradeDrawer';
import { SettingsModal } from './components/SettingsModal';
import { PauseModal } from './components/PauseModal';
import { UnlockSpeedModal } from './components/UnlockSpeedModal';
import { GameConfig } from './GameConfig';
import { Gamepad2, FileCode2, Sparkles, Settings as SettingsIcon } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'PLAY' | 'ANDROID_PROJECT'>('PLAY');

  // 持久化資料 (localStorage)
  const [coins, setCoins] = useState<number>(() => {
    const saved = localStorage.getItem('cosmic_coins');
    return saved ? parseInt(saved, 10) : 500;
  });

  const [bestScore, setBestScore] = useState<number>(() => {
    const saved = localStorage.getItem('cosmic_best_score');
    return saved ? parseInt(saved, 10) : 0;
  });

  const [ballDamageLevel, setBallDamageLevel] = useState<number>(() => {
    const saved = localStorage.getItem('cosmic_ball_dmg_lvl');
    return saved ? parseInt(saved, 10) : 0;
  });

  const [luckLevel, setLuckLevel] = useState<number>(() => {
    const saved = localStorage.getItem('cosmic_luck_lvl');
    return saved ? parseInt(saved, 10) : 0;
  });

  const [is2xUnlocked, setIs2xUnlocked] = useState<boolean>(() => {
    return localStorage.getItem('cosmic_2x_unlocked') === 'true';
  });

  const [is5xUnlocked, setIs5xUnlocked] = useState<boolean>(() => {
    return localStorage.getItem('cosmic_5x_unlocked') === 'true';
  });

  const [reviveTickets, setReviveTickets] = useState<number>(() => {
    const saved = localStorage.getItem('cosmic_revive_tickets');
    return saved ? parseInt(saved, 10) : 2;
  });

  const [targetFps, setTargetFps] = useState<number>(60);
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [continuousShoot, setContinuousShoot] = useState<boolean>(true);
  const [showFps, setShowFps] = useState<boolean>(true);

  // 模態視窗
  const [isUpgradeOpen, setIsUpgradeOpen] = useState<boolean>(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);
  const [isPauseOpen, setIsPauseOpen] = useState<boolean>(false);
  const [unlockSpeedTarget, setUnlockSpeedTarget] = useState<{ speed: number; cost: number } | null>(null);

  // 儲存狀態至 localStorage
  useEffect(() => {
    localStorage.setItem('cosmic_coins', coins.toString());
  }, [coins]);

  useEffect(() => {
    localStorage.setItem('cosmic_best_score', bestScore.toString());
  }, [bestScore]);

  useEffect(() => {
    localStorage.setItem('cosmic_ball_dmg_lvl', ballDamageLevel.toString());
  }, [ballDamageLevel]);

  useEffect(() => {
    localStorage.setItem('cosmic_luck_lvl', luckLevel.toString());
  }, [luckLevel]);

  useEffect(() => {
    localStorage.setItem('cosmic_2x_unlocked', is2xUnlocked.toString());
  }, [is2xUnlocked]);

  useEffect(() => {
    localStorage.setItem('cosmic_5x_unlocked', is5xUnlocked.toString());
  }, [is5xUnlocked]);

  useEffect(() => {
    localStorage.setItem('cosmic_revive_tickets', reviveTickets.toString());
  }, [reviveTickets]);

  // 升級球傷害
  const handleUpgradeBallDamage = () => {
    const cost = Math.round(
      GameConfig.UPGRADE_BALL_DMG_BASE_COST * (1 + ballDamageLevel * GameConfig.UPGRADE_BALL_DMG_COST_FACTOR)
    );
    if (coins >= cost) {
      setCoins((c) => c - cost);
      setBallDamageLevel((l) => l + 1);
    }
  };

  // 升級幸運
  const handleUpgradeLuck = () => {
    if (luckLevel >= GameConfig.LUCK_MAX_LEVEL) return;
    const cost = GameConfig.UPGRADE_LUCK_BASE_COST * (luckLevel + 1);
    if (coins >= cost) {
      setCoins((c) => c - cost);
      setLuckLevel((l) => l + 1);
    }
  };

  // 解鎖倍速確認
  const handleConfirmUnlockSpeed = () => {
    if (!unlockSpeedTarget) return;
    const { speed, cost } = unlockSpeedTarget;
    if (coins >= cost) {
      setCoins((c) => c - cost);
      if (speed === 2) setIs2xUnlocked(true);
      if (speed === 5) setIs5xUnlocked(true);
    }
    setUnlockSpeedTarget(null);
  };

  return (
    <div className="flex flex-col w-screen h-screen bg-[#060814] text-slate-100 overflow-hidden font-sans">
      {/* 頂部導航列 (Top Bar Contract: 3 區域) */}
      <header className="flex items-center justify-between gap-8 px-6 py-3 border-b border-slate-800/80 bg-slate-950/80 shrink-0 z-20">
        {/* 區域 1: 品牌標題 */}
        <div className="flex items-center gap-2 whitespace-nowrap shrink-0">
          <span className="text-base font-bold tracking-tight text-white">太空彈球防衛戰</span>
          <span className="text-[11px] text-sky-400 font-mono hidden sm:inline">COSMIC BALL DEFENSE</span>
        </div>

        {/* 區域 2: 檢視模式切換 */}
        <nav className="flex items-center gap-1.5 p-1 bg-slate-900 rounded-lg border border-slate-800">
          <button
            onClick={() => setActiveTab('PLAY')}
            className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
              activeTab === 'PLAY'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Gamepad2 className="w-3.5 h-3.5" />
            <span>即時試玩 (Web Canvas)</span>
          </button>
          <button
            onClick={() => setActiveTab('ANDROID_PROJECT')}
            className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
              activeTab === 'ANDROID_PROJECT'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <FileCode2 className="w-3.5 h-3.5" />
            <span>Android 原生工程與 CI/CD</span>
          </button>
        </nav>

        {/* 區域 3: 輔助操作按鈕 */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => setIsUpgradeOpen(true)}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium bg-amber-500/20 text-amber-300 border border-amber-500/40 rounded-lg hover:bg-amber-500/30 transition-colors whitespace-nowrap shrink-0"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>戰力養成</span>
          </button>
          <button
            onClick={() => setIsSettingsOpen(true)}
            className="p-1.5 text-slate-400 hover:text-white bg-slate-900 border border-slate-800 rounded-lg transition-colors whitespace-nowrap shrink-0"
            title="系統設定"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* 主體展示區 */}
      <main className="flex-1 relative overflow-hidden flex items-center justify-center">
        {activeTab === 'PLAY' ? (
          <GameCanvas
            coins={coins}
            bestScore={bestScore}
            ballDamageLevel={ballDamageLevel}
            luckLevel={luckLevel}
            is2xUnlocked={is2xUnlocked}
            is5xUnlocked={is5xUnlocked}
            reviveTickets={reviveTickets}
            targetFps={targetFps}
            soundEnabled={soundEnabled}
            continuousShoot={continuousShoot}
            showFps={showFps}
            onCoinsChange={setCoins}
            onBestScoreChange={setBestScore}
            onReviveTicketsChange={setReviveTickets}
            onRequestUnlockSpeed={(speed, cost) => setUnlockSpeedTarget({ speed, cost })}
            onOpenPause={() => setIsPauseOpen(true)}
            onOpenUpgrades={() => setIsUpgradeOpen(true)}
          />
        ) : (
          <AndroidProjectExplorer />
        )}
      </main>

      {/* 全域彈窗 */}
      <UpgradeDrawer
        isOpen={isUpgradeOpen}
        onClose={() => setIsUpgradeOpen(false)}
        coins={coins}
        ballDamageLevel={ballDamageLevel}
        luckLevel={luckLevel}
        onUpgradeBallDamage={handleUpgradeBallDamage}
        onUpgradeLuck={handleUpgradeLuck}
      />

      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        targetFps={targetFps}
        onTargetFpsChange={setTargetFps}
        soundEnabled={soundEnabled}
        onSoundChange={setSoundEnabled}
        continuousShoot={continuousShoot}
        onContinuousShootChange={setContinuousShoot}
        showFps={showFps}
        onShowFpsChange={setShowFps}
      />

      <PauseModal
        isOpen={isPauseOpen}
        onClose={() => setIsPauseOpen(false)}
        soundEnabled={soundEnabled}
        onToggleSound={() => setSoundEnabled((s) => !s)}
        onRestart={() => {
          setIsPauseOpen(false);
          window.location.reload();
        }}
      />

      <UnlockSpeedModal
        isOpen={unlockSpeedTarget !== null}
        onClose={() => setUnlockSpeedTarget(null)}
        speed={unlockSpeedTarget?.speed ?? 2}
        cost={unlockSpeedTarget?.cost ?? 1000}
        currentCoins={coins}
        onConfirmUnlock={handleConfirmUnlockSpeed}
      />
    </div>
  );
}
