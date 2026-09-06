package com.u2k.w4llpaperch4nger

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

// 画像を「画面に収まるよう余白付きで縮小配置」した新しいBitmapを作る関数
fun fitImageToScreen(original: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap {
    val result = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(Color.BLACK)

    val scale = minOf(
        screenWidth.toFloat() / original.width,
        screenHeight.toFloat() / original.height
    )

    val scaledWidth = (original.width * scale).toInt()
    val scaledHeight = (original.height * scale).toInt()

    val left = (screenWidth - scaledWidth) / 2f
    val top = (screenHeight - scaledHeight) / 2f

    val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
    canvas.drawBitmap(scaledBitmap, left, top, Paint(Paint.FILTER_BITMAP_FLAG))

    return result
}