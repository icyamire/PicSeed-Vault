package com.icyamire.valut.crypto

object Keccak256 {

    private const val RATE_BYTES = 136

    private val ROUND_CONSTANTS = ulongArrayOf(
        0x0000000000000001UL, 0x0000000000008082UL, 0x800000000000808AUL, 0x8000000080008000UL,
        0x000000000000808BUL, 0x0000000080000001UL, 0x8000000080008081UL, 0x8000000000008009UL,
        0x000000000000008AUL, 0x0000000000000088UL, 0x0000000080008009UL, 0x000000008000000AUL,
        0x000000008000808BUL, 0x800000000000008BUL, 0x8000000000008089UL, 0x8000000000008003UL,
        0x8000000000008002UL, 0x8000000000000080UL, 0x000000000000800AUL, 0x800000008000000AUL,
        0x8000000080008081UL, 0x8000000000008080UL, 0x0000000080000001UL, 0x8000000080008008UL
    )

    private val RHO_OFFSETS = IntArray(25).apply {
        var x = 1
        var y = 0
        for (t in 0 until 24) {
            this[x + 5 * y] = ((t + 1) * (t + 2) / 2) % 64
            val nextX = y
            val nextY = (2 * x + 3 * y) % 5
            x = nextX
            y = nextY
        }
    }

    fun digest(input: ByteArray): ByteArray {
        val state = ULongArray(25)

        // Padding: input || 0x01 || 0x00...0x00 || 0x80
        val inputLen = input.size
        val paddedLen = ((inputLen + 1 + RATE_BYTES - 1) / RATE_BYTES) * RATE_BYTES
        val padded = ByteArray(if (paddedLen == inputLen) inputLen + RATE_BYTES else paddedLen)
        System.arraycopy(input, 0, padded, 0, inputLen)
        padded[inputLen] = 0x01.toByte() // Keccak domain separator
        padded[padded.size - 1] = (padded[padded.size - 1].toInt() or 0x80).toByte()

        // Absorb phase
        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until RATE_BYTES / 8) {
                var lane = 0UL
                for (j in 0 until 8) {
                    lane = lane or ((padded[offset + i * 8 + j].toULong() and 0xFFUL) shl (j * 8))
                }
                state[i] = state[i] xor lane
            }
            keccakF1600(state)
            offset += RATE_BYTES
        }

        // Squeeze phase (32 bytes = 4 64-bit words)
        val result = ByteArray(32)
        for (i in 0 until 4) {
            val lane = state[i]
            for (j in 0 until 8) {
                result[i * 8 + j] = ((lane shr (j * 8)) and 0xFFUL).toByte()
            }
        }
        return result
    }

    private fun keccakF1600(state: ULongArray) {
        val c = ULongArray(5)
        val d = ULongArray(5)
        val b = ULongArray(25)

        for (round in 0 until 24) {
            // Theta
            for (x in 0 until 5) {
                c[x] = state[x] xor state[x + 5] xor state[x + 10] xor state[x + 15] xor state[x + 20]
            }
            for (x in 0 until 5) {
                d[x] = c[(x + 4) % 5] xor rotateLeft(c[(x + 1) % 5], 1)
            }
            for (x in 0 until 5) {
                for (y in 0 until 5) {
                    state[x + y * 5] = state[x + y * 5] xor d[x]
                }
            }

            // Rho and Pi
            for (x in 0 until 5) {
                for (y in 0 until 5) {
                    val index = x + y * 5
                    val rot = RHO_OFFSETS[index]
                    b[y + ((2 * x + 3 * y) % 5) * 5] = rotateLeft(state[index], rot)
                }
            }

            // Chi
            for (x in 0 until 5) {
                for (y in 0 until 5) {
                    val index = x + y * 5
                    state[index] = b[index] xor (b[(x + 1) % 5 + y * 5].inv() and b[(x + 2) % 5 + y * 5])
                }
            }

            // Iota
            state[0] = state[0] xor ROUND_CONSTANTS[round]
        }
    }

    private fun rotateLeft(value: ULong, shift: Int): ULong {
        val s = shift % 64
        if (s == 0) return value
        return (value shl s) or (value shr (64 - s))
    }
}
