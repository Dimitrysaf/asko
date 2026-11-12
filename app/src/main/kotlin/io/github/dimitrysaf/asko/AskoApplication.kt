package io.github.dimitrysaf.asko

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import io.github.dimitrysaf.asko.lib.di.AppContainer
import io.github.dimitrysaf.asko.lib.share.ImageLoads

class AskoApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Coil's usual loader, counting the images on their way (a share image waits for them). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).eventListener(ImageLoads).build()
}
