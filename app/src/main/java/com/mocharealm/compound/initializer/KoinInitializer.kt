package com.mocharealm.compound.initializer

import android.content.Context
import androidx.startup.Initializer
import com.mocharealm.compound.di.dataModule
import com.mocharealm.compound.di.domainModule
import com.mocharealm.compound.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KoinInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(domainModule, dataModule, uiModule)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
