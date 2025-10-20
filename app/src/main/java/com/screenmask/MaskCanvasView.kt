
package com.screenmask

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class MaskCanvasView(context: Context, colorRects: List<Rule>) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rects = colorRects.map {
        RectPack(it.left, it.top, it.right, it.bottom, it.color)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rects.forEach { r ->
            paint.color = r.color
            canvas.drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), paint)
        }
    }

    private data class RectPack(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val color: Int
    )
}