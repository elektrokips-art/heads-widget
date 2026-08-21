package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothManager
import android.content.Context
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

private const val CHANNEL = "linkbuds_widget/native"

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (bluetoothAdapter == null) {
                result.error("no_adapter", "Device has no Bluetooth adapter", null)
                return@setMethodCallHandler
            }

            when (call.method) {
                "getBondedDevices" -> {
                    try {
                        val devices = bluetoothAdapter.bondedDevices.map { device ->
                            mapOf("name" to (device.name ?: "(no name)"), "address" to device.address)
                        }
                        result.success(devices)
                    } catch (e: SecurityException) {
                        result.error("permission_denied", "BLUETOOTH_CONNECT not granted", null)
                    }
                }

                // Saves the chosen device, then immediately polls both battery sources and
                // updates the home screen widget. Also used for a manual "recheck" -- saving
                // the same device again is harmless.
                "saveSelectedDevice" -> {
                    val address = call.argument<String>("address")
                    val name = call.argument<String>("name")
                    if (address == null || name == null) {
                        result.error("bad_args", "Missing address/name", null)
                        return@setMethodCallHandler
                    }
                    try {
                        val device = bluetoothAdapter.getRemoteDevice(address)
                        WidgetPrefs.saveDevice(applicationContext, address, name)
                        WidgetRefreshScheduler.reschedule(applicationContext, WidgetPrefs.refreshIntervalMin(applicationContext))
                        Thread {
                            val poll = BatteryPoller.pollAndSave(applicationContext, device, name, connected = true)
                            runOnUiThread {
                                result.success(
                                    mapOf(
                                        "left" to poll.left,
                                        "right" to poll.right,
                                        "case" to poll.case,
                                        "fallback" to poll.fallback
                                    )
                                )
                            }
                        }.start()
                    } catch (e: SecurityException) {
                        result.error("permission_denied", "BLUETOOTH_CONNECT not granted", null)
                    } catch (e: IllegalArgumentException) {
                        result.error("bad_args", "Invalid MAC address", null)
                    }
                }

                "getWidgetSettings" -> {
                    result.success(
                        mapOf(
                            "color" to WidgetPrefs.color(applicationContext),
                            "opacityPercent" to WidgetPrefs.opacityPercent(applicationContext),
                            "cornerRadiusDp" to WidgetPrefs.cornerRadiusDp(applicationContext),
                            "refreshIntervalMin" to WidgetPrefs.refreshIntervalMin(applicationContext)
                        )
                    )
                }

                "saveWidgetSettings" -> {
                    // ARGB colors with alpha=FF exceed Int32 range, so Flutter's platform
                    // channel sends them as Int64 -- read as Long and truncate to Int (this
                    // reinterprets the bit pattern correctly as a signed 32-bit color, same
                    // as Kotlin's own 0xFFxxxxxx.toInt() literals).
                    val color = call.argument<Long>("color")?.toInt()
                    val opacityPercent = call.argument<Int>("opacityPercent")
                    val cornerRadiusDp = call.argument<Int>("cornerRadiusDp")
                    val refreshIntervalMin = call.argument<Int>("refreshIntervalMin")
                    if (color == null || opacityPercent == null || cornerRadiusDp == null || refreshIntervalMin == null) {
                        result.error("bad_args", "Missing appearance fields", null)
                        return@setMethodCallHandler
                    }
                    WidgetPrefs.saveAppearance(applicationContext, color, opacityPercent, cornerRadiusDp, refreshIntervalMin)
                    WidgetRefreshScheduler.reschedule(applicationContext, refreshIntervalMin)
                    LinkBudsWidgetProvider.refreshAll(applicationContext)
                    result.success(null)
                }

                // Null result means either the brand isn't recognized (no ANC source matched
                // its name) or the read failed -- Flutter treats both the same: hide the ANC UI.
                "getAncMode" -> {
                    val address = call.argument<String>("address")
                    val name = call.argument<String>("name")
                    if (address == null || name == null) {
                        result.error("bad_args", "Missing address/name", null)
                        return@setMethodCallHandler
                    }
                    val source = AncControlSources.forDeviceName(name)
                    if (source == null) {
                        result.success(null)
                        return@setMethodCallHandler
                    }
                    try {
                        val device = bluetoothAdapter.getRemoteDevice(address)
                        Thread {
                            val mode = source.getMode(device)
                            runOnUiThread { result.success(mode?.name) }
                        }.start()
                    } catch (e: SecurityException) {
                        result.error("permission_denied", "BLUETOOTH_CONNECT not granted", null)
                    } catch (e: IllegalArgumentException) {
                        result.error("bad_args", "Invalid MAC address", null)
                    }
                }

                "setAncMode" -> {
                    val address = call.argument<String>("address")
                    val name = call.argument<String>("name")
                    val modeStr = call.argument<String>("mode")
                    if (address == null || name == null || modeStr == null) {
                        result.error("bad_args", "Missing address/name/mode", null)
                        return@setMethodCallHandler
                    }
                    val mode = try {
                        AncMode.valueOf(modeStr)
                    } catch (e: IllegalArgumentException) {
                        result.error("bad_args", "Invalid mode $modeStr", null)
                        return@setMethodCallHandler
                    }
                    val source = AncControlSources.forDeviceName(name)
                    if (source == null) {
                        result.success(false)
                        return@setMethodCallHandler
                    }
                    try {
                        val device = bluetoothAdapter.getRemoteDevice(address)
                        Thread {
                            val success = source.setMode(device, mode)
                            runOnUiThread { result.success(success) }
                        }.start()
                    } catch (e: SecurityException) {
                        result.error("permission_denied", "BLUETOOTH_CONNECT not granted", null)
                    } catch (e: IllegalArgumentException) {
                        result.error("bad_args", "Invalid MAC address", null)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }
}
