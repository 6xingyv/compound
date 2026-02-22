package com.mocharealm.compound.ui.screen.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.Screen
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.gaze.ui.modifier.surface
import com.mocharealm.tci18n.core.tdString
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current

    LaunchedEffect(state.authState) {
        if (state.authState is AuthState.Ready) {
            navigator.replaceAll(Screen.Home)
        }
    }

    val surfaceContainerColor = MiuixTheme.colorScheme.surfaceContainer
    val onSurfaceContainerColor = MiuixTheme.colorScheme.onSurfaceContainer

    val layoutDirection = LocalLayoutDirection.current
    val directionFactor =
        if (layoutDirection == LayoutDirection.Ltr) 1 else -1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
        },
        popupHost = {},
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight(),
            overscrollEffect = null,
        ) {
            item {
                AnimatedContent(
                    state.authState,
                    transitionSpec = {
                        slideInHorizontally { it * directionFactor } + fadeIn() togetherWith slideOutHorizontally { it * -directionFactor } + fadeOut()
                    }
                )
                { authState ->
                    when (authState) {
                        AuthState.WaitingForPhoneNumber,
                        AuthState.WaitingForOtp -> Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AnimatedContent(
                                state.authState,
                                transitionSpec = {
                                    slideInHorizontally { it * directionFactor } + fadeIn() togetherWith slideOutHorizontally { it * -directionFactor } + fadeOut()
                                }
                            ) { authState ->
                                when (authState) {
                                    AuthState.WaitingForPhoneNumber -> Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            tdString("YourNumber"),
                                            style = MiuixTheme.textStyles.title2
                                        )
                                        Text(
                                            tdString("StartText"),
                                            Modifier.alpha(0.6f),
                                            style = MiuixTheme.textStyles.body1
                                        )
                                    }

                                    AuthState.WaitingForOtp -> Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            tdString("YourCode"),
                                            style = MiuixTheme.textStyles.title2
                                        )
                                        Text(
                                            tdString("SentAppCode"),
                                            Modifier.alpha(0.6f),
                                            style = MiuixTheme.textStyles.body1
                                        )
                                    }

                                    else -> {}
                                }
                            }
                            Text(tdString("Country"))
                            Column(
                                Modifier.surface(
                                    color = surfaceContainerColor,
                                    border = BorderStroke(
                                        1.dp,
                                        onSurfaceContainerColor.copy(0.1f)
                                    ),
                                    shape = ContinuousRoundedRectangle(16.dp)
                                )
                            ) {
                                TextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentType = ContentType.PhoneNumber
                                        },
                                    state = viewModel.phone,
                                    outputTransformation = null,
                                    lineLimits = TextFieldLineLimits.SingleLine,
                                    padding = 16.dp,
                                    clipRadius = 0.dp,
                                    activeBackgroundColor = Color.Transparent,
                                    inactiveBackgroundColor = Color.Transparent,
                                    activeBorderSize = 0.dp,
                                    inactiveBorderSize = 0.dp,
                                    textStyle = MiuixTheme.textStyles.body1,
                                    decorator = { innerTextField ->
                                        if (viewModel.phone.text.isEmpty()) {
                                            Box {
                                                innerTextField()
                                                Text(
                                                    tdString(tdString("PhoneNumber")),
                                                    color = LocalContentColor.current.copy(0.4f),
                                                    style = MiuixTheme.textStyles.body1,
                                                )
                                            }
                                        } else innerTextField()
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        autoCorrectEnabled = false,
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next,
                                    ),
                                    onKeyboardAction = KeyboardActionHandler {
                                        if (!state.loading && viewModel.phone.text.isNotBlank()) viewModel.submitPhone()
                                    },
                                    enabled = state.authState == AuthState.WaitingForPhoneNumber
                                )
                                AnimatedVisibility(state.authState == AuthState.WaitingForOtp) {
                                    Spacer(
                                        Modifier
                                            .height(1.dp)
                                            .fillMaxWidth()
                                            .background(onSurfaceContainerColor.copy(0.1f))
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics {
                                                contentType = ContentType.SmsOtpCode
                                            },
                                        state = viewModel.code,
                                        outputTransformation = null,
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        padding = 16.dp,
                                        clipRadius = 0.dp,
                                        activeBackgroundColor = Color.Transparent,
                                        inactiveBackgroundColor = Color.Transparent,
                                        activeBorderSize = 0.dp,
                                        inactiveBorderSize = 0.dp,
                                        textStyle = MiuixTheme.textStyles.body1,
                                        decorator = { innerTextField ->
                                            if (viewModel.code.text.isEmpty()) {
                                                Box {
                                                    innerTextField()
                                                    Text(
                                                        tdString(tdString("Code")),
                                                        color = LocalContentColor.current.copy(
                                                            0.4f
                                                        ),
                                                        style = MiuixTheme.textStyles.body1,
                                                    )
                                                }
                                            } else innerTextField()
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            autoCorrectEnabled = false,
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        onKeyboardAction = KeyboardActionHandler {
                                            if (!state.loading && viewModel.code.text.isNotBlank()) viewModel.submitPhone()
                                        },
                                        enabled = state.authState == AuthState.WaitingForOtp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(
                                    text = if (state.loading) tdString("Loading") else tdString(
                                        "Continue"
                                    ),
                                    onClick = {
                                        if (state.authState == AuthState.WaitingForPhoneNumber) viewModel.submitPhone()
                                        if (state.authState == AuthState.WaitingForOtp) viewModel.submitCode()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.loading && (viewModel.phone.text.isNotBlank() || viewModel.code.text.isNotBlank()),
                                )
                            }
                        }


                        AuthState.WaitingForPassword -> Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    tdString("YourPasswordHeader"),
                                    style = MiuixTheme.textStyles.title2
                                )
                                Text(
                                    tdString("LoginPasswordTextShort"),
                                    Modifier.alpha(0.6f),
                                    style = MiuixTheme.textStyles.body1
                                )
                            }

                            Column(
                                Modifier.surface(
                                    color = surfaceContainerColor,
                                    border = BorderStroke(
                                        1.dp,
                                        onSurfaceContainerColor.copy(0.1f)
                                    ),
                                    shape = ContinuousRoundedRectangle(16.dp)
                                )
                            ) {
                                TextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentType = ContentType.Password
                                        },
                                    state = viewModel.password,
                                    outputTransformation = null,
                                    lineLimits = TextFieldLineLimits.SingleLine,
                                    padding = 16.dp,
                                    clipRadius = 0.dp,
                                    activeBackgroundColor = Color.Transparent,
                                    inactiveBackgroundColor = Color.Transparent,
                                    activeBorderSize = 0.dp,
                                    inactiveBorderSize = 0.dp,
                                    textStyle = MiuixTheme.textStyles.body1,
                                    decorator = { innerTextField ->
                                        if (viewModel.password.text.isEmpty()) {
                                            Box {
                                                innerTextField()
                                                Text(
                                                    tdString("EnterPassword"),
                                                    color = LocalContentColor.current.copy(0.4f),
                                                    style = MiuixTheme.textStyles.body1,
                                                )
                                            }
                                        } else innerTextField()
                                    }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                TextButton(
                                    text = if (state.loading) tdString("Loading") else tdString("Continue"),
                                    onClick = viewModel::submitPassword,
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.loading && viewModel.password.text.isNotBlank(),
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

                        is AuthState.Ready -> {}
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