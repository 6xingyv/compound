package com.mocharealm.compound.ui.screen.intro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.screen.chat.GroupPosition
import com.mocharealm.compound.ui.screen.intro.composable.ContinuousDepthFloatingScene
import kotlin.random.Random

@Composable
fun IntroScreen() {
    val context = LocalContext.current
    val senderMe = 0 to "Simon Scholz"
    val senders = listOf(
        senderMe,
        1 to "Mocha Pot",
        2 to "YuKongA",
    )
    val texts = listOf(
        "这个副产物还是意外问题",
        "或者说",
        "that looks much better ✨",
        "Why Stella music couldn't play music",
        "it's just a simple theme, all glass materials are from telegram itself, no theme can modify that",
        "I'm officially quitting flamingo and downloading musics cause my phone can't handle the storage 🙃",
        "Would you be so kind as to share the link or apk file with us? please",
        "有人知道这个固件怎么刷吗？🤔",
        "Checking the latest logs... 🔍",
        "Looks like a logic error in the backend.",
        "笑死，这 Bug 竟然还没修 💀",
        "Can anyone confirm if this works on Android 15?",
        "Wait, let me check the documentation real quick.",
        "太强了，大佬带带我 Orz",
        "Just a quick reminder to backup your data!",
        "GitHub action failed again, standard procedure 🤦‍♂️",
        "有没有好用的平替推荐？",
        "The UI is surprisingly smooth on high refresh rates.",
        "Nice work! 🚀",
        "I think we need to refactor this part anyway.",
        "这是特性，不是 Bug 😂",
        "Anyone up for some testing tonight?",
        "Downloading... 99% (ETA: 2 hours) 📶",
        "Is it just me or the server is slow today?",
        "Fixed the crash, pushing the update now 🛠️",
        "这界面改得有点像 iOS 了",
        "Good morning everyone! ☕",
        "Doesn't work for me, maybe it's the kernel version.",
        "Try clearing the cache and see if it helps.",
        "这个透明效果是怎么实现的？🎨",
        "Perfectly balanced, as all things should be.",
        "I'm using the latest beta build.",
        "有点意思，但我选择原地观望",
        "The storage consumption is insane 📈",
        "Keep up the good work! 👏",
        "Oops, wrong group 😅",
        "Does this support Material You dynamic colors?",
        "这翻译绝了，一股机翻味",
        "Tested on Pixel 9, works like a charm.",
        "Not all heroes wear capes, thanks! 🙌",
        "这个动画掉帧有点严重啊",
        "Let's move this discussion to the dev channel",
        "Is there any workaround for this?",
        "蚌埠住了，这都能炸 💥",
        "Update: It's working now after a reboot",
        "Where can I find the source code? 📂",
        "I prefer the previous version honestly.",
        "新版本更新了啥？有 Changelog 吗？",
        "Seems like a permission issue 🔑",
        "Everything is fine here, no issues found"
    )
    val types = listOf(
        MessageType.TEXT,
        MessageType.STICKER,
    )
    val stickers = 1..70
    val childs = List(20) { index ->
        val sender = senders.random()
        val type = types.random()
        Message(
            id = index.toLong(),
            chatId = 0,
            senderId = sender.first.toLong(),
            senderName = sender.second,
            content = if (type == MessageType.TEXT) texts.random() else "",
            timestamp = Random.nextLong(),
            isOutgoing = sender == senderMe,
            messageType = type,
            fileUrl = if (type == MessageType.TEXT) null else "file:///android_asset/stickers/${stickers.random()}.tgs",
            fileId = Random.nextInt(),
            stickerFormat = StickerFormat.TGS,
            entities = emptyList(),
            replyTo = null,
            hasSpoiler = Random.nextBoolean(),
            thumbnailFileId = null,
            thumbnailUrl = null,
            shareInfo = null
        )
    }.map { msg ->
        @Composable {
            MessageBubble(msg, GroupPosition.SINGLE, false, null)
        }
    }
    ContinuousDepthFloatingScene(childs, Modifier.fillMaxSize())
}