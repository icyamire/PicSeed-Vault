package com.icyamire.valut.crypto

object Bech32 {

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    fun encode(hrp: String, witnessVersion: Int, witnessProgram: ByteArray): String {
        val converted = convertBits(witnessProgram, 8, 5, true)
        val data = ByteArray(1 + converted.size)
        data[0] = witnessVersion.toByte()
        System.arraycopy(converted, 0, data, 1, converted.size)

        val checksum = createChecksum(hrp, data)
        val combined = ByteArray(data.size + checksum.size)
        System.arraycopy(data, 0, combined, 0, data.size)
        System.arraycopy(checksum, 0, combined, data.size, checksum.size)

        val sb = StringBuilder(hrp).append("1")
        for (b in combined) {
            sb.append(CHARSET[b.toInt() and 0x1F])
        }
        return sb.toString()
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        for (b in values) {
            val v = b.toInt() and 0xFF
            val top = chk ushr 25
            chk = ((chk and 0x1FFFFFF) shl 5) xor v
            if ((top and 1) != 0) chk = chk xor 0x3b6a57b2
            if ((top and 2) != 0) chk = chk xor 0x26508e6d
            if ((top and 4) != 0) chk = chk xor 0x1ea119fa
            if ((top and 8) != 0) chk = chk xor 0x3d4233dd
            if ((top and 16) != 0) chk = chk xor 0x2a1462b3
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val len = hrp.length
        val ret = ByteArray(len * 2 + 1)
        for (i in 0 until len) {
            val c = hrp[i].code
            ret[i] = (c ushr 5).toByte()
            ret[i + len + 1] = (c and 31).toByte()
        }
        ret[len] = 0
        return ret
    }

    private fun createChecksum(hrp: String, data: ByteArray): ByteArray {
        val hrpExpanded = hrpExpand(hrp)
        val values = ByteArray(hrpExpanded.size + data.size + 6)
        System.arraycopy(hrpExpanded, 0, values, 0, hrpExpanded.size)
        System.arraycopy(data, 0, values, hrpExpanded.size, data.size)

        val mod = polymod(values) xor 1
        val ret = ByteArray(6)
        for (i in 0 until 6) {
            ret[i] = ((mod ushr (5 * (5 - i))) and 31).toByte()
        }
        return ret
    }

    fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val out = ArrayList<Byte>()

        for (b in data) {
            val value = b.toInt() and 0xFF
            acc = (acc shl fromBits) or value
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.add(((acc ushr bits) and maxv).toByte())
            }
        }

        if (pad) {
            if (bits > 0) {
                out.add(((acc shl (toBits - bits)) and maxv).toByte())
            }
        } else {
            require(bits < fromBits && ((acc shl (fromBits - bits)) and maxv) == 0) {
                "Invalid padding"
            }
        }

        val res = ByteArray(out.size)
        for (i in out.indices) res[i] = out[i]
        return res
    }
}
