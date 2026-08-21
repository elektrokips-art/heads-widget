package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice

/** Battery query over [SonyHeadphonesSession] -- see that file for protocol/attribution notes. */
object SonyBatteryProtocol {
    private const val PAYLOAD_BATTERY_REQUEST: Byte = 0x22
    private const val PAYLOAD_BATTERY_REPLY: Byte = 0x23
    private const val PAYLOAD_BATTERY_NOTIFY: Byte = 0x25

    private const val BATTERY_TYPE_DUAL: Byte = 0x09
    private const val BATTERY_TYPE_CASE: Byte = 0x0a

    private val EXPECTED_REPLY_TYPES = setOf(PAYLOAD_BATTERY_REPLY, PAYLOAD_BATTERY_NOTIFY)

    data class Result(val left: Int?, val right: Int?, val case: Int?)

    /** Blocking; must be called off the main thread. */
    fun read(device: BluetoothDevice, timeoutMs: Long = 8000): Result? {
        return SonyHeadphonesSession.withSession(device, timeoutMs) { session ->
            val dualPayload = session.request(byteArrayOf(PAYLOAD_BATTERY_REQUEST, BATTERY_TYPE_DUAL), EXPECTED_REPLY_TYPES)
                ?: return@withSession null
            val (left, right) = parseDualBattery(dualPayload)

            val casePayload = session.request(byteArrayOf(PAYLOAD_BATTERY_REQUEST, BATTERY_TYPE_CASE), EXPECTED_REPLY_TYPES)
            val case = casePayload?.let { parseSingleBattery(it) }

            Result(left, right, case)
        }
    }

    private fun parseDualBattery(payload: ByteArray): Pair<Int?, Int?> {
        if (payload.size < 5) return Pair(null, null)
        val left = (payload[2].toInt() and 0xFF).takeIf { it in 0..100 }
        val right = (payload[4].toInt() and 0xFF).takeIf { it in 0..100 }
        return Pair(left, right)
    }

    private fun parseSingleBattery(payload: ByteArray): Int? {
        if (payload.size < 3) return null
        return (payload[2].toInt() and 0xFF).takeIf { it in 0..100 }
    }
}
