package com.mocharealm.compound.ui.screen.msglist.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.ui.composable.ManualRollingNumber
import com.mocharealm.compound.ui.composable.base.Avatar
import com.mocharealm.compound.ui.composable.chat.RichText
import com.mocharealm.compound.ui.util.formatMessageTimestamp
import com.mocharealm.compound.ui.util.toAnnotatedString
import com.mocharealm.compound.ui.util.toPreviewAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousCapsule
import com.mocharealm.gaze.icons.At
import com.mocharealm.gaze.icons.Heart_Fill
import com.mocharealm.gaze.icons.Pin
import com.mocharealm.gaze.icons.SFIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChatListItem(
    chat: Chat,
    onChatClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    titleFontWeight: FontWeight = FontWeight.Bold,
    titleVerticalAlignment: Alignment.Vertical = Alignment.CenterVertically
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { 28.dp.toPx() }
    val avatarPx = with(density) { 45.dp.toPx() }
    val spacePx = with(density) { 12.dp.toPx() }
    val separatorColor = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(0.12f)

    Row(
        modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .drawWithCache {
                onDrawBehind {
                    drawLine(
                        separatorColor,
                        start = Offset(paddingPx + avatarPx + spacePx, size.height),
                        end = Offset(size.width - paddingPx, size.height),
                        strokeWidth = 1f
                    )
                }
            }
            .clickable { onChatClick(chat.id) }
            .padding(28.dp, 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            modifier = Modifier.size(45.dp),
            photoPath = chat.photoUrl,
            initials = chat.title.content.take(2)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = titleVerticalAlignment) {
                RichText(
                    text = chat.title.toAnnotatedString(),
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = titleFontWeight),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    isInteractive = false
                )
                Text(
                    text = chat.lastMessageDate.formatMessageTimestamp(),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(0.6f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RichText(
                    text = chat.lastMessage?.toPreviewAnnotatedString(chat)
                        ?: AnnotatedString(""),
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.6f),
                    maxLines = 2,
                    minLines = 2,
                    isInteractive = false
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MiuixTheme.colorScheme.primary,
                                    shape = ContinuousCapsule
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ManualRollingNumber(
                                numberText = chat.unreadCount.toString(),
                                color = Color.White,
                                style = MiuixTheme.textStyles.footnote1
                            )
                        }
                    }
                    Row(
                        Modifier.alpha(0.6f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chat.isPinned) {
                            Icon(
                                imageVector = SFIcons.Pin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(16.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        }
                        if (chat.unreadMentionCount > 0) {
                            Icon(
                                imageVector = SFIcons.At,
                                contentDescription = "Mention",
                                modifier = Modifier.size(16.dp),
                                tint = MiuixTheme.colorScheme.primary
                            )
                        }
                        if (chat.unreadReactionCount > 0) {
                            Icon(
                                imageVector = SFIcons.Heart_Fill,
                                contentDescription = "Reaction",
                                modifier = Modifier.size(16.dp),
                                tint = MiuixTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
