package com.mocharealm.compound.ui.composable.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.ui.composable.base.SpoilerImage
import com.mocharealm.compound.ui.composable.base.VideoPlayer


@Composable
fun MediaAlbumGrid(messages: List<Message>, modifier: Modifier = Modifier) {
    val columns = if (messages.size >= 2) 2 else 1
    val rows = (messages.size + columns - 1) / columns

    Column(modifier = modifier) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until columns) {
                    val idx = row * columns + col
                    if (idx < messages.size) {
                        val msg = messages[idx]
                        val fileUrl = msg.fileUrl
                        if (!fileUrl.isNullOrEmpty()) {
                            SpoilerImage(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp, max = 200.dp)
                                    .padding(1.dp), hasSpoiler = msg.hasSpoiler
                            ) {
                                if (msg.messageType == MessageType.VIDEO) {
                                    VideoPlayer(
                                        filePath = fileUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        loop = false,
                                        mute = true
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(java.io.File(fileUrl)).build(),
                                        contentDescription = "Album photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .padding(1.dp)
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}