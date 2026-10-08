package com.icyamire.valut.crypto

import java.math.BigInteger
import java.security.MessageDigest

object Base58 {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58)

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var zeroes = 0
        while (zeroes < input.size && input[zeroes] == 0.toByte()) {
            zeroes++
        }

        var num = BigInteger(1, input)
        val sb = StringBuilder()

        while (num > BigInteger.ZERO) {
            val mod = num.mod(BASE)
            sb.append(ALPHABET[mod.toInt()])
            num = num.divide(BASE)
        }

        repeat(zeroes) {
            sb.append('1')
        }

        return sb.reverse().toString()
    }

    fun encodeCheck(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val checksum = md.digest(md.digest(data)).copyOfRange(0, 4)
        val combined = ByteArray(data.size + 4)
        System.arraycopy(data, 0, combined, 0, data.size)
        System.arraycopy(checksum, 0, combined, data.size, 4)
        return encode(combined)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        var zeroes = 0
        while (zeroes < input.length && input[zeroes] == '1') {
            zeroes++
        }

        var num = BigInteger.ZERO
        for (i in zeroes until input.length) {
            val charIndex = ALPHABET.indexOf(input[i])
            require(charIndex >= 0) { "Invalid Base58 character '${input[i]}'" }
            num = num.multiply(BASE).add(BigInteger.valueOf(charIndex.toLong()))
        }

        val numBytes = num.toByteArray()
        val stripLeadingZero = numBytes.size > 1 && numBytes[0] == 0.toByte()
        val result = ByteArray(zeroes + numBytes.size - if (stripLeadingZero) 1 else 0)
        System.arraycopy(
            numBytes,
            if (stripLeadingZero) 1 else 0,
            result,
            zeroes,
            numBytes.size - if (stripLeadingZero) 1 else 0
        )
        return result
    }
}
