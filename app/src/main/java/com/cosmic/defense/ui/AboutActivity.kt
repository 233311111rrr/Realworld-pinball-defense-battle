package com.cosmic.defense.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(6, 8, 20))
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
        }

        val tvTitle = TextView(this).apply {
            text = "ℹ️ 關於與素材授權聲明"
            textSize = 24f
            isFakeBoldText = true
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 30)
        }
        rootLayout.addView(tvTitle)

        val content = """
            【遊戲簡介】
            《太空彈球防衛戰》(Cosmic Ball Defense) 是一款基於 Android 原生 Kotlin、minSdk 26、targetSdk 34 開發的手機直式太空彈球塔防遊戲。
            採用 SurfaceView + Canvas 自繪架構，零 WebView、零第三方大型遊戲引擎依賴，支援 30~185Hz 螢幕高更新率與 240Hz 固定時間步長物理模擬。

            【素材來源與 CC0 授權】
            本遊戲嚴格遵守無外鏈、無侵權原則，所有圖像均使用公共領域 (CC0 1.0 Universal) 授權素材，並由遊戲引擎程序化高精細 Canvas 向量渲染：
            1. 外星章魚怪 - CC0 Procedural Canvas Vector
            2. 異星獨眼獸 - CC0 Procedural Canvas Vector
            3. 機械裝甲無人機 - CC0 Procedural Canvas Vector
            4. 金屬巡航戰艇 - CC0 Procedural Canvas Vector
            5. 第10排毀滅領主 - CC0 Procedural Canvas Vector
            6. 晶化脈衝寶箱 - CC0 Procedural Canvas Vector
            
            素材參考源自 OpenGameArt.org、Kenney.nl 與 itch.io CC0 專區，本地資源備份於 app/src/main/assets/monsters/ 目錄。
            詳情請參閱專案根目錄之 ASSETS_LICENSE.md。

            【開發架構】
            - 遊戲核心迴圈：GameLoop (Fixed Timestep 240Hz)
            - 實體與物件池：BallPool (150顆上限)、DamageTextPool、ParticlePool
            - 怪物路線：8排蛇形行軍與第一排追趕急行軍演算法
            - 構建流水線：GitHub Actions (.github/workflows/build.yml)
        """.trimIndent()

        val tvContent = TextView(this).apply {
            text = content
            textSize = 14f
            setTextColor(Color.rgb(203, 213, 225))
            setLineSpacing(12f, 1f)
            setPadding(0, 0, 0, 40)
        }
        rootLayout.addView(tvContent)

        val btnBack = Button(this).apply {
            text = "返回"
            setOnClickListener { finish() }
        }
        rootLayout.addView(btnBack)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }
}
