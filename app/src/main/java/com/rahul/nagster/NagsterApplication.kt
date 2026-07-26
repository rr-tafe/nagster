package com.rahul.nagster

import android.app.Application

class NagsterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NagStore.init(this)
        Notifications.ensureChannel(this)
    }
}
