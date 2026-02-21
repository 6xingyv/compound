package com.mocharealm.compound.ui.screen.chat.composable

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.domain.model.ShareInfo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.core.net.toUri
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle

/**
 * Compact source-card shown at the bottom of a message bubble when the
 * message was shared from a third-party app via the Compound Share Protocol.
 */
@Composable
fun ShareSourceCard(
    shareInfo: ShareInfo,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .clip(ContinuousRoundedRectangle(8.dp))
            .background(MiuixTheme.colorScheme.onSurfaceContainer.copy(0.1f))
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, shareInfo.appUrl.toUri())
                    )
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(shareInfo.iconUrl)
                .build(),
            contentDescription = shareInfo.name,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                val error by painter.state.collectAsState()
                Log.e("CoilError", "Throwable: $error")

                Avatar(
                    initials = shareInfo.name.take(1),
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        Text(
            text = shareInfo.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor
        )
    }
}
