package com.aiforseniors.scamshield

import android.app.Application

class ScamShieldApp : Application() {
    lateinit var api: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        api = ApiClient(
            baseUrl = BuildConfig.API_BASE_URL,
            deviceToken = BuildConfig.DEVICE_API_TOKEN,
        )
    }
}
