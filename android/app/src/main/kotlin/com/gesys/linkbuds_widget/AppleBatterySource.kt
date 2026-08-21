package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Apple AirPods/Beats don't expose battery over any connectable protocol at all -- instead
 * they continuously broadcast it in a BLE advertisement ("Proximity Pairing" message,
 * manufacturer ID 0x004C/76), connected or not. So this source just listens passively for a
 * few seconds instead of connecting to anything. Decode table ported from the OpenPods project
 * (https://github.com/adolfintel/OpenPods, GPL-3.0) -- a from-scratch Kotlin re-implementation
 * of the same wire format, not copied code (same note as the Sony protocol).
 *
 * Android hides the real MAC address of BLE advertisers for privacy, so there's no reliable
 * way to confirm a beacon is from *your* AirPods and not a stranger's nearby pair -- OpenPods'
 * workaround (strongest signal above -60dB wins) is used here too.
 */
class AppleBatterySource : BatterySource {
    companion object {
        private const val TAG = "AppleBatterySource"
        private const val MANUFACTURER_ID = 76
        private const val DATA_LENGTH = 27
        private const val MIN_RSSI = -60
        private const val SCAN_TIMEOUT_MS = 6000L
    }

    override fun matches(deviceName: String): Boolean {
        val lower = deviceName.lowercase(Locale.ROOT)
        return lower.contains("airpods") || lower.contains("beats")
    }

    override fun read(context: Context, device: BluetoothDevice): BatteryReading? {
        val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner ?: return null

        val latch = CountDownLatch(1)
        var bestData: ByteArray? = null
        var bestRssi = Int.MIN_VALUE

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID) ?: return
                if (data.size != DATA_LENGTH || data[0] != 7.toByte() || data[1] != 25.toByte()) return
                if (result.rssi < MIN_RSSI || result.rssi <= bestRssi) return
                bestRssi = result.rssi
                bestData = data
                latch.countDown()
            }
        }

        try {
            val filter = ScanFilter.Builder()
                .setManufacturerData(MANUFACTURER_ID, byteArrayOf(7, 25), byteArrayOf(-1, -1))
                .build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(listOf(filter), settings, callback)
            latch.await(SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission", e)
            return null
        } finally {
            try {
                scanner.stopScan(callback)
            } catch (e: SecurityException) {
                // permission revoked mid-scan, nothing left to clean up
            }
        }

        return bestData?.let { parse(it) }
    }

    private fun parse(data: ByteArray): BatteryReading? {
        val hex = data.joinToString("") { String.format(Locale.ROOT, "%02X", it) }
        if (hex.length < 16) return null

        fun hexDigit(index: Int) = Character.digit(hex[index], 16)
        fun toPercent(raw: Int): Int? = if (raw in 0..10) raw * 10 else null

        val flip = (hexDigit(10) and 0x02) == 0
        val left = toPercent(hexDigit(if (flip) 12 else 13))
        val right = toPercent(hexDigit(if (flip) 13 else 12))
        val case = toPercent(hexDigit(15))

        val reading = BatteryReading(left, right, case)
        return if (reading.isEmpty()) null else reading
    }
}
