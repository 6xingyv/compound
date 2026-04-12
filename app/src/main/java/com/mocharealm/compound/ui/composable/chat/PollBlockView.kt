package com.mocharealm.compound.ui.composable.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.util.toAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PollBlockView(
    block: MessageBlock.PollBlock,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RichText(
            text = block.question.toAnnotatedString(),
            style = MiuixTheme.textStyles.body1,
            contentColor = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = if (block.isAnonymous) "Anonymous Poll" else "Public Poll",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        block.options.forEach { option ->
            PollOptionView(option = option, Modifier.padding(bottom = 8.dp))
        }
        
        Text(
            text = "${block.totalVoterCount} votes",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PollOptionView(
    option: MessageBlock.PollBlock.PollOption,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
        ) {
            Text(
                text = "${option.votePercentage}%",
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.width(40.dp)
            )
            RichText(
                text = option.text.toAnnotatedString(),
                style = MiuixTheme.textStyles.body2,
                contentColor = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (option.isChosen) {
                // simple dot to indicate chosen
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(ContinuousRoundedRectangle(3.dp))
                        .background(MiuixTheme.colorScheme.primary)
                )
            }
        }
        
        // Progress bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(ContinuousRoundedRectangle(3.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant)
        ) {
            // Actual progress
            if (option.votePercentage > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(option.votePercentage / 100f)
                        .height(6.dp)
                        .clip(ContinuousRoundedRectangle(3.dp))
                        .background(MiuixTheme.colorScheme.primary)
                )
            }
        }
    }
}
