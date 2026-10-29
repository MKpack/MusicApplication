package com.example.musicapplication.ui.mainPage.audioPlayer

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.min

class CoverColorsTest {

    @Test
    fun `白底上面积不大的红字才是重点颜色`() {
        val colors = pickCoverColors(
            listOf(
                CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.98f, population = 800),
                CoverSwatch(hue = 40f, saturation = 0.05f, lightness = 0.85f, population = 120),
                CoverSwatch(hue = 355f, saturation = 0.85f, lightness = 0.5f, population = 80),
            )
        )

        assertFalse(colors.isMonochrome)
        assertEquals(355f, colors.keyColors.first().hue(), 1f)
    }

    @Test
    fun `色相相近的颜色面积合起来算`() {
        // 一片蓝天被拆成 4 种蓝，每种 6%；橙色只有一种，占 15%
        val colors = pickCoverColors(
            listOf(
                CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.97f, population = 61),
                CoverSwatch(hue = 30f, saturation = 0.7f, lightness = 0.5f, population = 15),
                CoverSwatch(hue = 200f, saturation = 0.7f, lightness = 0.5f, population = 6),
                CoverSwatch(hue = 205f, saturation = 0.7f, lightness = 0.5f, population = 6),
                CoverSwatch(hue = 210f, saturation = 0.7f, lightness = 0.5f, population = 6),
                CoverSwatch(hue = 214f, saturation = 0.7f, lightness = 0.5f, population = 6),
            )
        )

        assertTrue(colors.keyColors[0].hue() in 195f..220f)
        assertEquals(30f, colors.keyColors[1].hue(), 1f)
    }

    @Test
    fun `只有一种颜色时用相邻的色相补齐 3 种`() {
        val colors = pickCoverColors(
            listOf(
                CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.95f, population = 60),
                CoverSwatch(hue = 220f, saturation = 0.6f, lightness = 0.45f, population = 40),
            )
        )

        assertEquals(3, colors.keyColors.size)
        assertEquals(220f, colors.keyColors[0].hue(), 1f)
        colors.keyColors.drop(1).forEach { assertTrue(hueDistance(it.hue(), 220f) < 30f) }
        assertNotEquals(colors.keyColors[1], colors.keyColors[2])
    }

    @Test
    fun `黑白封面给几种中性的灰`() {
        val colors = pickCoverColors(
            listOf(
                CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.1f, population = 500),
                CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.6f, population = 300),
                // 偏暖的白：太白了，不算有颜色
                CoverSwatch(hue = 30f, saturation = 0.4f, lightness = 0.97f, population = 200),
            )
        )

        assertTrue(colors.isMonochrome)
        assertEquals(3, colors.keyColors.size)
        colors.keyColors.forEach { color ->
            val spread = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            assertTrue("$color 不够灰", spread < 0.07f)
        }
    }

    @Test
    fun `没有颜色数据时用默认的深灰`() {
        assertEquals(CoverColors.Default, pickCoverColors(emptyList()))
        assertEquals(
            CoverColors.Default,
            pickCoverColors(listOf(CoverSwatch(hue = 0f, saturation = 0f, lightness = 0.5f, population = 0)))
        )
    }

    /** sRGB 颜色的色相，0~360 */
    private fun Color.hue(): Float {
        val max = maxOf(red, green, blue)
        val delta = max - minOf(red, green, blue)
        if (delta == 0f) return 0f
        val sector = when (max) {
            red -> ((green - blue) / delta).mod(6f)
            green -> (blue - red) / delta + 2f
            else -> (red - green) / delta + 4f
        }
        return sector * 60f
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return min(d, 360f - d)
    }
}
