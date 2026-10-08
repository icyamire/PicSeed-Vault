package com.icyamire.valut.crypto

object Ripemd160 {

    fun digest(input: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()

        // Pre-processing / Padding
        val bitLen = input.size.toLong() * 8
        val padLen = if (input.size % 64 < 56) 56 - (input.size % 64) else 120 - (input.size % 64)
        val padded = ByteArray(input.size + padLen + 8)
        System.arraycopy(input, 0, padded, 0, input.size)
        padded[input.size] = 0x80.toByte()

        // Append 64-bit length in Little Endian
        for (i in 0 until 8) {
            padded[padded.size - 8 + i] = ((bitLen ushr (i * 8)) and 0xFF).toByte()
        }

        // Process in 512-bit (64-byte) chunks
        val x = IntArray(16)
        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until 16) {
                x[i] = (padded[offset + i * 4].toInt() and 0xFF) or
                        ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 8) or
                        ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 16) or
                        ((padded[offset + i * 4 + 3].toInt() and 0xFF) shl 24)
            }

            var al = h0; var bl = h1; var cl = h2; var dl = h3; var el = h4
            var ar = h0; var br = h1; var cr = h2; var dr = h3; var er = h4

            // 80 rounds
            for (j in 0 until 80) {
                // Left round
                var t = (al + f(j, bl, cl, dl) + x[R[j]] + K[j / 16])
                t = rol(t, S[j]) + el
                al = el; el = dl; dl = rol(cl, 10); cl = bl; bl = t

                // Right round
                t = (ar + f(79 - j, br, cr, dr) + x[RP[j]] + KP[j / 16])
                t = rol(t, SP[j]) + er
                ar = er; er = dr; dr = rol(cr, 10); cr = br; br = t
            }

            val t = h1 + cl + dr
            h1 = h2 + dl + er
            h2 = h3 + el + ar
            h3 = h4 + al + br
            h4 = h0 + bl + cr
            h0 = t

            offset += 64
        }

        val result = ByteArray(20)
        putIntLE(result, 0, h0)
        putIntLE(result, 4, h1)
        putIntLE(result, 8, h2)
        putIntLE(result, 3 * 4, h3)
        putIntLE(result, 4 * 4, h4)
        return result
    }

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when (j / 16) {
        0 -> x xor y xor z
        1 -> (x and y) or (x.inv() and z)
        2 -> (x or y.inv()) xor z
        3 -> (x and z) or (y and z.inv())
        4 -> x xor (y or z.inv())
        else -> 0
    }

    private fun rol(value: Int, bits: Int): Int = (value shl bits) or (value ushr (32 - bits))

    private fun putIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private val K = intArrayOf(0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC.toInt(), 0xA953FD4E.toInt())
    private val KP = intArrayOf(0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000)

    private val R = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
        4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13
    )

    private val RP = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
        12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11
    )

    private val S = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
        9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6
    )

    private val SP = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
        8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11
    )
}
