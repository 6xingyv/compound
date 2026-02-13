package com.mocharealm.compound.ui.screen.me

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.ui.composable.Avatar
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperArrow

@Composable
fun MeScreen(
    padding: PaddingValues,
    viewModel: MeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 12.dp,
        ),
        overscrollEffect = null,
    ) {
        if (state.loading) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    BasicComponent(title = "Loading...")
                }
            }
        } else if (state.error != null) {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    BasicComponent(
                        title = "Error",
                        summary = state.error,
                    )
                    TextButton(
                        text = "Retry",
                        onClick = viewModel::loadUser,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        } else if (state.user != null) {
            val user = state.user!!
            item {
                SmallTitle(text = "Account")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    BasicComponent(
                        title = "${user.firstName} ${user.lastName}".trim(),
                        summary = if (user.username.isNotEmpty()) "@${user.username}" else user.phoneNumber,
                        startAction = {
                            Avatar(
                                initials = "${
                                    user.firstName.firstOrNull()?.uppercase() ?: ""
                                }${user.lastName.firstOrNull()?.uppercase() ?: ""}",
                                modifier = Modifier.size(40.dp),
                                photoPath = user.profilePhotoUrl,
                            )
                        },
                    )
                }
            }
            item {
                SmallTitle(text = "Info")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    BasicComponent(
                        title = "User ID",
                        summary = user.id.toString(),
                    )
                    BasicComponent(
                        title = "Phone",
                        summary = user.phoneNumber,
                    )
                    BasicComponent(
                        title = "Account Status",
                        summary = "Active",
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp),
                ) {
                    SuperArrow(
                        title = "Log out",
                        onClick = viewModel::logoutUser,
                    )
                }
            }
        } else {
            item {
                Card(modifier = Modifier.padding(12.dp)) {
                    BasicComponent(title = "No user data")
                    TextButton(
                        text = "Load Profile",
                        onClick = viewModel::loadUser,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
