package com.neoncoreng.bukkar

import android.app.Application
import android.content.Context


class BukkarApplication : Application() {
    init {
        app = this
    }

    companion object {
        private lateinit var app: BukkarApplication
        fun getAppContext(): Context =
            app.applicationContext
    }
}