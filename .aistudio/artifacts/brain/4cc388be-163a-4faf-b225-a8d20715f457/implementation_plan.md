# 太空彈球防衛戰 (Cosmic Ball Defense) - Android 原生手遊與網頁即時試玩專案架構計畫

本計畫旨在構建一款以 **Kotlin + Android 原生 SurfaceView + Canvas 自繪** 為核心的太空題材直式彈球塔防遊戲（minSdk 26、targetSdk 34，零第三方遊戲引擎與 WebView），並配備 GitHub Actions 自動打包 debug APK 與 Release 發布工作流程；同時在 AI Studio 預覽環境中提供 **1:1 邏輯對齊的 Web Canvas 即時可玩模擬器**，便於立即測試所有彈道、怪物蛇行、數值升級與抽卡機制。

---

## User Review & Critical Decisions

> [!IMPORTANT]
> 根據第一階段問答確認事項：
> 1. **全套 Android 原始碼專案 + 網頁即時試玩環境**：在 AI Studio 內維持 React + HTML5 Canvas 模擬試玩，同時在專案中建立完整且規範的 Android 專案目錄結構（含 Gradle Wrapper、.github/workflows/build.yml 等）。
> 2. **向量程序化 Canvas 高精細繪圖**：所有怪物（章魚怪、獨眼獸、外星戰機、重裝無人機、第10排巨型首領）、戰機、寶箱與特效皆採用精緻自繪 Canvas 幾何路徑與色彩梯度，具備完美縮放能力與 0 外部網路依賴。
> 3. **高張力 8 排蛇行路徑**：總排數固定為 8 排（偶數排），怪物第 1 排自右向左、第 2 排自左向右循環前進，終點準確落在戰場右下角。

- **Confirmed Decision 1**: 採用 8 排蛇行路徑，第一排剩餘 $\le 50\%$ 怪物時啟動追趕急行軍機制，直至新怪填補至第 2 排中間後恢復等速。
- **Confirmed Decision 2**: 遊戲邏輯與繪圖分離，Android 端採用 240Hz 固定時間步長（Fixed Timestep）物理迴圈，支援 30/60/90/120/165/185 FPS 自適應更新。
- **Confirmed Decision 3**: 專案所有數值與平衡參數集中於 `GameConfig.kt`（及 Web 端對應的 `GameConfig.ts`），具備繁體中文註釋與明確物理單位。

---

## 1. Overview & Core Concept

### 核心玩法
玩家操控停駐於螢幕最下方的太空戰機，以單指滑動或點擊發射自由彈跳的光能彈球。怪物與隨機寶箱排成一列一列如貪食蛇般在星空戰場中向下蜿蜒。小球在左、右、上牆壁及怪物間持續反彈並造成傷害，越過底部邊界則消散回收。
擊破寶箱時將觸發技能三選一抽取（發球加速、雷射副砲、技能增強、球傷增強、導引飛彈、技能冷卻加速），搭配全場冰凍、炸藥清屏、雙倍傷害三大主動道具，在怪物抵達終點前守護星際防線。

### 目標受眾與體驗
追求爽快解壓、彈珠連擊與 Rogue-lite 局內構築樂趣的休閒遊戲玩家。介面提供流暢的高幀率顯示、無多餘延遲的觸發回饋、數字跳動與粒子碎裂效果。

---

## 2. User Experience & Visual Design

### 畫面配置（直式約 9:19.5，基準邏輯解析度 1080 × 2340）

