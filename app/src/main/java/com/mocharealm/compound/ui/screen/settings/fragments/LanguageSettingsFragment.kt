package com.mocharealm.compound.ui.screen.settings.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.LanguageSettingsModuleToken
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import com.mocharealm.tci18n.core.tdLangPackId
import com.mocharealm.tci18n.core.tdString
import com.mocharealm.tcsettings.compose.rememberSettingsController
import com.mocharealm.tcsettings.core.SettingsStore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.util.Locale

data class LanguageInfo(
    val id: String,
    val name: String,
    val nativeName: String,
    val isOfficial: Boolean,
    val isRtl: Boolean
)

@Composable
fun LanguageSettingsFragment() {
    val inDark = isSystemInDarkTheme()
    val surfaceColor =
        if (inDark) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.surface
    val surfaceContainerColor =
        if (inDark) MiuixTheme.colorScheme.surface else MiuixTheme.colorScheme.surfaceContainer

    val tdLibDataSource: TdLibDataSource = koinInject()
    val store: SettingsStore = koinInject()
    val controller = rememberSettingsController()
    val scope = rememberCoroutineScope()

    // 获取当前语言设置
    val currentLanguage by remember {
        store.flow(
            LanguageSettingsModuleToken.LanguageCode,
            tdLangPackId(Locale.getDefault())
        )
    }.collectAsState(initial = "")

    var languages by remember { mutableStateOf<List<LanguageInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 获取语言列表
    LaunchedEffect(Unit) {
        tdLibDataSource.getLocalizationTargetInfo().fold(
            onSuccess = { info ->
                languages = info.languagePacks.map { lang ->
                    LanguageInfo(
                        id = lang.id,
                        name = lang.name,
                        nativeName = lang.nativeName,
                        isOfficial = lang.isOfficial,
                        isRtl = lang.isRtl
                    )
                }
                isLoading = false
            },
            onFailure = {
                isLoading = false
            }
        )
    }

    Scaffold(
        Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .background(surfaceContainerColor)
                .fillMaxSize()
                .scrollEndHaptic(),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            // 语言选择
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tdString("SettingsLanguage"),
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        if (isLoading) {
                            Text(
                                "Loading languages...",
                                style = MiuixTheme.textStyles.body2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .alpha(0.6f)
                            )
                        } else {
                            languages.forEach { language ->
                                LanguageItem(
                                    language = language,
                                    isSelected = currentLanguage == language.id,
                                    onClick = {
                                        // 更新语言设置
                                        scope.launch {
                                            controller.update(
                                                LanguageSettingsModuleToken.LanguageCode,
                                                language.id
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 翻译设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Translation",
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Translate Messages", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Automatically translate incoming messages",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            LanguageSettingsModuleToken.TranslateMessages.Render()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: LanguageInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                language.name,
                style = MiuixTheme.textStyles.body1,
                color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
            )
            if (language.nativeName != language.name) {
                Text(
                    language.nativeName,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.alpha(0.6f)
                )
            }
        }
        if (isSelected) {
            Text(
                "✓",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.primary
            )
        }
    }
}
