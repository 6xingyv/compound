package com.mocharealm.compound.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.NameLength
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository

val LocalPersonNameFormatter = staticCompositionLocalOf<PersonNameFormatterRepository> {
    error("No PersonNameFormatterRepository provided")
}

@Composable
fun User.formatName(length: NameLength = NameLength.MEDIUM): String {
    return LocalPersonNameFormatter.current.formatName(this, length)
}
