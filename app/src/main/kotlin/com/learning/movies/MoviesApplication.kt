package com.learning.movies

import android.app.Application
import com.learning.movies.data.di.dataModule
import com.learning.movies.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MoviesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MoviesApplication)
            modules(dataModule, presentationModule)
        }
    }
}
