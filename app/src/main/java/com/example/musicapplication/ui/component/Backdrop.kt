package com.example.musicapplication.ui.component

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 毛玻璃用的“背景画面”。
 *
 * 内容层用 [backdropSource] 把自己画进一个 GraphicsLayer（底层是 RenderNode），
 * 玻璃控件用 [backdropBlur] 在自己的位置把它再画一遍并加上模糊，原理和 Haze 一样。
 * 玻璃里画的是对这个 RenderNode 的引用：列表滚动时只有内容层重新录制，玻璃不用重画。
 *
 * 注意：玻璃控件不能放在内容层里面（否则会自己画自己），要和内容层是兄弟、画在它上面。
 */
@Stable
class Backdrop {
    internal var layer: GraphicsLayer? by mutableStateOf(null)
    internal var positionInRoot: Offset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdrop(): Backdrop = remember { Backdrop() }

/** 把这个控件画出来的内容记录到 [backdrop]，供上层的玻璃控件模糊使用 */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this then BackdropSourceElement(backdrop)

/**
 * 在当前控件的位置画出模糊后的 [backdrop]，裁成 [shape]，然后再画控件自己的内容。
 * [sigma] 是模糊程度（高斯模糊的标准差），[saturation] 让透出来的颜色更鲜艳一些。
 */
fun Modifier.backdropBlur(
    backdrop: Backdrop,
    shape: Shape,
    sigma: Dp = 10.dp,
    saturation: Float = 1.7f,
): Modifier = this then BackdropBlurElement(backdrop, shape, sigma, saturation)

/** 背景模糊 + 提高饱和度，接近 iOS 液态玻璃的通透感（RenderEffect，Android 12+） */
internal fun glassBackdropEffect(sigmaPx: Float, saturation: Float = 1.7f): RenderEffect {
    // RenderEffect 的模糊半径和高斯模糊的 sigma 之间要换算一下
    val radius = ((sigmaPx - 0.5f) / 0.57735f).coerceAtLeast(0.1f)
    val matrix = ColorMatrix().apply { setSaturation(saturation) }
    return android.graphics.RenderEffect.createChainEffect(
        android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)),
        android.graphics.RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    ).asComposeRenderEffect()
}

private data class BackdropSourceElement(val backdrop: Backdrop) : ModifierNodeElement<BackdropSourceNode>() {
    override fun create() = BackdropSourceNode(backdrop)

    override fun update(node: BackdropSourceNode) {
        node.backdrop = backdrop
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "backdropSource"
    }
}

private class BackdropSourceNode(backdrop: Backdrop) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var layer: GraphicsLayer? = null

    var backdrop: Backdrop = backdrop
        set(value) {
            if (field === value) return
            if (field.layer === layer) field.layer = null
            field = value
            value.layer = layer
        }

    override fun onAttach() {
        val created = requireGraphicsContext().createGraphicsLayer()
        layer = created
        backdrop.layer = created
    }

    override fun onDetach() {
        val current = layer ?: return
        if (backdrop.layer === current) backdrop.layer = null
        requireGraphicsContext().releaseGraphicsLayer(current)
        layer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.positionInRoot = coordinates.positionInRoot()
    }

    override fun ContentDrawScope.draw() {
        val target = layer
        if (target == null) {
            drawContent()
            return
        }
        target.record { this@draw.drawContent() }
        drawLayer(target)
    }
}

private data class BackdropBlurElement(
    val backdrop: Backdrop,
    val shape: Shape,
    val sigma: Dp,
    val saturation: Float,
) : ModifierNodeElement<BackdropBlurNode>() {
    override fun create() = BackdropBlurNode(backdrop, shape, sigma, saturation)

    override fun update(node: BackdropBlurNode) {
        node.update(backdrop, shape, sigma, saturation)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "backdropBlur"
    }
}

private class BackdropBlurNode(
    private var backdrop: Backdrop,
    private var shape: Shape,
    private var sigma: Dp,
    private var saturation: Float,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var blurLayer: GraphicsLayer? = null
    private var effect: RenderEffect? = null
    private var effectKey: Pair<Float, Float>? = null

    fun update(backdrop: Backdrop, shape: Shape, sigma: Dp, saturation: Float) {
        this.backdrop = backdrop
        this.shape = shape
        this.sigma = sigma
        this.saturation = saturation
        invalidateDraw()
    }

    override fun onDetach() {
        blurLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        blurLayer = null
    }

    // 控件自己移动时（底栏收起、迷你播放器展开）要重新对齐背景
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val source = backdrop.layer
        if (source != null && size.minDimension > 0f) {
            val sigmaPx = sigma.toPx()
            val key = sigmaPx to saturation
            if (effectKey != key) {
                effect = glassBackdropEffect(sigmaPx, saturation)
                effectKey = key
            }
            val layer = blurLayer ?: requireGraphicsContext().createGraphicsLayer().also { blurLayer = it }
            layer.renderEffect = effect

            // 四周多录一圈，边缘的模糊也能取到真实的背景，不会发灰
            val pad = ceil(sigmaPx * 2).toInt()
            val offset = requireLayoutCoordinates().positionInRoot() - backdrop.positionInRoot
            layer.topLeft = IntOffset(-pad, -pad)
            layer.record(size = IntSize(size.width.roundToInt() + pad * 2, size.height.roundToInt() + pad * 2)) {
                translate(left = pad - offset.x, top = pad - offset.y) {
                    drawLayer(source)
                }
            }

            val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@draw)) }
            clipPath(clip) { drawLayer(layer) }
        }
        drawContent()
    }
}
