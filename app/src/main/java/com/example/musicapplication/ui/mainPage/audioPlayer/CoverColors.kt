package com.example.musicapplication.ui.mainPage.audioPlayer

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 从封面里挑出来的重点颜色，给播放页背景用。
 * [keyColors] 按重要程度排好，第一个最能代表这张封面；色相互不相同，固定 3 个。
 * 黑白灰的封面 [isMonochrome] 为 true，这时 [keyColors] 是几种灰。
 */
@Immutable
data class CoverColors(
    val keyColors: List<Color>,
    val isMonochrome: Boolean,
) {
    companion object {
        /** 还没取到颜色、没有封面时用：几种深灰 */
        val Default = CoverColors(
            keyColors = listOf(Color(0xFF48484A), Color(0xFF3A3A3C), Color(0xFF2C2C2E)),
            isMonochrome = true
        )
    }
}

/** 封面里的一种颜色：色相 0~360，饱和度、亮度 0~1，[population] 是这种颜色占了多少像素 */
internal data class CoverSwatch(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
    val population: Int,
)

// 色度（HSL 里的鲜艳程度，0 = 灰，1 = 最鲜艳）低于这个就算黑白灰，不参加挑选
private const val MinChroma = 0.1f

// 有颜色的部分加起来不到这么多，就当作黑白灰的封面
private const val MinChromaticShare = 0.03f

// 挑出来的几种颜色色相至少差这么多度，背景上才看得出是不同的颜色
private const val MinHueGap = 30f

// 算一种颜色的面积时，把色相相差不超过这么多度的颜色也算进去
private const val HueNeighborhood = 15f

// 颜色不够 3 种时，补上的颜色和第一种差这么多度
private const val CompanionHueShift = 18f

// 黑白灰封面的灰色最多带这么一点原来的色调
private const val MaxMonochromeTint = 0.06f

private data class Candidate(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
    val share: Float,
) {
    val chroma: Float
        get() = saturation * (1f - abs(2f * lightness - 1f))
}

/** 从封面（一张小图就够，128px）里挑重点颜色 */
fun extractCoverColors(bitmap: Bitmap): CoverColors {
    val swatches = Palette.from(bitmap)
        .maximumColorCount(24)
        // 默认的过滤会去掉接近黑白的颜色，还会去掉一段橙红色（肤色、暖色灯光常落在这里），自己来筛
        .clearFilters()
        .generate()
        .swatches
        .map { swatch ->
            val hsl = swatch.hsl
            CoverSwatch(hue = hsl[0], saturation = hsl[1], lightness = hsl[2], population = swatch.population)
        }
    return pickCoverColors(swatches)
}

/**
 * Palette 的 dominantSwatch 是面积最大的颜色，可很多封面面积最大的是白底、黑底或者灰色的背景，
 * 真正显眼的颜色（衣服、灯光、插画的主色）面积反而不大，所以原来经常取到灰扑扑的颜色。
 * 这里给每种颜色打分：面积占 6 成（色相相近的颜色面积合在一起算），鲜艳程度占 4 成；
 * 黑、白、灰不参加。然后挑出色相互相差够大的前 3 个。
 */
internal fun pickCoverColors(swatches: List<CoverSwatch>): CoverColors {
    val total = swatches.sumOf { it.population }.toFloat()
    if (total <= 0f) return CoverColors.Default

    val candidates = swatches.map {
        Candidate(hue = it.hue, saturation = it.saturation, lightness = it.lightness, share = it.population / total)
    }

    // 有颜色、不太黑也不太白、面积不是一丁点的颜色才参加
    val chromatic = candidates.filter {
        it.chroma >= MinChroma && it.lightness in 0.08f..0.95f && it.share >= 0.005f
    }
    if (chromatic.sumOf { it.share.toDouble() } < MinChromaticShare) {
        return monochromeColors(candidates)
    }

    val ranked = chromatic
        .map { candidate ->
            // 一大片渐变的蓝天会被拆成好几种蓝，单看每一种面积都不大，合在一起才是它真正的面积
            val neighborhood = chromatic
                .filter { hueDistance(it.hue, candidate.hue) <= HueNeighborhood }
                .sumOf { it.share.toDouble() }
                .toFloat()
            candidate to (0.6f * sqrt(neighborhood) + 0.4f * candidate.chroma)
        }
        .sortedByDescending { it.second }
        .map { it.first }

    val picked = mutableListOf<Candidate>()
    for (candidate in ranked) {
        if (picked.all { hueDistance(it.hue, candidate.hue) >= MinHueGap }) picked += candidate
        if (picked.size == 3) break
    }
    // 封面上只有一两种颜色时，用相邻的色相补齐，背景不会只有一块颜色；只偏一点，不引入封面上没有的颜色
    val main = picked.first()
    while (picked.size < 3) {
        val shift = if (picked.size == 1) CompanionHueShift else -CompanionHueShift
        picked += main.copy(hue = (main.hue + shift + 360f) % 360f, saturation = main.saturation * 0.8f)
    }
    return CoverColors(
        keyColors = picked.map { Color.hsl(it.hue, it.saturation, it.lightness) },
        isMonochrome = false
    )
}

/** 黑白灰的封面：按封面整体的明暗给几种灰，带一点点原来的色调（比如老照片的暖黄） */
private fun monochromeColors(candidates: List<Candidate>): CoverColors {
    val totalShare = candidates.sumOf { it.share.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val lightness = candidates.sumOf { (it.lightness * it.share).toDouble() }.toFloat() / totalShare
    val tinted = candidates.maxByOrNull { it.chroma * it.share }
    val hue = tinted?.hue ?: 0f
    val saturation = min(tinted?.saturation ?: 0f, MaxMonochromeTint)
    return CoverColors(
        keyColors = listOf(lightness, lightness * 0.75f + 0.08f, lightness * 0.5f + 0.05f).map {
            Color.hsl(hue, saturation, it.coerceIn(0.1f, 0.9f))
        },
        isMonochrome = true
    )
}

private fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return min(d, 360f - d)
}
