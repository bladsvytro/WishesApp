package com.wishesapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wishesapp.widget.WishWidgetWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WishesApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule a periodic widget refresh every 30 min.
        // Handles both modes: in LATEST mode it fetches the newest wish from Firestore;
        // in RANDOM mode it rotates to a new random wish.
        // This also acts as the background refresh when the app is closed and no FCM
        // server-side function is set up yet.
        WishWidgetWorker.schedulePeriodic(this)
    }
}
