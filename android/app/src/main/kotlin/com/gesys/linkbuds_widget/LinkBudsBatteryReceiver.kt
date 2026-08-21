package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val ACTION_REFRESH = "com.gesys.linkbuds_widget.ACTION_REFRESH"
const val ACTION_SET_ANC = "com.gesys.linkbuds_widget.ACTION_SET_ANC"
const val EXTRA_ANC_MODE = "anc_mode"
private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

/**
 * Manifest-registered so the widget keeps updating even when the app process is dead.
 * ACL_CONNECTED/ACL_DISCONNECTED are documented exceptions to Android 8+ implicit-broadcast
 * restrictions; BATTERY_LEVEL_CHANGED is an undocumented hidden action, registered as a
 * best-effort extra trigger to re-poll -- not relied on alone. The widget's own periodic
 * update (see linkbuds_widget_info.xml) and its manual refresh button cover the rest.
 */
class LinkBudsBatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val savedAddress = WidgetPrefs.deviceAddress(appContext) ?: return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = deviceExtra(intent) ?: return
                if (device.address != savedAddress) return
                // Right after reconnecting, the earbuds' control channel (RFCOMM) can take a
                // moment to become available even though the audio profile is already up --
                // this is the exact "have to reopen the app after a drop" annoyance with
                // Sony's own app. One retry after a short pause covers it.
                pollInBackground(appContext, device, connected = true, allowRetry = true)
            }

            ACTION_BATTERY_LEVEL_CHANGED -> {
                val device = deviceExtra(intent) ?: return
                if (device.address != savedAddress) return
                pollInBackground(appContext, device, connected = true)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = deviceExtra(intent) ?: return
                if (device.address != savedAddress) return
                WidgetPrefs.setConnected(appContext, false)
                LinkBudsWidgetProvider.refreshAll(appContext)
            }

            ACTION_REFRESH -> {
                val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                val device = adapter?.bondedDevices?.firstOrNull { it.address == savedAddress } ?: return
                pollInBackground(appContext, device, connected = WidgetPrefs.isConnected(appContext))
            }

            ACTION_SET_ANC -> {
                val modeStr = intent.getStringExtra(EXTRA_ANC_MODE) ?: return
                val mode = try {
                    AncMode.valueOf(modeStr)
                } catch (e: IllegalArgumentException) {
                    return
                }
                val deviceName = WidgetPrefs.deviceName(appContext) ?: return
                val source = AncControlSources.forDeviceName(deviceName) ?: return
                val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                val device = adapter?.bondedDevices?.firstOrNull { it.address == savedAddress } ?: return

                val pending = goAsync()
                Thread {
                    try {
                        if (source.setMode(device, mode)) {
                            WidgetPrefs.saveAncMode(appContext, mode)
                            LinkBudsWidgetProvider.refreshAll(appContext)
                        }
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }

    private fun deviceExtra(intent: Intent): BluetoothDevice? {
        @Suppress("DEPRECATION")
        return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

    private fun pollInBackground(context: Context, device: BluetoothDevice, connected: Boolean, allowRetry: Boolean = false) {
        val deviceName = WidgetPrefs.deviceName(context) ?: device.name ?: return
        val pending = goAsync()
        Thread {
            try {
                val result = BatteryPoller.pollAndSave(context, device, deviceName, connected)
                if (allowRetry && result.isEmpty()) {
                    Thread.sleep(3000)
                    BatteryPoller.pollAndSave(context, device, deviceName, connected)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
