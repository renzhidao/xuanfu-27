
package com.screenmask

data class Rule(
    val id: Long,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val color: Int,
    val enabled: Boolean,
    // 渲染模式：0=局部纯色块，1=全屏画布涂抹（仅在该区域涂）
    val mode: Int = MODE_BLOCK
) {
    companion object {
        const val MODE_BLOCK = 0
        const val MODE_CANVAS = 1
    }
}