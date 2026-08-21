package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Fallback poll for when Bluetooth broadcasts are missed; interval is user-configurable
 *  but Android/WorkManager enforces a 15-minute floor regardless of what's requested. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val address = WidgetPrefs.deviceAddress(appContext) ?: return Result.success()
        val name = WidgetPrefs.deviceName(appContext) ?: return Result.success()
        val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val device = adapter?.bondedDevices?.firstOrNull { it.address == address } ?: return Result.success()

        BatteryPoller.pollAndSave(appContext, device, name, WidgetPrefs.isConnected(appContext))
        return Result.success()
    }
}

object WidgetRefreshScheduler {
    private const val WORK_NAME = "linkbuds_widget_refresh"
    private const val MIN_INTERVAL_MIN = 15L

    fun reschedule(context: Context, intervalMinutes: Int) {
        val clamped = intervalMinutes.toLong().coerceAtLeast(MIN_INTERVAL_MIN)
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(clamped, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