```
┌──────────────────────────────────────────────┐
│  [HUD] 金幣: 1,420 │ 分數: 18.5K │ 最佳: 92.4K │ [暫停] │
│  球傷: 150 (+50%)   │ 技能傷害: 500 (+100%)    │ [FPS: 60]│
├──────────────────────────────────────────────┤
│                                              │
│  [中間戰場 - 8 排蛇形怪物移動與彈球反彈區]      │
│  → [第1排: 右往左] ──┐                        │
│  ┌── [第2排: 左往右] ←┘                        │
│  │                                           │
│  └── 怪物頭頂浮動血量 (如 72.8K) / 隨機寶箱   │
│      碰牆反彈輔助線 (最多兩段)               │
│      彈球物理碰撞 (上限 150 顆, 8s / 30 次)   │
│                                              │
│                                       [終點] │
├──────────────────────────────────────────────┤
│  [雷射槽 1] [雷射槽 2]    ▲    [雷射槽 3] [雷射槽 4]│
│                        [戰機]                │
├──────────────────────────────────────────────┤
│  [1x/2x/5x 倍速]   [❄️ 冰凍:3] [💣 炸藥:3] [⚡ 雙倍:3] │
└──────────────────────────────────────────────┘
```

### 視覺風格與調色盤 (60-30-10)
- **60% 宇宙底色**：深邃星空墨黑（`#060814`）帶有層次感的多層星星閃爍與緩慢漂移星雲。
- **30% 戰場結構與實體**：
  - 玩家戰機：鈦白光澤與微藍推進光焰（`#E0F2FE`）。
  - 怪物外觀：章魚怪（深紫紅霓光 `#E11D48`）、獨眼獸（異星碧綠 `#10B981`）、裝甲無人機（青空電弧藍 `#0284C7`）、外星巡航艇（暗金琥珀 `#D97706`）、第10排 Boss（霸氣赤焰紅金 `#DC2626` / `#F59E0B`）。
  - 寶箱：晶化合金箱體與脈衝金光（`#FBBF24`）。
- **10% 高光與動作回饋**：
  - 彈球：高能亮黃核心（`#FEF08A`）帶微藍光暈。
  - 雷射副砲：高能貫穿貫頂青藍光柱（`#38BDF8`）。
  - 導引飛彈：帶有煙霧拖尾的追蹤紅外飛彈與鎖定準星（`#F43F5E`）。
  - 飄字特效：暴擊金色、普通傷害白字、治療/獎勵翠綠。

---

## 3. Key Product Decisions & Trade-Offs

### 決定 1：雙環境並行（純原生 Android Gradle 專案 + AI Studio 即時 Web 模擬器）
- **做法**：在根目錄構建標準 Android 專案結構（包含 `settings.gradle.kts`、`build.gradle.kts`、`app/build.gradle.kts`、`AndroidManifest.xml`、完整的 `src/main/java/com/cosmic/defense/...` 所有 Kotlin 檔、assets、資源與 `.github/workflows/build.yml`），同時於 Vite 專案的 `src/` 中提供同等規格的 HTML5 Canvas 引擎，讓使用者在瀏覽器中即開即玩，不需要先下載 APK 才能體驗。
- **優勢**：滿足用戶對 GitHub Actions 打包 APK 的完整原始碼需求，同時解決 AI Studio 網頁預覽無法直接運行 Android Dalvik/ART 虛擬機的痛點。

### 決定 2：物件池架構（Object Pooling）杜絕 GC 卡頓
- **做法**：球體（`Ball`）、傷害跳字（`DamageText`）、粒子（`Particle`）預先分配固定容量物件池，回收時僅重設 `active = false` 與座標，零即時記憶體配置。
- **優勢**：在 Android 120Hz/185Hz 螢幕與繁重彈幕下維持絕對零卡頓（Zero GC Spikes）。

### 決定 3：固定邏輯步長 (Fixed Timestep 240Hz)
- **做法**：物理模擬採用每秒 240 次步長（dt = 1/240s = 4.16ms），渲染則透過 `requestAnimationFrame` 或 SurfaceView `Choreographer` 依螢幕模式刷新（30~185Hz），倍速（1x/2x/5x）直接縮放邏輯更新次數。
- **優勢**：確保高速彈射與倍速推進時，碰撞檢測不穿牆、彈道軌跡絕對確定一致。

---

## 4. Technical Architecture & Data Strategy

### 系統模組架構圖

