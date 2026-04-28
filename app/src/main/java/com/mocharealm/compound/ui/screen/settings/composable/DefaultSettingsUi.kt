package com.mocharealm.compound.ui.screen.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.ui.composable.ManualRollingNumber
import com.mocharealm.gaze.capsule.ContinuousCapsule
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.icons.Minus
import com.mocharealm.gaze.icons.Plus
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.Button
import com.mocharealm.gaze.ui.composable.Toggle
import com.mocharealm.tcsettings.core.SelectableValue
import com.mocharealm.tcsettings.core.SettingToken
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RenderBooleanSettingItem(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) = Toggle({ value }, onValueChange)

@Composable
fun RenderStringSettingItem(
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        modifier = Modifier
    )
}

@Composable
fun RenderIntSettingItem(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val trackColor =
        if (!isSystemInDarkTheme()) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)
    val layerBackdrop = rememberLayerBackdrop {
        drawRect(trackColor)
        drawContent()
    }

    Row(
        Modifier
            .background(trackColor, ContinuousCapsule)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            {
                onValueChange(value - 1)
            },
            surfaceColor = trackColor,
            shape = CircleShape
        ) {
            Icon(
                SFIcons.Minus,
                null,
                Modifier
                    .padding(2.dp)
                    .size(24.dp)
            )
        }
        ManualRollingNumber(
            value.toString(),
            LocalContentColor.current,
            MiuixTheme.textStyles.body1,
            Modifier.clip(RectangleShape)
        )
        Button(
            {
                onValueChange(value + 1)
            },
            surfaceColor = trackColor,
            shape = CircleShape
        ) {
            Icon(
                SFIcons.Plus,
                null,
                Modifier
                    .padding(2.dp)
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun <T> RenderSelectableSettingItem(
    token: SettingToken<T>,
    value: SelectableValue<T>,
    onValueChange: (T) -> Unit
) {
    Text(
        text = value.current.toString(),
        modifier = Modifier.clickable {
            if (value.options.isNotEmpty()) {
                val currentIndex =
                    value.options.indexOf(value.current).let { if (it < 0) 0 else it }
                val nextIndex = (currentIndex + 1) % value.options.size
                onValueChange(value.options[nextIndex])
            }
        }
    )
}
