package com.bjwag.mensaminus.utils

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log

// thanks to kiliankoe for his open source e-meal kit
// i don't even know what is happening here

object CardReader {
    private const val TAG = "CardReader"
    private const val FILE_ID = 0x01.toByte()

    fun readBalance(tag: Tag): Double? {
        val isoDep = IsoDep.get(tag) ?: return null
        
        try {
            isoDep.connect()
            isoDep.timeout = 2000
            
            // 1. Select Application (AID 5F 84 15)
            // secret handshake
            val selectCmd = byteArrayOf(
                0x90.toByte(), 0x5A.toByte(), 0x00, 0x00, 0x03, 
                0x5F, 0x84.toByte(), 0x15.toByte(), 0x00
            )
            val selectRes = isoDep.transceive(selectCmd)

            if (isSuccess(selectRes)) {
                // 2. Get Value (Command 0x6C)
                val getValueCmd = byteArrayOf(
                    0x90.toByte(), 0x6C.toByte(), 0x00, 0x00, 0x01, 
                    FILE_ID, 0x00
                )
                val valueRes = isoDep.transceive(getValueCmd)

                if (valueRes.size >= 4) {
                    val rawValue = (valueRes[0].toInt() and 0xFF) or
                                   ((valueRes[1].toInt() and 0xFF) shl 8) or
                                   ((valueRes[2].toInt() and 0xFF) shl 16) or
                                   ((valueRes[3].toInt() and 0xFF) shl 24)
                    
                    return rawValue.toDouble() / 1000.0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "NFC read failed", e)
        } finally {
            try { isoDep.close() } catch (e: Exception) {}
        }
        return null
    }

    private fun isSuccess(res: ByteArray): Boolean {
        if (res.size < 2) return false
        val sw1 = res[res.size - 2]
        val sw2 = res.last()
        return (sw1 == 0x90.toByte() && sw2 == 0x00.toByte()) || 
               (sw1 == 0x91.toByte() && sw2 == 0x00.toByte())
    }
}
