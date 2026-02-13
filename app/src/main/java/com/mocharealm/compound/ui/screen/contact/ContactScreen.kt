package com.mocharealm.compound.ui.screen.contact

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

private data class Contact(val name: String, val subtitle: String)

@Composable
fun ContactScreen(
    padding: PaddingValues,
) {
    val contacts = listOf(
        Contact("Alex Johnson", "Online"),
        Contact("Jamie Lee", "Last seen 5m ago"),
        Contact("Chris Kim", "Mobile"),
        Contact("Taylor Brooks", "Offline")
    )

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 12.dp,
        ),
        overscrollEffect = null,
    ) {
        item {
            SmallTitle(text = "People")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                contacts.forEach { contact ->
                    BasicComponent(
                        title = contact.name,
                        summary = contact.subtitle,
                    )
                }
            }
        }
    }
}
