package com.icyamire.valut.crypto

import com.icyamire.valut.crypto.Secp256k1.toByteArrayUnsigned
import com.icyamire.valut.model.ChainWallet
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object BitcoinWalletDeriver {

    private const val BITCOIN_SEED_KEY = "Bitcoin seed"

    private data class HDKey(val privateKey: BigInteger, val chainCode: ByteArray)

    fun deriveBip84Wallet(seed: ByteArray): ChainWallet {
        // Master key generation
        var current = deriveMasterKey(seed)

        // Derivation path m/84'/0'/0'/0/0
        val path = intArrayOf(
            84 or 0x80000000.toInt(), // 84'
            0 or 0x80000000.toInt(),  // 0'
            0 or 0x80000000.toInt(),  // 0'
            0,                       // 0
            0                        // 0
        )

        for (index in path) {
            current = deriveChildKey(current, index)
        }

        val privKeyBytes = current.privateKey.toByteArrayUnsigned(32)
        val pubKeyPoint = Secp256k1.multiply(current.privateKey)
        val compressedPubKey = Secp256k1.compressPubKey(pubKeyPoint)

        // Hash160 = RIPEMD160(SHA256(pubKey))
        val sha256 = MessageDigest.getInstance("SHA-256").digest(compressedPubKey)
        val pubKeyHash = Ripemd160.digest(sha256)

        // BIP-84 Bech32 Native SegWit address: bc1q...
        val address = Bech32.encode("bc", 0, pubKeyHash)

        // WIF private key: 0x80 + 32-byte privKey + 0x01 (compressed flag) -> Base58Check
        val wifData = ByteArray(34)
        wifData[0] = 0x80.toByte()
        System.arraycopy(privKeyBytes, 0, wifData, 1, 32)
        wifData[33] = 0x01.toByte()
        val wifPrivateKey = Base58.encodeCheck(wifData)

        return ChainWallet(
            chainName = "Bitcoin (BTC)",
            symbol = "BTC",
            address = address,
            privateKey = wifPrivateKey,
            derivationPath = "m/84'/0'/0'/0/0",
            publicKey = compressedPubKey.toHexString()
        )
    }

    private fun deriveMasterKey(seed: ByteArray): HDKey {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(BITCOIN_SEED_KEY.toByteArray(Charsets.UTF_8), "HmacSHA512"))
        val i = mac.doFinal(seed)

        val il = i.copyOfRange(0, 32)
        val ir = i.copyOfRange(32, 64)

        val masterPrivKey = BigInteger(1, il).mod(Secp256k1.N)
        return HDKey(masterPrivKey, ir)
    }

    private fun deriveChildKey(parent: HDKey, index: Int): HDKey {
        val isHardened = (index and 0x80000000.toInt()) != 0
        val data: ByteArray

        if (isHardened) {
            data = ByteArray(37)
            data[0] = 0x00
            val privBytes = parent.privateKey.toByteArrayUnsigned(32)
            System.arraycopy(privBytes, 0, data, 1, 32)
            val indexBytes = ByteBuffer.allocate(4).putInt(index).array()
            System.arraycopy(indexBytes, 0, data, 33, 4)
        } else {
            val pubKeyPoint = Secp256k1.multiply(parent.privateKey)
            val compressedPubKey = Secp256k1.compressPubKey(pubKeyPoint)
            data = ByteArray(37)
            System.arraycopy(compressedPubKey, 0, data, 0, 33)
            val indexBytes = ByteBuffer.allocate(4).putInt(index).array()
            System.arraycopy(indexBytes, 0, data, 33, 4)
        }

        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(parent.chainCode, "HmacSHA512"))
        val i = mac.doFinal(data)

        val il = i.copyOfRange(0, 32)
        val ir = i.copyOfRange(32, 64)

        val parseIl = BigInteger(1, il)
        val childPrivKey = parseIl.add(parent.privateKey).mod(Secp256k1.N)

        return HDKey(childPrivKey, ir)
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
