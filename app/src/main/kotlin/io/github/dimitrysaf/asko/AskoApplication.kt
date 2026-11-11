package io.github.dimitrysaf.asko

import android.app.Application
import io.github.dimitrysaf.asko.lib.di.AppContainer

class AskoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
