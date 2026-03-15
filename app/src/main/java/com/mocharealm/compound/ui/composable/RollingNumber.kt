package com.mocharealm.compound.ui.composable

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text


class CharAnimState {
    val scale = Animatable(1f)
    val translationY = Animatable(0f)
    val alpha = Animatable(1f)
    val blur = Animatable(0f)

    // 统一配置：进场动画
    suspend fun animateIn(startOffsetY: Float) {
        // 关键：在动画开始前，强制“瞬移”到进场前的初始状态（透明、缩小）
        scale.snapTo(0.4f)
        translationY.snapTo(startOffsetY)
        alpha.snapTo(0f)
        blur.snapTo(4f)

        // 并行启动动画
        coroutineScope {
            launch {
                scale.animateTo(
                    1f,
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
            }
            launch {
                translationY.animateTo(
                    0f,
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
            }
            launch {
                alpha.animateTo(1f, tween(300, easing = LinearEasing))
            }
            launch {
                blur.animateTo(0f, tween(300, easing = LinearEasing))
            }
        }
    }

    suspend fun animateOut(endOffsetY: Float) {
        coroutineScope {
            launch {
                scale.animateTo(
                    0.4f,
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
            }
            launch {
                translationY.animateTo(
                    endOffsetY,
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
            }
            launch {
                alpha.animateTo(0f, tween(300, easing = LinearEasing))
            }
            launch {
                blur.animateTo(4f, tween(300, easing = LinearEasing))
            }
        }
    }
}

@Stable
data class RunningChar(
    val char: Char,
    val animState: CharAnimState = CharAnimState()
)

@Composable
fun ManualRollingTimeDisplay(
    timeParts: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 2. 遍历列表动态生成 UI
        timeParts.forEachIndexed { index, part ->
            // 渲染滚动的数字部分
            ManualRollingNumber(
                numberText = part,
                style = style,
                color = color,
            )
            if (index < timeParts.lastIndex) {
                Text(
                    text = ":",
                    style = style,
                    color = color,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }
    }
}

@Composable
fun ManualRollingNumber(
    numberText: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        numberText.forEachIndexed { index, char ->
            ManualRollingChar(
                targetChar = char,
                style = style,
                color = color,
                key = index
            )
        }
    }
}

@Composable
private fun ManualRollingChar(
    targetChar: Char,
    style: TextStyle,
    color: Color,
    key: Int
) {
    var heightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    var currentChar by remember { mutableStateOf(RunningChar(targetChar)) }
    var exitingChar by remember { mutableStateOf<RunningChar?>(null) }

    // 记录上一次的字符，用于判断方向
    var lastChar by remember { mutableStateOf(targetChar) }

    // 3. 监听字符变化，触发动画逻辑
    LaunchedEffect(targetChar) {
        if (targetChar == lastChar) return@LaunchedEffect

        val isIncrement = when {
            lastChar == '9' && targetChar == '0' -> true
            lastChar == '0' && targetChar == '9' -> false
            else -> targetChar >= lastChar
        }
        lastChar = targetChar

        val startOffsetY = if (isIncrement) heightPx else -heightPx // 新字起始位置
        val endOffsetY = if (isIncrement) -heightPx else heightPx   // 旧字结束位置

        // 步骤A：将当前的char“降级”为exitingChar，并准备离场
        val oldRunningChar = currentChar
        exitingChar = oldRunningChar

        // 步骤B：创建新的current char
        val newRunningChar = RunningChar(targetChar)
        currentChar = newRunningChar

        // 步骤C：并行执行：旧字出场 + 新字进场
        launch {
            // 这里使用了新的协程作用域，确保两个动画互不阻塞
            launch {
                oldRunningChar.animState.animateOut(endOffsetY)
                // 动画结束，移除旧字符引用
                if (exitingChar == oldRunningChar) {
                    exitingChar = null
                }
            }
            launch {
                newRunningChar.animState.animateIn(startOffsetY)
            }
        }
    }

    // 4. UI 布局
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.onSizeChanged { heightPx = it.height.toFloat() }
    ) {
        // 绘制“正在退出”的字符
        exitingChar?.let { running ->
            RenderSingleChar(running, color, style, density.density)
        }

        // 绘制“当前/正在进入”的字符
        RenderSingleChar(currentChar, color, style, density.density)

        // 占位符：确保Box有正确的高度（因为上面的字符都在动，可能导致布局塌陷，这里放一个不可见的字撑开高度）
        Text(text = "0", style = style, color = Color.Transparent)
    }
}

@Composable
private fun RenderSingleChar(
    running: RunningChar,
    color: Color,
    style: TextStyle,
    density: Float
) {
    val state = running.animState

    if (state.alpha.value > 0.01f) {
        Text(
            text = running.char.toString(),
            style = style,
            color = color,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = state.scale.value
                    scaleY = state.scale.value
                    translationY = state.translationY.value
                    alpha = state.alpha.value

                    transformOrigin = TransformOrigin.Center

                    val blurVal = state.blur.value
                    if (blurVal > 0f) {
                        renderEffect = BlurEffect(
                            blurVal * density,
                            blurVal * density,
                            TileMode.Decal
                        )
                    }
                }
        )
    }
}