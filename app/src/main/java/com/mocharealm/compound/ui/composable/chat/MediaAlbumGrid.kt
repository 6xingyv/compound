package com.mocharealm.compound.ui.composable.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

class SwipedOutCard(
    val block: MessageBlock.MediaBlock,
    val offset: Animatable<Offset, AnimationVector2D>,
    val rotation: Animatable<Float, AnimationVector1D>
)

@Composable
fun MediaAlbumGrid(
    blocks: List<MessageBlock.MediaBlock>,
    hasTextBlock: Boolean = false,
    isOutgoing: Boolean = false,
    isBorderless: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalOriginal = blocks.size
    var startIndex by remember { mutableIntStateOf(0) }
    
    val displayBlocks = remember(blocks, startIndex) {
        if (totalOriginal == 0) emptyList() else {
            val list = mutableListOf<MessageBlock.MediaBlock>()
            for (i in 0 until minOf(4, totalOriginal)) {
                list.add(blocks[(startIndex + i) % totalOriginal])
            }
            list
        }
    }
    
    val totalDisplay = displayBlocks.size
    
    val topCardDragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    val swipedOutCards = remember { mutableStateListOf<SwipedOutCard>() }

    var lastDragTime by remember { mutableLongStateOf(0L) }
    var velocity by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            displayBlocks.reversed().forEachIndexed { revIndex, block ->
                val calculatedOriginalIndex = totalDisplay - 1 - revIndex
                
                key(block.id) {
                    var targetIndex by remember { mutableIntStateOf(calculatedOriginalIndex + 1) }
                    LaunchedEffect(calculatedOriginalIndex) {
                        targetIndex = calculatedOriginalIndex
                    }
                    
                    val staticOffsetX by animateDpAsState(
                        targetValue = when (targetIndex) {
                            0 -> 0.dp
                            1 -> 16.dp
                            2 -> (-4).dp
                            else -> (-24).dp
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.9f),
                        label = "offsetX"
                    )
                    val staticOffsetY by animateDpAsState(
                        targetValue = when (targetIndex) {
                            0 -> 0.dp
                            1 -> (-6).dp
                            2 -> (-12).dp
                            else -> (-18).dp
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.9f),
                        label = "offsetY"
                    )
                    val staticRotation by animateFloatAsState(
                        targetValue = when (targetIndex) {
                            0 -> 0f
                            1 -> 5f
                            2 -> -3f
                            else -> -10f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.9f),
                        label = "rotation"
                    )
                    val scale by animateFloatAsState(
                        targetValue = when (targetIndex) {
                            0 -> 1f
                            1 -> 0.95f
                            2 -> 0.9f
                            else -> 0.8f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.9f),
                        label = "scale"
                    )
                    val alpha by animateFloatAsState(
                        targetValue = when (targetIndex) {
                            0 -> 1f
                            1 -> 1f
                            2 -> 1f
                            else -> 0f
                        },
                        animationSpec = tween(300),
                        label = "alpha"
                    )

                    val animatedOffsetX = if (targetIndex == 0) topCardDragOffset.value.x else 0f
                    val animatedOffsetY = if (targetIndex == 0) topCardDragOffset.value.y else 0f
                    val dragRotation = if (targetIndex == 0) animatedOffsetX / 20f else 0f
                    
                    val blockWidth = if (block.width > 0) block.width.toFloat() else 800f
                    val blockHeight = if (block.height > 0) block.height.toFloat() else 800f
                    val aspectRatio = (blockWidth / blockHeight).coerceIn(0.6f, 1.8f) // Clamp slightly to prevent extremely absurd proportions
                    
                    val cardWidth = 240.dp
                    val cardHeight = cardWidth / aspectRatio

                    val modifierForTop = if (targetIndex == 0 && totalOriginal > 1) {
                        Modifier.pointerInput(block.id) {
                            detectDragGestures(
                                onDragStart = {
                                    lastDragTime = System.currentTimeMillis()
                                    velocity = Offset.Zero
                                },
                                onDragEnd = {
                                    val currentOffset = topCardDragOffset.value
                                    if (abs(currentOffset.x) > 150f || abs(velocity.x) > 500f || abs(velocity.y) > 500f) {
                                        val initialRotation = staticRotation + dragRotation
                                        val flyOut = SwipedOutCard(
                                            block = block,
                                            offset = Animatable(currentOffset, Offset.VectorConverter),
                                            rotation = Animatable(initialRotation, Float.VectorConverter)
                                        )
                                        swipedOutCards.add(flyOut)
                                        
                                        startIndex = (startIndex + 1) % totalOriginal
                                        
                                        scope.launch {
                                            topCardDragOffset.snapTo(Offset.Zero)
                                        }

                                        scope.launch {
                                            var targetX = currentOffset.x
                                            var targetY = currentOffset.y
                                            val speed = max(abs(velocity.x), abs(velocity.y))
                                            val flyDuration = 500f
                                            if (speed > 100f) {
                                                targetX += velocity.x * (flyDuration / 1000f)
                                                targetY += velocity.y * (flyDuration / 1000f)
                                                
                                                if (abs(targetX) < 1000f && abs(targetY) < 1000f) {
                                                    val mult = max(1000f / max(1f, abs(targetX)), 1000f / max(1f, abs(targetY)))
                                                    targetX *= mult
                                                    targetY *= mult
                                                }
                                            } else {
                                                val dirX = sign(currentOffset.x.takeIf { it != 0f } ?: 1f)
                                                targetX = dirX * 1000f
                                                targetY = currentOffset.y * (1000f / max(1f, abs(currentOffset.x)))
                                            }
                                            
                                            launch {
                                                flyOut.offset.animateTo(
                                                    targetValue = Offset(targetX, targetY),
                                                    animationSpec = tween(400)
                                                )
                                                swipedOutCards.remove(flyOut)
                                            }
                                            launch {
                                                val rotAdd = if (velocity.x != 0f) sign(velocity.x) * 60f else sign(targetX) * 60f
                                                flyOut.rotation.animateTo(
                                                    targetValue = initialRotation + rotAdd,
                                                    animationSpec = tween(400)
                                                )
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            topCardDragOffset.animateTo(
                                                Offset.Zero, 
                                                spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.9f)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        topCardDragOffset.animateTo(
                                            Offset.Zero, 
                                            spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.9f)
                                        )
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val currentTime = System.currentTimeMillis()
                                val dt = currentTime - lastDragTime
                                if (dt > 0) {
                                    val newVx = (dragAmount.x / dt) * 1000f
                                    val newVy = (dragAmount.y / dt) * 1000f
                                    velocity = Offset(
                                        velocity.x * 0.5f + newVx * 0.5f,
                                        velocity.y * 0.5f + newVy * 0.5f
                                    )
                                }
                                lastDragTime = currentTime
                                scope.launch {
                                    topCardDragOffset.snapTo(
                                        Offset(
                                            topCardDragOffset.value.x + dragAmount.x,
                                            topCardDragOffset.value.y + dragAmount.y
                                        )
                                    )
                                }
                            }
                        }
                    } else Modifier

                    val cardModifier = Modifier
                        .offset(x = staticOffsetX, y = staticOffsetY)
                        .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                        .graphicsLayer {
                            rotationZ = staticRotation + dragRotation
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .then(modifierForTop)
                        .shadow(elevation = if (targetIndex == 0) 4.dp else 2.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .width(cardWidth)
                        .height(cardHeight)
                        .background(Color.Gray.copy(alpha = 0.2f))

                    if (block.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO) {
                        VideoBlock(block, modifier = cardModifier, videoModifier = Modifier.fillMaxSize())
                    } else {
                        PhotoBlock(
                            block, 
                            modifier = cardModifier, 
                            imageModifier = Modifier.fillMaxSize(), 
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            swipedOutCards.forEach { flyOut ->
                key(flyOut.block.id.toString() + "_flyout") {
                    val blockWidth = if (flyOut.block.width > 0) flyOut.block.width.toFloat() else 800f
                    val blockHeight = if (flyOut.block.height > 0) flyOut.block.height.toFloat() else 800f
                    val aspectRatio = (blockWidth / blockHeight).coerceIn(0.6f, 1.8f) 
                    
                    val cardWidth = 240.dp
                    val cardHeight = cardWidth / aspectRatio

                    val flyOutModifier = Modifier
                        .offset { IntOffset(flyOut.offset.value.x.roundToInt(), flyOut.offset.value.y.roundToInt()) }
                        .graphicsLayer {
                            rotationZ = flyOut.rotation.value
                        }
                        .dropShadow(ContinuousRoundedRectangle(16.dp)) {
                            radius = 20f.dp.toPx()
                            offset = Offset(0f, 4.dp.toPx())
                            color = Color.Black.copy(alpha = 0.25f)
                        }
                        .clip(ContinuousRoundedRectangle(16.dp))
                        .width(cardWidth)
                        .height(cardHeight)
                        
                    if (flyOut.block.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO) {
                        VideoBlock(flyOut.block, modifier = flyOutModifier, videoModifier = Modifier.fillMaxSize())
                    } else {
                        PhotoBlock(
                            flyOut.block, 
                            modifier = flyOutModifier, 
                            imageModifier = Modifier.fillMaxSize(), 
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        
        if (!hasTextBlock && totalOriginal > 0) {
            Text(
                text = tdString("Of","%1\$d" to startIndex+1, "%2\$d" to totalOriginal),
                color = if (isOutgoing && !isBorderless) Color.White else MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}
