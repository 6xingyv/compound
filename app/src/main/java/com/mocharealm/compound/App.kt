package com.mocharealm.compound

import android.app.Application
import com.mocharealm.compound.di.dataModule
import com.mocharealm.compound.di.domainModule
import com.mocharealm.compound.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(domainModule, dataModule, uiModule)
            androidContext(this@App)
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        }
    }
}