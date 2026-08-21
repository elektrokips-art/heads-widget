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
 * Anker Soundcore earbuds protocol over classic Bluetooth RFCOMM. Connects via SDP + a
 * per-model service UUID (see [SoundcoreModels]) using the standard, public
 * `createInsecureRfcommSocketToServiceRecord(UUID)` API -- the same proven approach as
 * Sony/BBK elsewhere in this app, and (confirmed by decompiling the official Soundcore app)
 * what it actually uses itself.
 *
 * An earlier version of this file tried a raw RFCOMM *channel number* instead (ported from the
 * CoreSound project, a Linux/BlueZ tool where SDP wasn't reliably queryable) -- a real HCI log
 * from a Soundcore C40i showed every candidate channel getting rejected outright with an
 * RFCOMM DM response, i.e. that technique doesn't map cleanly onto Android's stack. Standard
 * SDP+UUID is both simpler and matches what the vendor's own app does.
 *
 * Packet framing read from the CoreSound project (https://github.com/CriticalRange/CoreSound,
 * GPL-3.0) -- same attribution note as the other protocol sources: a from-scratch Kotlin
 * re-implementation of the wire format, not copied code.
 *
 * Frame format (host -> device): 08 EE 00 00 00 [category][type][totalLen:2LE][payload][checksum]
 * Frame format (device -> host): 09 FF 00 00 01 [category][type][totalLen:2LE][payload][checksum]
 * totalLen = 10 + payload.size (counts every byte from the leading 08/09 through the checksum).
 * checksum = sum of all preceding bytes in the frame, mod 256.
 */
object SoundcoreConnection {
    private const val TAG = "SoundcoreConnection"
    private const val CONNECT_TIMEOUT_MS = 8000L

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
     * Blocking; must be called off the main thread. [deviceName] picks the model-specific UUID
     * (see [SoundcoreModels]) -- null if unrecognized, in which case this doesn't attempt a
     * connection at all. Sends [probe] once connected, passes the reply to [block] along with
     * the open streams (mirrors [SonyHeadphonesSession.withSession]'s shape). A single watchdog
     * covers the whole attempt -- connect, probe write, and whatever [block] does -- since a
     * connection that succeeds but never speaks the protocol would otherwise block a read()
     * call forever with nothing able to interrupt it.
     */
    fun <T> withConnection(
        device: BluetoothDevice,
        deviceName: String,
        probe: ByteArray,
        block: (InputStream, OutputStream, Frame) -> T?
    ): T? {
        val uuid = SoundcoreModels.uuidForDeviceName(deviceName) ?: return null

        var socket: BluetoothSocket? = null
        val watchdogHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            Log.w(TAG, "Timed out talking to $deviceName ($uuid), closing socket")
            try {
                socket?.close()
            } catch (e: IOException) {
                // already closing/closed
            }
        }

        return try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
            watchdogHandler.postDelayed(timeoutRunnable, CONNECT_TIMEOUT_MS)
            socket.connect()

            socket.outputStream.write(probe)
            socket.outputStream.flush()
            val confirmFrame = readFrame(socket.inputStream, CONNECT_TIMEOUT_MS)
                ?: throw IOException("No response to probe")

            val result = block(socket.inputStream, socket.outputStream, confirmFrame)
            watchdogHandler.removeCallbacks(timeoutRunnable)
            socket.close()
            result
        } catch (e: IOException) {
            Log.w(TAG, "Soundcore connection attempt failed for $deviceName: ${e.message}")
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
}
