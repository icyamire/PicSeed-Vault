package com.icyamire.valut.crypto

import java.math.BigInteger

object Ed25519 {

    val P = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19))
    val L = BigInteger.TWO.pow(252).add(BigInteger("27742317777372353535851937790883648493"))

    private val D = BigInteger.valueOf(-121665)
        .multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P)

    val GY = BigInteger.valueOf(4)
        .multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)

    // Calculate Gx from Gy: x^2 = (y^2 - 1) / (d * y^2 + 1)
    val GX = calculateX(GY)

    val G = Point(GX, GY)

    data class Point(val x: BigInteger, val y: BigInteger) {
        companion object {
            val ZERO = Point(BigInteger.ZERO, BigInteger.ONE)
        }
    }

    private fun calculateX(y: BigInteger): BigInteger {
        val u = y.pow(2).subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y.pow(2)).add(BigInteger.ONE).mod(P)
        val xx = u.multiply(v.modInverse(P)).mod(P)

        // x = xx^((p+3)/8) mod p
        val exp = P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8))
        var x = xx.modPow(exp, P)

        if (x.pow(2).subtract(xx).mod(P) != BigInteger.ZERO) {
            val i = BigInteger.TWO.modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)
            x = x.multiply(i).mod(P)
        }

        if (x.testBit(0)) {
            x = P.subtract(x)
        }
        return x
    }

    fun add(p1: Point, p2: Point): Point {
        val numX = p1.x.multiply(p2.y).add(p1.y.multiply(p2.x)).mod(P)
        val denX = BigInteger.ONE.add(D.multiply(p1.x).multiply(p2.x).multiply(p1.y).multiply(p2.y)).mod(P)
        val x3 = numX.multiply(denX.modInverse(P)).mod(P)

        val numY = p1.y.multiply(p2.y).add(p1.x.multiply(p2.x)).mod(P)
        val denY = BigInteger.ONE.subtract(D.multiply(p1.x).multiply(p2.x).multiply(p1.y).multiply(p2.y)).mod(P)
        val y3 = numY.multiply(denY.modInverse(P)).mod(P)

        return Point(x3, y3)
    }

    fun multiply(scalar: BigInteger, point: Point = G): Point {
        var s = scalar.mod(L)
        if (s == BigInteger.ZERO) return Point.ZERO

        var result = Point.ZERO
        var base = point

        while (s > BigInteger.ZERO) {
            if (s.testBit(0)) {
                result = add(result, base)
            }
            base = add(base, base)
            s = s.shiftRight(1)
        }
        return result
    }

    fun encodePoint(point: Point): ByteArray {
        val yBytes = point.y.toByteArrayLE(32)
        if (point.x.testBit(0)) {
            yBytes[31] = (yBytes[31].toInt() or 0x80).toByte()
        }
        return yBytes
    }

    private fun BigInteger.toByteArrayLE(length: Int): ByteArray {
        val bytes = this.toByteArray()
        val result = ByteArray(length)
        val count = minOf(bytes.size, length)
        for (i in 0 until count) {
            result[i] = bytes[bytes.size - 1 - i]
        }
        return result
    }
}
