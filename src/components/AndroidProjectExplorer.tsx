import React, { useState } from 'react';
import { Copy, Check, FileCode, Terminal, Sparkles } from 'lucide-react';

interface AndroidFileItem {
  name: string;
  path: string;
  desc: string;
  language: string;
}

const ANDROID_FILES: AndroidFileItem[] = [
  { name: 'build.yml (GitHub Actions)', path: '.github/workflows/build.yml', desc: 'CI/CD 自動打包 debug APK 與 Release 上傳', language: 'yaml' },
  { name: 'GameConfig.kt', path: 'app/src/main/java/com/cosmic/defense/config/GameConfig.kt', desc: '數值集中管理（含各項單位與用途）', language: 'kotlin' },
  { name: 'GameView.kt (SurfaceView)', path: 'app/src/main/java/com/cosmic/defense/engine/GameView.kt', desc: '自繪畫布、1080x2340 縮放與 HUD 渲染', language: 'kotlin' },
  { name: 'GameLoop.kt', path: 'app/src/main/java/com/cosmic/defense/engine/GameLoop.kt', desc: '240Hz 固定時間步長物理迴圈與幀率分離', language: 'kotlin' },
  { name: 'SnakeGrid.kt', path: 'app/src/main/java/com/cosmic/defense/world/SnakeGrid.kt', desc: '8 排偶數蛇形路徑、右下角終點與急行軍機制', language: 'kotlin' },
  { name: 'World.kt', path: 'app/src/main/java/com/cosmic/defense/world/World.kt', desc: '主戰場、反彈物理、兩段瞄準線、副砲與飛彈', language: 'kotlin' },
  { name: 'Monster.kt', path: 'app/src/main/java/com/cosmic/defense/entities/Monster.kt', desc: '章魚怪/獨眼/無人機/領主/寶箱自繪與血量 K/M', language: 'kotlin' },
  { name: 'BallPool.kt', path: 'app/src/main/java/com/cosmic/defense/entities/BallPool.kt', desc: '150 顆彈球物件池、超額自動回收最舊球', language: 'kotlin' },
  { name: 'SkillManager.kt', path: 'app/src/main/java/com/cosmic/defense/skills/SkillManager.kt', desc: '寶箱技能三選一、幸運機率、雷射砲與飛彈累加', language: 'kotlin' },
  { name: 'GameActivity.kt', path: 'app/src/main/java/com/cosmic/defense/ui/GameActivity.kt', desc: '全螢幕沉浸、30~185Hz 模式適配、抽卡與復活彈窗', language: 'kotlin' },
  { name: 'MainActivity.kt', path: 'app/src/main/java/com/cosmic/defense/ui/MainActivity.kt', desc: '主畫面、金幣、球傷害升級與幸運升級面板', language: 'kotlin' },
  { name: 'SettingsActivity.kt', path: 'app/src/main/java/com/cosmic/defense/ui/SettingsActivity.kt', desc: '幀率切換、音效/音樂/震動/持續射擊設定', language: 'kotlin' },
  { name: 'AndroidManifest.xml', path: 'app/src/main/AndroidManifest.xml', desc: '直式鎖定、硬體加速、全螢幕與震動權限', language: 'xml' },
  { name: 'app/build.gradle.kts', path: 'app/build.gradle.kts', desc: 'minSdk 26, targetSdk 34, AndroidX 配置', language: 'kotlin' },
  { name: 'settings.gradle.kts', path: 'settings.gradle.kts', desc: 'Gradle 依賴倉庫與模組宣告', language: 'kotlin' },
  { name: 'ASSETS_LICENSE.md', path: 'ASSETS_LICENSE.md', desc: 'CC0 1.0 授權素材來源與無外鏈聲明', language: 'markdown' },
];

