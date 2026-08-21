package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * OPPO/OnePlus/Realme (all BBK Electronics) earbuds protocol over classic Bluetooth RFCOMM.
 * Much simpler than Sony's: no handshake, just connect and send a request. Framing, UUID, and
 * battery command codes read from the Gadgetbridge project (AGPL-3.0), which has confirmed,
 * merged support for OPPO Enco Air/Air2 and Realme Buds T110 using this exact protocol --
 * same attribution note as the Sony/Apple sources: a from-scratch Kotlin re-implementation of
 * the wire format, not copied code. OnePlus and vivo aren't in Gadgetbridge's supported list,
 * so matching their device names to this protocol (see [BbkBatterySource]) is an educated
 * guess based on shared BBK hardware lineage, not a verified fact -- if wrong, it just fails
 * and falls back to the reflection API, same as any other unsupported device.
 */
object BbkBatteryProtocol {
    private const val TAG = "BbkBatteryProtocol"
    private val UUID_BBK = UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5")

    private const val PREAMBLE: Byte = 0xAA.toByte()
    private const val CMD_BATTERY_REQ: Short = 0x0106
    private const val CMD_BATTERY_RET: Short = 0x8106.toShort()

    data class Result(val left: Int?, val right: Int?, val case: Int?)

    private data class RawMessage(val command: Short, val payload: ByteArray)

    fun read(device: BluetoothDevice, timeoutMs: Long = 8000): Result? {
        var socket: BluetoothSocket? = null
        val watchdogHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            Log.w(TAG, "Timed out talking to ${device.address}, closing socket")
            try {
                socket?.close()
            } catch (e: IOException) {
                // already closing/closed
            }
        }

        return try {
            socket = device.createRfcommSocketToServiceRecord(UUID_BBK)
            watchdogHandler.postDelayed(timeoutRunnable, timeoutMs)
            socket.connect()

            socket.outputStream.write(encodeMessage(CMD_BATTERY_REQ, ByteArray(0), seq = 0))
            socket.outputStream.flush()

            val result = readUntilBatteryReply(socket.inputStream)
            watchdogHandler.removeCallbacks(timeoutRunnable)
            socket.close()
            result
        } catch (e: IOException) {
            Log.w(TAG, "BBK protocol attempt failed: ${e.message}")
            watchdogHandler.removeCallbacks(timeoutRunnable)
            try {
                socket?.close()
            } catch (closeError: IOException) {
                // already closed by watchdog
            }
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission", e)
            null
        }
    }

    private fun encodeMessage(command: Short, payload: ByteArray, seq: Int): ByteArray {
        val buf = ByteBuffer.allocate(9 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(PREAMBLE)
        buf.put((9 + payload.size - 2).toByte())
        buf.put(0)
        buf.put(0)
        buf.putShort(command)
        buf.put(seq.toByte())
        buf.putShort(payload.size.toShort())
        buf.put(payload)
        return buf.array()
    }

    /** Skips any non-battery replies (e.g. unsolicited status) until it finds the one we want. */
    private fun readUntilBatteryReply(input: InputStream): Result? {
        while (true) {
            val message = readMessage(input) ?: return null
            if (message.command == CMD_BATTERY_RET) {
                return parseBattery(message.payload)
            }
        }
    }

    private fun readMessage(input: InputStream): RawMessage? {
        val preambleByte = input.read()
        if (preambleByte == -1 || preambleByte.toByte() != PREAMBLE) return null

        val totalLength = input.read()
        if (totalLength == -1) return null

        val body = ByteArray(totalLength)
        var readSoFar = 0
        while (readSoFar < totalLength) {
            val n = input.read(body, readSoFar, totalLength - readSoFar)
            if (n == -1) return null
            readSoFar += n
        }

        // body = [zero(2)][command(2)][seq(1)][payloadLength(2)][payload...]
        if (body.size < 7) return null
        val buf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
        buf.short // "zero" field (0 on Oppo, 4 on Realme per Gadgetbridge notes) -- unused
        val command = buf.short
        buf.get() // seq, unused for a one-shot request
        val payloadLength = buf.short.toInt() and 0xFFFF
        if (payloadLength > buf.remaining()) return null
        val payload = ByteArray(payloadLength)
        buf.get(payload)

        return RawMessage(command, payload)
    }

    private fun parseBattery(payload: ByteArray): Result? {
        if (payload.size < 2) return null
        var left: Int? = null
        var right: Int? = null
        var case: Int? = null

        var i = 2
        while (i + 1 < payload.size) {
            val indexByte = payload[i].toInt() and 0xFF
            if (indexByte != 0xFF) {
                val level = payload[i + 1].toInt() and 0x7F
                when (indexByte - 1) {
                    0 -> left = level
                    1 -> right = level
                    2 -> case = level
                }
            }
            i += 2
        }

        return if (left == null && right == null && case == null) null else Result(left, right, case)
    }
}
