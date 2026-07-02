package com.example.phishtrack

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class PhishTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = StringWriter().also {
                throwable.printStackTrace(PrintWriter(it))
            }.toString()

            runCatching {
                filesDir.resolve("last_crash.txt").writeText(
                    "Thread: ${thread.name}\n\n$stackTrace"
                )
            }
            Log.e("PhishTrackCrash", "Uncaught exception on ${thread.name}", throwable)

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
