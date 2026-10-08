package com.icyamire.valut.crypto

import java.math.BigInteger

object Secp256k1 {

    val P = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    val N = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)

    val GX = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
    val GY = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)

    val G = ECPoint(GX, GY)

    data class ECPoint(val x: BigInteger, val y: BigInteger) {
        val isInfinity: Boolean
            get() = x == BigInteger.ZERO && y == BigInteger.ZERO

        companion object {
            val INFINITY = ECPoint(BigInteger.ZERO, BigInteger.ZERO)
        }
    }

    fun add(p1: ECPoint, p2: ECPoint): ECPoint {
        if (p1.isInfinity) return p2
        if (p2.isInfinity) return p1

        if (p1.x == p2.x) {
            if (p1.y != p2.y || p1.y == BigInteger.ZERO) {
                return ECPoint.INFINITY
            }
            return doublePoint(p1)
        }

        val dy = p2.y.subtract(p1.y).mod(P)
        val dx = p2.x.subtract(p1.x).mod(P)
        val lambda = dy.multiply(dx.modInverse(P)).mod(P)

        val x3 = lambda.pow(2).subtract(p1.x).subtract(p2.x).mod(P)
        val y3 = lambda.multiply(p1.x.subtract(x3)).subtract(p1.y).mod(P)

        return ECPoint(x3, y3)
    }

    fun doublePoint(p: ECPoint): ECPoint {
        if (p.isInfinity || p.y == BigInteger.ZERO) return ECPoint.INFINITY

        val num = BigInteger.valueOf(3).multiply(p.x.pow(2)).mod(P)
        val den = BigInteger.valueOf(2).multiply(p.y).modInverse(P)
        val lambda = num.multiply(den).mod(P)

        val x3 = lambda.pow(2).subtract(BigInteger.valueOf(2).multiply(p.x)).mod(P)
        val y3 = lambda.multiply(p.x.subtract(x3)).subtract(p.y).mod(P)

        return ECPoint(x3, y3)
    }

    fun multiply(scalar: BigInteger, point: ECPoint = G): ECPoint {
        var k = scalar.mod(N)
        if (k == BigInteger.ZERO || point.isInfinity) return ECPoint.INFINITY

        var result = ECPoint.INFINITY
        var base = point

        while (k > BigInteger.ZERO) {
            if (k.testBit(0)) {
                result = add(result, base)
            }
            base = doublePoint(base)
            k = k.shiftRight(1)
        }

        return result
    }

    fun compressPubKey(point: ECPoint): ByteArray {
        require(!point.isInfinity) { "Cannot compress point at infinity" }
        val prefix = if (point.y.testBit(0)) 0x03.toByte() else 0x02.toByte()
        val xBytes = point.x.toByteArrayUnsigned(32)
        val result = ByteArray(33)
        result[0] = prefix
        System.arraycopy(xBytes, 0, result, 1, 32)
        return result
    }

    fun uncompressPubKey(point: ECPoint): ByteArray {
        require(!point.isInfinity) { "Cannot uncompress point at infinity" }
        val xBytes = point.x.toByteArrayUnsigned(32)
        val yBytes = point.y.toByteArrayUnsigned(32)
        val result = ByteArray(65)
        result[0] = 0x04.toByte()
        System.arraycopy(xBytes, 0, result, 1, 32)
        System.arraycopy(yBytes, 0, result, 33, 32)
        return result
    }

    fun BigInteger.toByteArrayUnsigned(length: Int): ByteArray {
        val bytes = this.toByteArray()
        if (bytes.size == length) return bytes
        val result = ByteArray(length)
        if (bytes.size > length) {
            System.arraycopy(bytes, bytes.size - length, result, 0, length)
        } else {
            System.arraycopy(bytes, 0, result, length - bytes.size, bytes.size)
        }
        return result
    }
}
