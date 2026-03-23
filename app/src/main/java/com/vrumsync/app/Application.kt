package com.vrumsync.app

import android.app.Application
import com.vrumsync.app.database.DatabaseBuilder

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        DatabaseBuilder.getInstance(this)
    }
}