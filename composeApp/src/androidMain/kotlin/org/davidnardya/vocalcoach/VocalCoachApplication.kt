package org.davidnardya.vocalcoach

import android.app.Application
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

class VocalCoachApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatformContext.init(this)
    }
}
