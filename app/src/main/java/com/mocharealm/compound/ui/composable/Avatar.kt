package com.mocharealm.compound.ui.composable

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import top.yukonga.miuix.kmp.basic.Text

@SuppressLint("ModifierParameter")
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier.size(40.dp),
    photoPath: String? = null
) {
    if (!photoPath.isNullOrEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(java.io.File(photoPath))
                .build(),
            contentDescription = "Avatar",
            modifier = modifier
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
        }
    }
}