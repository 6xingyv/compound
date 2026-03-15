package com.mocharealm.compound.ui.screen.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.mocharealm.compound.BuildConfig
import com.mocharealm.compound.R
import com.mocharealm.compound.domain.model.File
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.ShareInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.screen.chat.GroupPosition
import com.mocharealm.compound.ui.screen.intro.composable.ContinuousDepthFloatingScene
import com.mocharealm.compound.ui.screen.signin.SignInScreen
import com.mocharealm.compound.ui.util.PaddingValuesSide
import com.mocharealm.compound.ui.util.takeExcept
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.ui.animation.InteractiveHighlight
import com.mocharealm.gaze.ui.composable.Button
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

@Composable
fun IntroScreen() {
    val showBottomSheet = remember { mutableStateOf(false) }
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight =
        remember(animationScope) { InteractiveHighlight(animationScope = animationScope) }

    val animation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(8000)
        launch { animation.animateTo(1f, tween(1000)) }
    }

    fun fadeVerticalBrush(
        progress: Float,
        fadeRange: Float = 0.2f,
        startColor: Color = Color.White,
        endColor: Color = Color.White.copy(0f)
    ): Brush {
        if (progress <= 0.01f) return SolidColor(startColor)
        if (progress >= 0.99f) return SolidColor(endColor)
        val start = 1f - progress * (1f + fadeRange)
        val end = start + fadeRange
        return Brush.verticalGradient(start to startColor, end to endColor)
    }

    val messages = rememberMessages()

    val messageComposables = remember {
        messages.map { msg ->
            @Composable
            {
                MessageBubble(
                    message = msg,
                    groupPosition = GroupPosition.SINGLE,
                    showAvatar = false,
                )
            }
        }
    }

    Scaffold { innerPadding ->
        if (animation.value <= 0.99f)
            ContinuousDepthFloatingScene(
                messageComposables,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -200f * animation.value
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush =
                                    fadeVerticalBrush(
                                        animation.value,
                                    ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            )

        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = false
                    compositingStrategy = CompositingStrategy.Offscreen
                    translationY = 200f * (1f - animation.value)
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush =
                                fadeVerticalBrush(
                                    animation.value,
                                    startColor = Color.White.copy(0f),
                                    endColor = Color.White
                                ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val surfaceColor = MiuixTheme.colorScheme.surface
            val primaryColor = MiuixTheme.colorScheme.primary
            val layerBackdrop = rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding.takeExcept(PaddingValuesSide.Bottom))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-28).dp)
            ) {
                Image(
                    painterResource(R.drawable.ic_compound),
                    "",
                    Modifier
                        .aspectRatio(1f)
                        .sizeIn(maxHeight = 480.dp)
                        .graphicsLayer {
                            val width = size.width
                            val height = size.height

                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX =
                                maxOffset *
                                        tanh(initialDerivative * offset.x / maxOffset)
                            translationY =
                                maxOffset *
                                        tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale *
                                        abs(
                                            cos(offsetAngle) * offset.x /
                                                    size.maxDimension
                                        ) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale *
                                        abs(
                                            sin(offsetAngle) * offset.y /
                                                    size.maxDimension
                                        ) *
                                        (height / width).fastCoerceAtMost(1f)
                        }
                        .then(interactiveHighlight.gestureModifier)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Compound",
                        style = MiuixTheme.textStyles.title1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "A beautiful messenger app.",
                        Modifier.alpha(0.6f),
                        style = MiuixTheme.textStyles.body1,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val context = LocalContext.current
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                Text(
                    "${packageInfo.versionName ?: "Unknown"} (${BuildConfig.GIT_COMMIT_HASH})",
                    Modifier.alpha(0.6f),
                    style = MiuixTheme.textStyles.footnote1,
                    textAlign = TextAlign.Center
                )
                Button(
                    { showBottomSheet.value = true },
                    layerBackdrop,
                    Modifier
                        .padding(innerPadding.takeExcept(PaddingValuesSide.Top))
                        .fillMaxWidth()
                        .padding(16.dp),
                    tint = primaryColor
                ) {
                    Text(
                        tdString("login_with_telegram"),
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MiuixTheme.textStyles.body1,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            SuperBottomSheet(
                show = showBottomSheet.value,
                modifier = Modifier,
                title = tdString("login_with_telegram"),
                insideMargin = DpSize.Zero,
                onDismissRequest = { showBottomSheet.value = false },
                renderInRootScaffold = false,
            ) { Column(Modifier.fillMaxSize()) { SignInScreen() } }
        }
    }
}

@Composable
private fun rememberMessages(
    count: Int = 200,
    seed: Long = System.currentTimeMillis()
): List<Message> {
    return remember(count, seed) { generateMessages(count, seed) }
}

private fun generateMessages(count: Int, seed: Long): List<Message> {
    val random = Random(seed)

    val senderMe = User(id = 0, firstName = "Simon", lastName = "Scholz", username = "simon")
    val senders =
        listOf(
            senderMe,
            User(id = 1, firstName = "Mocha", lastName = "Pot", username = "mochapot"),
            User(id = 2, firstName = "Mocha", lastName = "Realm", username = "mocharealm"),
        )

    val textsArray =
        arrayOf(
            "这个副产物还是意外问题",
            "或者说",
            "that looks much better ✨",
            "Why Stella music couldn't play music",
            "it's just a simple theme, all glass materials are from telegram itself, no theme can modify that",
            "My phone can't handle the storage 🙃",
            "Would you be so kind as to share the link or apk file with us? please",
            "Checking the latest logs... 🔍",
            "Looks like a logic error in the backend.",
            "笑死，这 Bug 竟然还没修 💀",
            "Can anyone confirm if this works on Android 15?",
            "Wait, let me check the documentation real quick.",
            "太强了，大佬带带我 Orz",
            "Just a quick reminder to backup your data!",
            "failed again",
            "有没有好用的平替推荐？",
            "The UI is surprisingly smooth",
            "Nice work! 🚀",
            "I think we need to refactor this part anyway.",
            "这是特性，不是 Bug 😂",
            "Anyone up for some testing tonight?",
            "Downloading... 99% (ETA: 2 hours) 📶",
            "Is it just me or the server is slow today?",
            "Fixed the crash, pushing the update now 🛠️",
            "这界面有点像 iOS 了",
            "Good morning everyone! ☕",
            "Doesn't work for me, maybe it's the kernel version.",
            "Try clearing the cache and see if it helps.",
            "这个🎨透明效果是怎么实现的",
            "Perfectly balanced, as all things should be.",
            "I'm using the latest beta build.",
            "有点意思，但我懒得",
            "The storage consumption is insane 📈",
            "Keep up the good work! 👏",
            "Oops, wrong group 😅",
            "Does this support Material You dynamic colors?",
            "Tested on Pixel 9, works like a charm.",
            "Not all heroes wear capes, thanks! 🙌",
            "Let's move this discussion to the dev channel",
            "Is there any workaround for this?",
            "蚌埠住了，这都能炸 💥",
            "Update: It's working now after a reboot",
            "Where can I find the source code? 📂",
            "I prefer the previous version honestly.",
            "新版本更新了啥",
            "Seems like a permission issue",
            "Everything is fine here, no issues found"
        )

    val typeWeights = listOf("TEXT" to 0.4, "STICKER" to 0.3, "PHOTO" to 0.1)
    val cumulativeWeights =
        typeWeights.runningFold(0.0) { acc, (_, weight) -> acc + weight }.drop(1)

    val stickers = 1..70
    val lifePhotos = 1..11
    val clefPhotos = 1..4

    return buildList(count) {
        repeat(count) { index ->
            val sender = senders.random(random)
            val msgId = index.toLong()
            val timestamp = random.nextLong()
            val isOutgoing = sender == senderMe

            val typeStr = getRandomWeightedType(typeWeights, cumulativeWeights, random)
            val message =
                when (typeStr) {
                    "PHOTO" -> {
                        val isClef = random.nextBoolean()
                        val fileUrl =
                            "file:///android_asset/photos/${
                                if (isClef) "clef/${clefPhotos.random(random)}"
                                else "life/${lifePhotos.random(random)}"
                            }.webp"
                        Message(
                            sender = sender,
                            chatId = 0,
                            isOutgoing = isOutgoing,
                            blocks =
                                listOf(
                                    MessageBlock.MediaBlock(
                                        id = msgId,
                                        timestamp = timestamp,
                                        mediaType =
                                            MessageBlock.MediaBlock
                                                .MediaType.PHOTO,
                                        file =
                                            File(
                                                fileId = 0,
                                                fileUrl = fileUrl
                                            ),
                                        hasSpoiler =
                                            if (isClef) false
                                            else random.nextBoolean(),
                                    )
                                ),
                            shareInfo =
                                if (isClef)
                                    ShareInfo(
                                        "Clef",
                                        "https://i.imgur.com/26mpQhd.png",
                                        ""
                                    )
                                else null
                        )
                    }

                    "STICKER" -> {
                        val fileUrl =
                            "file:///android_asset/stickers/${stickers.random(random)}.tgs"
                        Message(
                            sender = sender,
                            chatId = 0,
                            isOutgoing = isOutgoing,
                            blocks =
                                listOf(
                                    MessageBlock.StickerBlock(
                                        id = msgId,
                                        timestamp = timestamp,
                                        stickerFormat =
                                            MessageBlock.StickerBlock
                                                .StickerFormat.TGS,
                                        file =
                                            File(
                                                fileId = 0,
                                                fileUrl = fileUrl
                                            ),
                                        caption = Text(""),
                                    )
                                ),
                        )
                    }

                    else ->
                        Message(
                            sender = sender,
                            chatId = 0,
                            isOutgoing = isOutgoing,
                            blocks =
                                listOf(
                                    MessageBlock.TextBlock(
                                        id = msgId,
                                        timestamp = timestamp,
                                        content =
                                            Text(
                                                textsArray.random(
                                                    random
                                                )
                                            ),
                                    )
                                ),
                        )
                }
            add(message)
        }
    }
}

private fun <T> getRandomWeightedType(
    items: List<Pair<T, Double>>,
    cumulativeWeights: List<Double>,
    random: Random
): T {
    val randomValue = random.nextDouble() * cumulativeWeights.last()

    var low = 0
    var high = cumulativeWeights.size - 1

    while (low <= high) {
        val mid = (low + high) ushr 1
        when {
            randomValue <= cumulativeWeights[mid] -> {
                if (mid == 0 || randomValue > cumulativeWeights[mid - 1]) {
                    return items[mid].first
                }
                high = mid - 1
            }

            else -> low = mid + 1
        }
    }

    return items.last().first
}

private fun <T> List<T>.random(random: Random): T = get(random.nextInt(size))
