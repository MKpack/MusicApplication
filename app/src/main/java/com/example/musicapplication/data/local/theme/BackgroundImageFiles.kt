package com.example.musicapplication.data.local.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 自定义背景图存在 App 私有目录 files/backgrounds/ 里，只在本机，不上传。
 * 选好的图按屏幕的宽高比从中间裁好、缩到屏幕大小再压成 JPEG：显示时正好铺满屏幕，读得快，也不占多少空间。
 */
@Singleton
class BackgroundImageFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File
        get() = File(context.filesDir, "backgrounds")

    /** 把相册里选的图存一份到本地，返回新文件的路径。旧的背景图要等新路径保存好以后再用 [deleteAllExcept] 删 */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val metrics = context.resources.displayMetrics
        val screenWidth = min(metrics.widthPixels, metrics.heightPixels)
        val screenHeight = max(metrics.widthPixels, metrics.heightPixels)

        val source = ImageDecoder.createSource(context.contentResolver, uri)
        // 解码时就缩到刚好盖住屏幕的大小（不放大），照片的旋转方向 ImageDecoder 会自己处理
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val scale = min(
                1f,
                max(screenWidth.toFloat() / info.size.width, screenHeight.toFloat() / info.size.height)
            )
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1)
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        // 从中间裁成屏幕的宽高比
        val targetRatio = screenWidth.toFloat() / screenHeight
        val cropWidth = min(decoded.width, (decoded.height * targetRatio).roundToInt()).coerceAtLeast(1)
        val cropHeight = min(decoded.height, (decoded.width / targetRatio).roundToInt()).coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(
            decoded,
            (decoded.width - cropWidth) / 2,
            (decoded.height - cropHeight) / 2,
            cropWidth,
            cropHeight
        )

        dir.mkdirs()
        // 每次换图都用新文件名，图片缓存不会拿到上一张
        val file = File(dir, "background_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (cropped !== decoded) cropped.recycle()
        decoded.recycle()
        file.absolutePath
    }

    /** 删掉除 [keepPath] 以外的背景图；传 null 时全部删掉 */
    suspend fun deleteAllExcept(keepPath: String?) = withContext(Dispatchers.IO) {
        dir.listFiles()
            ?.filter { it.absolutePath != keepPath }
            ?.forEach { it.delete() }
        Unit
    }
}
