package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Sony's proprietary earbuds control protocol over classic Bluetooth RFCOMM -- confirmed the
 * standard BLE Battery Service (0x180F) is NOT implemented by LinkBuds S, so battery and ANC
 * control are only available this way. There is no public spec for this; the framing, RFCOMM
 * UUIDs, and message layouts below were read directly from the Gadgetbridge project
 * (https://codeberg.org/Freeyourgadget/Gadgetbridge, AGPL-3.0), which has working, merged
 * support for Sony LinkBuds S. This is a from-scratch Kotlin implementation of the same wire
 * protocol -- not a copy of their Java sources. Note for later: if this app is ever
 * distributed publicly, using a reverse-engineered protocol from an AGPL project is fine
 * (protocols/facts aren't copyrightable), but worth keeping in mind.
 *
 * Shared by [SonyBatteryProtocol] and [SonyAncControl] -- connect + init handshake + message
 * framing is identical for both, only the request/reply payload contents differ.
 */
object SonyHeadphonesSession {
    private const val TAG = "SonyHeadphonesSession"

    private val UUID_V2 = UUID.fromString("956C7B26-D49A-4BA8-B03F-B17D393CB6E2")
    private val UUID_V1 = UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA")

    private const val HEADER: Byte = 0x3e
    private const val TRAILER: Byte = 0x3c
    private const val ESCAPE: Byte = 0x3d
    private const val ESCAPE_MASK: Byte = 0xEF.toByte()

    private const val TYPE_ACK: Byte = 0x01
    private const val TYPE_COMMAND_1: Byte = 0x0c

    private data class RawMessage(val type: Byte, val seq: Byte, val payload: ByteArray)

    /** One open, handshaked connection. Request payloads and replies both start with the
     *  Sony "payload type" byte, matching how Gadgetbridge represents them. */
    class Session internal constructor(private val socket: BluetoothSocket, private var seq: Byte) {
        private val input = socket.inputStream
        private val output = socket.outputStream

        /** Sends [payload] and returns the first reply whose payload[0] is in [expectedTypes],
         *  ACKing everything read along the way (including unrelated notifications). */
        fun request(payload: ByteArray, expectedTypes: Set<Byte>): ByteArray? {
            writeMessage(output, TYPE_COMMAND_1, seq, payload)

            val ack = readMessage(input) ?: return null
            if (ack.type != TYPE_ACK) return null
            seq = ack.seq

            while (true) {
                val reply = readMessage(input) ?: return null
                writeMessage(output, TYPE_ACK, (1 - reply.seq).toByte(), ByteArray(0))
                if (reply.type == TYPE_COMMAND_1 && reply.payload.isNotEmpty() && expectedTypes.contains(reply.payload[0])) {
                    return reply.payload
                }
            }
        }
    }

    /**
     * Blocking; must be called off the main thread. Connects (trying UUID V2 then V1),
     * completes the init handshake, runs [block], and always closes the socket afterwards.
     */
    fun <T> withSession(device: BluetoothDevice, timeoutMs: Long = 8000, block: (Session) -> T?): T? {
        for (uuid in listOf(UUID_V2, UUID_V1)) {
            var socket: BluetoothSocket? = null
            val watchdogHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                Log.w(TAG, "Timed out talking to $uuid, closing socket")
                try {
                    socket?.close()
                } catch (e: IOException) {
                    // already closing/closed
                }
            }
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                watchdogHandler.postDelayed(timeoutRunnable, timeoutMs)
                socket.connect()

                var seq: Byte = 0
                writeMessage(socket.outputStream, TYPE_COMMAND_1, seq, byteArrayOf(0x00, 0x00))

                val ack = readMessage(socket.inputStream) ?: throw IOException("No init ACK")
                if (ack.type != TYPE_ACK) throw IOException("Expected ACK, got ${ack.type}")
                seq = ack.seq

                val initReply = readMessage(socket.inputStream) ?: throw IOException("No init reply")
                if (initReply.type != TYPE_COMMAND_1 || initReply.payload.isEmpty() || initReply.payload[0] != 0x01.toByte()) {
                    throw IOException("Unexpected init reply")
                }
                writeMessage(socket.outputStream, TYPE_ACK, (1 - initReply.seq).toByte(), ByteArray(0))

                val result = block(Session(socket, seq))
                watchdogHandler.removeCallbacks(timeoutRunnable)
                socket.close()
                return result
            } catch (e: IOException) {
                Log.w(TAG, "Sony session attempt failed for $uuid: ${e.message}")
                watchdogHandler.removeCallbacks(timeoutRunnable)
                try {
                    socket?.close()
                } catch (closeError: IOException) {
                    // already closed by watchdog
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission", e)
                return null
            }
        }
        return null
    }

    private fun checksum(bytes: ByteArray, start: Int = 0, end: Int = bytes.size): Byte {
        var sum = 0
        for (i in start until end) sum += bytes[i].toInt() and 0xFF
        return sum.toByte()
    }

    private fun escape(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (b in bytes) {
            when (b) {
                HEADER, TRAILER, ESCAPE -> {
                    out.write(ESCAPE.toInt())
                    out.write((b.toInt() and ESCAPE_MASK.toInt()) and 0xFF)
                }
                else -> out.write(b.toInt())
            }
        }
        return out.toByteArray()
    }

    private fun unescape(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i]
            if (b == ESCAPE) {
                i++
                if (i >= bytes.size) break
                out.write((bytes[i].toInt() or ESCAPE_MASK.toInt().inv()) and 0xFF)
            } else {
                out.write(b.toInt())
            }
            i++
        }
        return out.toByteArray()
    }

    private fun writeMessage(output: OutputStream, type: Byte, seq: Byte, payload: ByteArray) {
        val buf = ByteBuffer.allocate(payload.size + 6)
        buf.order(ByteOrder.BIG_ENDIAN)
        buf.put(type)
        buf.put(seq)
        buf.putInt(payload.size)
        buf.put(payload)
        val body = buf.array()
        val messageChecksum = checksum(body)

        val out = ByteArrayOutputStream()
        out.write(HEADER.toInt())
        out.write(escape(body))
        out.write(escape(byteArrayOf(messageChecksum)))
        out.write(TRAILER.toInt())

        output.write(out.toByteArray())
        output.flush()
    }

    private fun readMessage(input: InputStream): RawMessage? {
        val buf = ByteArrayOutputStream()
        var b = input.read()
        while (b != -1) {
            if (b.toByte() == HEADER) buf.reset()
            buf.write(b)
            if (b.toByte() == TRAILER) break
            b = input.read()
        }
        if (b == -1) return null

        val raw = buf.toByteArray()
        if (raw.isEmpty() || raw[0] != HEADER || raw[raw.size - 1] != TRAILER) return null

        val unescaped = unescape(raw)
        if (unescaped.size < 9) return null

        val checksumIndex = unescaped.size - 2
        val expectedChecksum = checksum(unescaped, 1, checksumIndex)
        if (unescaped[checksumIndex] != expectedChecksum) {
            Log.w(TAG, "Checksum mismatch")
            return null
        }

        val payloadLength = ((unescaped[3].toInt() and 0xFF) shl 24) or
            ((unescaped[4].toInt() and 0xFF) shl 16) or
            ((unescaped[5].toInt() and 0xFF) shl 8) or
            (unescaped[6].toInt() and 0xFF)

        if (payloadLength != checksumIndex - 7) return null

        val type = unescaped[1]
        val seq = unescaped[2]
        val payload = unescaped.copyOfRange(7, 7 + payloadLength)

        return RawMessage(type, seq, payload)
    }
}
