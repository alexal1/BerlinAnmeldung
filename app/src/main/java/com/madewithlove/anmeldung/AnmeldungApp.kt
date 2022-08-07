package com.madewithlove.anmeldung

import android.app.Application
import kotlinx.coroutines.Dispatchers

class AnmeldungApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PushManager.createInstance(this)
        NetworkManager.createInstance(PushManager.INSTANCE, Dispatchers.Main, Dispatchers.IO)
    }
}
