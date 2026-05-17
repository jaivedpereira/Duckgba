package com.duckgba

import android.app.Application
import com.duckgba.data.RomRepository
import com.duckgba.data.SettingsRepository

class DuckgbaApplication : Application() {

    lateinit var romRepository: RomRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        romRepository = RomRepository(this)
        settingsRepository = SettingsRepository(this)
    }

    companion object {
        @Volatile
        private var instance: DuckgbaApplication? = null
        fun get(): DuckgbaApplication = instance!!
    }
}
