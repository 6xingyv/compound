package com.mocharealm.compound

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import org.koin.android.ext.android.inject

class App : Application() {
    private val tdLibDataSource: TdLibDataSource by inject()

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                tdLibDataSource.setBackgroundActive(false)
            }

            override fun onStop(owner: LifecycleOwner) {
                tdLibDataSource.setBackgroundActive(true)
            }
        })

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        }
    }
}