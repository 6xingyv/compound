package com.mocharealm.compound.ui.screen.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.Screen
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.tci18n.core.tdString
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField

@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(state.authState) {
        if (state.authState is AuthState.Ready) {
            navigator.replaceAll(Screen.Home)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
        },
        popupHost = {},
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 12.dp,
            ),
            overscrollEffect = null,
        ) {
            item {
                when (state.authState) {
                    AuthState.WaitingForPhoneNumber -> {
                        SmallTitle(text = tdString("PhoneNumber"))
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = state.phone,
                                    onValueChange = viewModel::onPhoneChange,
                                    label = "+86 123 4567 8901",
                                    useLabelAsPlaceholder = true
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TextButton(
                                text = if (state.loading) tdString("Loading") else tdString("Continue"),
                                onClick = viewModel::submitPhone,
                                modifier = Modifier.weight(1f),
                                enabled = !state.loading && state.phone.isNotBlank(),
                            )
                        }
                    }

                    AuthState.WaitingForOtp -> {
                        SmallTitle(text = tdString("VerificationCode"))
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = state.code,
                                    onValueChange = viewModel::onCodeChange,
                                    label = "123456",
                                    useLabelAsPlaceholder = true
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TextButton(
                                text = if (state.loading) tdString("Loading") else tdString("Continue"),
                                onClick = viewModel::submitCode,
                                modifier = Modifier.weight(1f),
                                enabled = !state.loading && state.code.isNotBlank(),
                            )
                        }
                    }

                    AuthState.WaitingForPassword -> {
                        SmallTitle(text = tdString("YourPassword"))
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = state.password,
                                    onValueChange = viewModel::onPasswordChange,
                                    label = tdString("YourPassword"),
                                    useLabelAsPlaceholder = true
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TextButton(
                                text = if (state.loading) tdString("Loading") else tdString("Continue"),
                                onClick = viewModel::submitPassword,
                                modifier = Modifier.weight(1f),
                                enabled = !state.loading && state.password.isNotBlank(),
                            )
                        }
                    }

                    is AuthState.Ready -> {
                        val user = (state.authState as AuthState.Ready).user
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            BasicComponent(
                                title = "${user.firstName} ${user.lastName}".trim(),
                                summary = if (user.username.isNotEmpty()) "@${user.username}" else null,
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

                    is AuthState.Error -> {
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            BasicComponent(
                                title = tdString("ErrorOccurred"),
                                summary = (state.authState as AuthState.Error).message,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TextButton(
                                text = tdString("Retry"),
                                onClick = viewModel::refreshAuthState,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            state.error?.let { errorMsg ->
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    ) {
                        BasicComponent(
                            title = tdString("ErrorOccurred"),
                            summary = errorMsg,
                        )
                    }
                }
            }
        }
    }
}