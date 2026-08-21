package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Anker Soundcore earbuds protocol over classic Bluetooth RFCOMM. Unlike Sony/BBK, Soundcore
 * doesn't advertise a fixed per-model service UUID discoverable via SDP -- the control channel
 * is a plain RFCOMM channel *number*, and which number varies (multipoint mode can shift it).
 * So this connects directly to a channel (via the hidden BluetoothDevice.createInsecureRfcommSocket(int),
 * same category of non-SDK API as the reflection battery fallback elsewhere in this app) and
 * probes candidate channels until one answers the Soundcore protocol.
 *
 * Framing and channel list read from the CoreSound project
 * (https://github.com/CriticalRange/CoreSound, GPL-3.0), a desktop Soundcore controller that
 * covers many current models (not just the two Gadgetbridge has) -- same attribution note as
 * the other protocol sources: a from-scratch Kotlin re-implementation of the wire format, not
 * copied code.
 *
 * Confirmed working for battery on Sony/BBK (which connect via SDP+UUID, not a raw channel
 * number); the raw-channel technique here failed on a Soundcore C40i across every candidate
 * channel and both socket security modes (uniform "read failed, ret: -1" right after connect)
 * -- likely an Android/OEM Bluetooth stack limitation with the hidden raw-channel APIs rather
 * than a wrong channel, but unconfirmed without a packet capture. Left in as a best-effort
 * attempt; the reflection fallback covers devices where it doesn't pan out.
 *
 * Frame format (host -> device): 08 EE 00 00 00 [category][type][totalLen:2LE][payload][checksum]
 * Frame format (device -> host): 09 FF 00 00 01 [category][type][totalLen:2LE][payload][checksum]
 * totalLen = 10 + payload.size (counts every byte from the leading 08/09 through the checksum).
 * checksum = sum of all preceding bytes in the frame, mod 256.
 */
object SoundcoreConnection {
    private const val TAG = "SoundcoreConnection"

    // Priority order from CoreSound's own channel sweep (it falls back further to 1-30, not
    // mirrored here to keep worst-case connect time bounded).
    private val CANDIDATE_CHANNELS = intArrayOf(15, 16, 17, 19, 20, 12, 13, 14)

    private const val PER_CHANNEL_CONNECT_TIMEOUT_MS = 1500L
    private const val PROBE_RESPONSE_TIMEOUT_MS = 1200L

    data class Frame(val category: Int, val type: Int, val payload: ByteArray)

    fun encodeCommand(category: Int, type: Int, payload: ByteArray): ByteArray {
        val body = ByteArray(9 + payload.size)
        body[0] = 0x08; body[1] = 0xEE.toByte(); body[2] = 0; body[3] = 0; body[4] = 0
        body[5] = category.toByte()
        body[6] = type.toByte()
        val totalLen = 10 + payload.size
        body[7] = (totalLen and 0xFF).toByte()
        body[8] = ((totalLen shr 8) and 0xFF).toByte()
        System.arraycopy(payload, 0, body, 9, payload.size)

        var checksum = 0
        for (b in body) checksum = (checksum + (b.toInt() and 0xFF)) and 0xFF

        return body + checksum.toByte()
    }

    /** Reads and discards bytes until it finds a complete, well-formed device->host frame. */
    fun readFrame(input: InputStream, deadlineMs: Long): Frame? {
        val start = System.currentTimeMillis()
        val header = ByteArray(9)

        while (System.currentTimeMillis() - start < deadlineMs) {
            if (input.read() != 0x09) continue
            header[0] = 0x09
            if (!readFully(input, header, 1, 8)) return null
            if (header[1] != 0xFF.toByte() || header[2] != 0.toByte() || header[3] != 0.toByte() || header[4] != 1.toByte()) {
                continue
            }
            val totalLen = (header[7].toInt() and 0xFF) or ((header[8].toInt() and 0xFF) shl 8)
            if (totalLen < 10) continue

            val rest = ByteArray(totalLen - 9) // payload + checksum
            if (!readFully(input, rest, 0, rest.size)) return null

            val payload = rest.copyOfRange(0, rest.size - 1)
            return Frame(header[5].toInt() and 0xFF, header[6].toInt() and 0xFF, payload)
        }
        return null
    }

    private fun readFully(input: InputStream, out: ByteArray, offset: Int, length: Int): Boolean {
        var read = 0
        while (read < length) {
            val n = input.read(out, offset + read, length - read)
            if (n == -1) return false
            read += n
        }
        return true
    }

    /**
     * Blocking; must be called off the main thread. Tries each candidate channel, confirming
     * it's the Soundcore control channel by sending [probe] and waiting for a frame back --
     * that confirmation frame is itself passed to [block] (as [probe] is always the device-info
     * query, which doubles as a useful response for callers that just want device info; callers
     * that need something else read/write further on the same streams). Always closes the
     * socket afterwards.
     */
    fun <T> withConnection(device: BluetoothDevice, probe: ByteArray, block: (InputStream, OutputStream, Frame) -> T?): T? {
        val createInsecureRfcommSocket = try {
            BluetoothDevice::class.java.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "createInsecureRfcommSocket(int) not available on this OS build", e)
            return null
        }

        for (channel in CANDIDATE_CHANNELS) {
            var socket: BluetoothSocket? = null
            val watchdogHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                try {
                    socket?.close()
                } catch (e: IOException) {
                    // already closing/closed
                }
            }
            try {
                socket = createInsecureRfcommSocket.invoke(device, channel) as BluetoothSocket
                watchdogHandler.postDelayed(timeoutRunnable, PER_CHANNEL_CONNECT_TIMEOUT_MS)
                socket.connect()
                watchdogHandler.removeCallbacks(timeoutRunnable)

                socket.outputStream.write(probe)
                socket.outputStream.flush()
                val confirmFrame = readFrame(socket.inputStream, PROBE_RESPONSE_TIMEOUT_MS)

                if (confirmFrame == null) {
                    Log.w(TAG, "Channel $channel connected but silent, trying next")
                    socket.close()
                    continue
                }

                Log.i(TAG, "Soundcore protocol confirmed on channel $channel")
                val result = block(socket.inputStream, socket.outputStream, confirmFrame)
                socket.close()
                return result
            } catch (e: IOException) {
                Log.w(TAG, "Channel $channel refused: ${e.message}")
                watchdogHandler.removeCallbacks(timeoutRunnable)
                try {
                    socket?.close()
                } catch (closeError: IOException) {
                    // already closed by watchdog
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission", e)
                return null
            } catch (e: java.lang.reflect.InvocationTargetException) {
                Log.w(TAG, "Channel $channel threw on connect: ${e.cause?.message}")
            }
        }
        return null
    }
}