export const AndroidProjectExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<AndroidFileItem>(ANDROID_FILES[0]);
  const [copied, setCopied] = useState(false);

  const handleCopyPath = () => {
    navigator.clipboard.writeText(selectedFile.path);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 p-6 overflow-y-auto w-full">
      <div className="mb-6 bg-slate-900 border border-slate-800 rounded-xl p-5">
        <div className="flex items-center gap-3 mb-2">
          <Terminal className="w-5 h-5 text-sky-400" />
          <h2 className="text-lg font-bold text-white">Android 原生工程架構 (Kotlin + Gradle + CI/CD)</h2>
        </div>
        <p className="text-sm text-slate-300 leading-relaxed">
          全套原生 Android 程式碼已完整輸出至專案根目錄，符合 <strong>minSdk 26</strong>、<strong>targetSdk 34</strong>、直式鎖定與 SurfaceView Canvas 自繪架構。
          可直接上傳或 push 至 GitHub，並透過 GitHub Actions 自動建置出 <code>app-debug.apk</code>。
        </p>
      </div>

      <div className="mb-6 bg-sky-950/40 border border-sky-800/60 rounded-xl p-5">
        <div className="flex items-center gap-2 mb-2 text-sky-300 font-semibold text-sm">
          <Sparkles className="w-4 h-4 text-sky-400" />
          <span>GitHub Actions 打包與 Release 發布教學</span>
        </div>
        <ol className="list-decimal list-inside text-xs text-slate-300 space-y-1.5 leading-relaxed">
          <li>建立新的 GitHub 儲存庫並將此目錄程式碼 push 至 <code>main</code> 分支。</li>
          <li>進入儲存庫的 <strong>Actions</strong> 分頁，工作流程 <code>Build & Release Android APK</code> 會自動觸發執行。</li>
          <li>建置完成後點進該次 Run，最下方 Artifacts 即可直接下載 <strong>cosmic-ball-defense-debug-apk</strong>。</li>
          <li>建立版號標籤（例如 <code>git tag v1.0.0 && git push origin v1.0.0</code>），Actions 會自動建立 GitHub Release 並附上該 APK。</li>
        </ol>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 flex-1 min-h-[460px]">
        <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-3 flex flex-col gap-1.5 overflow-y-auto max-h-[520px]">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider px-2 py-1">專案檔案導覽</span>
          {ANDROID_FILES.map((f) => (
            <button
              key={f.path}
              onClick={() => setSelectedFile(f)}
              className={`flex flex-col text-left px-3 py-2.5 rounded-lg text-xs transition-colors ${
                selectedFile.path === f.path
                  ? 'bg-sky-600 text-white font-medium shadow-sm'
                  : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
              }`}
            >
              <div className="flex items-center gap-2">
                <FileCode className="w-3.5 h-3.5 shrink-0 opacity-80" />
                <span className="truncate">{f.name}</span>
              </div>
              <span className={`text-[10px] mt-0.5 truncate ${selectedFile.path === f.path ? 'text-sky-100' : 'text-slate-400'}`}>
                {f.desc}
              </span>
            </button>
          ))}
        </div>

        <div className="md:col-span-2 bg-slate-900/90 border border-slate-800 rounded-xl p-5 flex flex-col">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-4">
            <div>
              <h3 className="text-base font-bold text-white">{selectedFile.name}</h3>
              <p className="text-xs text-sky-400 font-mono mt-0.5">{selectedFile.path}</p>
            </div>
            <button
              onClick={handleCopyPath}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? '已複製路徑' : '複製路徑'}</span>
            </button>
          </div>

          <div className="flex-1 bg-slate-950 rounded-lg p-4 font-mono text-xs text-slate-300 overflow-y-auto border border-slate-800/80 leading-relaxed">
            <p className="text-slate-400 mb-2"># 檔案位置: {selectedFile.path}</p>
            <p className="text-slate-400 mb-4"># 功能簡介: {selectedFile.desc}</p>
            <div className="text-sky-300 bg-slate-900/60 p-3 rounded border border-slate-800 text-[11px]">
              ✔ 此檔案已完整實作於當前工作區檔案系統中。<br />
              ✔ 支援 240Hz 固定物理時間步長、物件池與全繁體中文註解。<br />
              ✔ 配合 GitHub Actions CI，免在本機安裝 Android Studio 即可線上一鍵打包 APK！
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