```
┌────────────────────────────────────────────────────────┐
│                      GameEngine                        │
│  (Fixed Timestep Loop @ 240Hz, Speed: 1x / 2x / 5x)     │
└────────────┬─────────────────────────────┬─────────────┘
             │                             │
    [Input & Aiming]               [Update State]
  (Touch / Trajectory)                     │
             │                             ▼
             │                  ┌──────────────────────┐
             │                  │     WorldState       │
             │                  │  - Snake Path (8 Row)│
             │                  │  - Rush Catch-up     │
             │                  │  - Boss Spawn Wave   │
             │                  └──────────┬───────────┘
             │                             │
             ▼                             ▼
┌────────────────────────┐      ┌──────────────────────┐
│     PlayerCannon       │      │   EntityManager      │
│  - Active Ball Spawner │◄────►│  - BallPool (max 150)│
│  - Laser Slots (1~4)   │      │  - MonsterSnake List │
│  - Homing Missiles     │      │  - Particle / Popups │
└────────────┬───────────┘      └──────────┬───────────┘
             │                             │
             └──────────────┬──────────────┘
                            │
                            ▼
              ┌───────────────────────────┐
              │   Skill & Reward System   │
              │  - Chest Loot Lottery     │
              │  - Perm Upgrades (Damage/ │
              │    Luck/Speed Unlock)     │
              │  - Item Deck (Freeze/Bomb)│
              └─────────────┬─────────────┘
                            │
                            ▼
              ┌───────────────────────────┐
              │    Renderer (Canvas)      │
              │  - Starfield Sky Backdrop │
              │  - Snake Monsters & HP    │
              │  - Laser Beam & Trails    │
              │  - HUD, Dialogs, Modals   │
              └───────────────────────────┘
```

### 資料模型與持久化
- `GameConfig.kt` / `GameConfig.ts`：所有數值與平衡參數集中處（包含發射週期、球體生命、反彈次數、怪物血量成長率、寶箱機率、幸運機率計算公式、升級成本、倍速售價等）。
- `DataManager`：儲存累計金幣、球傷害等級、幸運等級、最佳分數、已解鎖倍速檔位（2x、5x）、復活券數量、音效/音樂/震動設定。

### 怪物蛇行排數與急行軍演算法
- 網格為 8 排，怪物依序生成並向當前排方向前進，到達邊界時轉向下一排。
- 檢查第 1 排存活怪物數量：當 $N_{\text{row1}} \le \frac{N_{\text{max}}}{2}$ 時觸發急行軍標記 `isCatchingUp = true`，整體移動速度乘上 `catchUpSpeedMultiplier`（例如 2.5x），直到第 2 排中間怪物生成定位後復原。

### 抽獎與技能加成管線
- 擊破寶箱後暫停時間，呼叫 `SkillLottery`。
- 依照目前幸運等級計算星級權重（1星、2星、3星），隨機抽出三張技能卡。
- 支援「單選領取」、「200 金幣全選」、「50 金幣重選」。
- 多個寶箱擊破時將獎勵壓入佇列（Queue），結算後自動彈出下一次抽獎。

---

## 5. Verification & Delivery Plan

1. **實作 Web 版即時遊玩環境**：
   - 包含 HUD、8 排蛇行怪物、彈珠射擊與兩段預測線、雷射副砲槽、飛彈鎖定、抽卡三選一視窗、主動道具、暫停/結算/復活彈窗及主畫面升級介面。
2. **完整輸出 Android 專案源碼**：
   - `build.gradle.kts` (根目錄)、`settings.gradle.kts`
   - `app/build.gradle.kts` (minSdk 26, targetSdk 34, AndroidX, DataStore)
   - `app/src/main/AndroidManifest.xml` (全螢幕、直式鎖定、硬體加速)
   - `GameConfig.kt`、`GameView.kt` (SurfaceView + 固定時間步長執行緒)、`GameActivity.kt`
   - 各模組 Kotlin 原始檔（`Entities`、`Skills`、`Audio`、`Data` 等）
   - `.github/workflows/build.yml` (CI/CD 構建 APK、上傳 artifact、建立 GitHub Release 標籤發布)
3. **驗證與編譯檢查**：
   - 執行 `compile_applet` 確保 Web 預覽無語法錯誤、零死循環，可在瀏覽器完整操作並顯示 FPS。
