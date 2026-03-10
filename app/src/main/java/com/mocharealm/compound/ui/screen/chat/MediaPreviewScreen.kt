package com.mocharealm.compound.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.MediaItem
import com.mocharealm.compound.ui.util.LocalAnimatedVisibilityScope
import com.mocharealm.compound.ui.util.LocalSharedTransitionScope
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.nav.LocalListDetailExpanded
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import java.io.File

@Composable
fun MediaPreviewScreen(items: List<MediaItem>, initialIndex: Int) {
    val navigator = LocalNavigator.current
    val expandedState = LocalListDetailExpanded.current
    
    LaunchedEffect(Unit) {
        expandedState?.value = true
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    LaunchedEffect(pagerState.currentPage) {
        listState.animateScrollToItem(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val imageModifier = Modifier.fillMaxSize()
                val finalModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        imageModifier.sharedElement(
                            rememberSharedContentState(key = "media_${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else imageModifier

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (item.url.startsWith("http")) item.url else File(item.url))
                        .build(),
                    contentDescription = "Media Preview",
                    modifier = finalModifier,
                    contentScale = ContentScale.Fit
                )

                if (item.type == MediaItem.MediaType.VIDEO) {
                    Icon(
                        imageVector = SFIcons.Play_Fill,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = SFIcons.Chevron_Left,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { navigator.pop() }
            )

            Text(
                text = "${pagerState.currentPage + 1} ${tdString("Of")} ${items.size}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Box(Modifier.size(32.dp)) // Placeholder for symmetry
        }

        // Bottom Bar (Thumbnails)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(items) { index, item ->
                    val isSelected = pagerState.currentPage == index
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if ((item.thumbnailUrl ?: item.url).startsWith("http")) (item.thumbnailUrl ?: item.url) else File(item.thumbnailUrl ?: item.url))
                            .build(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(if (isSelected) 60.dp else 50.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
