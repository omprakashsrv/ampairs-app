package com.ampairs.app

import android.app.Application
import initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidContext(this@MainApp)
            androidLogger()
        }
        initKoin(koinApplication)
    }

}