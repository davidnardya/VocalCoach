package org.davidnardya.vocalcoach.platform

import android.content.Context

object AndroidPlatformContext {
    lateinit var application: Context
        private set

    fun init(context: Context) {
        application = context.applicationContext
    }
}
