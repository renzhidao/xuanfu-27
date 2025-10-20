
package com.screenmask

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private val ruleManager by lazy { RuleManager(this) }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlays()
    }

    private fun createOverlays() {
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()

        val enabledRules = ruleManager.getRules().filter { it.enabled }
        val blockRules = enabledRules.filter { it.mode == Rule.MODE_BLOCK }
        val canvasRules = enabledRules.filter { it.mode == Rule.MODE_CANVAS }

        // 方案A：局部纯色块（每条规则一个小 View）
        blockRules.forEach { rule ->
            val view = View(this).apply {
                setBackgroundColor(rule.color)
                isClickable = false
                isLongClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            val params = WindowManager.LayoutParams(
                rule.right - rule.left,
                rule.bottom - rule.top,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = rule.left
                y = rule.top
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            windowManager.addView(view, params)
            overlayViews.add(view)
        }

        // 方案B：全屏画布涂抹（只在对应区域绘制）
        if (canvasRules.isNotEmpty()) {
            val view = MaskCanvasView(this, canvasRules).apply {
                isClickable = false
                isLongClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            windowManager.addView(view, params)
            overlayViews.add(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}