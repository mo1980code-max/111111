package com.smartclean.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Smart Clean AI - Main Application Entry Point
 * Initializes Hilt Dependency Injection container & system monitoring
 */
@HiltAndroidApp
class SmartCleanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Global application initialization
    }
}
