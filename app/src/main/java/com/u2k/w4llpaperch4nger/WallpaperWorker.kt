package com.u2k.w4llpaperch4nger

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.DisplayMetrics
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class WallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val folderUriString = ImageStorage.getFolder(applicationContext).first()
                ?: return Result.failure()

            val folderUri = Uri.parse(folderUriString)
            val images = listImagesInFolder(applicationContext, folderUri)

            if (images.isEmpty()) {
                return Result.failure()
            }

            val chosenImage = images.random()

            val inputStream = applicationContext.contentResolver.openInputStream(chosenImage.uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val displayMetrics = DisplayMetrics()
            val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val fittedBitmap = fitImageToScreen(
                originalBitmap,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )

            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            wallpaperManager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_LOCK)

            ImageStorage.saveLastUpdated(applicationContext, System.currentTimeMillis())

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}