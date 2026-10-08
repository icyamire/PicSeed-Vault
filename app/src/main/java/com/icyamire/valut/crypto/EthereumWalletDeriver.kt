package com.icyamire.valut.crypto

import com.icyamire.valut.crypto.Secp256k1.toByteArrayUnsigned
import com.icyamire.valut.model.ChainWallet
import java.math.BigInteger
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object EthereumWalletDeriver {

    private const val BITCOIN_SEED_KEY = "Bitcoin seed"

    private data class HDKey(val privateKey: BigInteger, val chainCode: ByteArray)

    fun deriveEthWallet(seed: ByteArray): ChainWallet {
        var current = deriveMasterKey(seed)

        // Derivation path m/44'/60'/0'/0/0
        val path = intArrayOf(
            44 or 0x80000000.toInt(), // 44'
            60 or 0x80000000.toInt(), // 60'
            0 or 0x80000000.toInt(),  // 0'
            0,                       // 0
            0                        // 0
        )

        for (index in path) {
            current = deriveChildKey(current, index)
        }

        val privKeyBytes = current.privateKey.toByteArrayUnsigned(32)
        val pubKeyPoint = Secp256k1.multiply(current.privateKey)
        val uncompressedPubKey = Secp256k1.uncompressPubKey(pubKeyPoint)

        // Take 64 bytes of uncompressed public key (excluding leading 0x04)
        val pubKeyBytes = uncompressedPubKey.copyOfRange(1, 65)

        // Address = Keccak256(pubKeyBytes)[12..31]
        val keccakHash = Keccak256.digest(pubKeyBytes)
        val rawAddress = keccakHash.copyOfRange(12, 32)
        val checksumAddress = toEip55Address(rawAddress)

        val privKeyHex = "0x" + privKeyBytes.toHexString()

        return ChainWallet(
            chainName = "Ethereum (ETH / USDT)",
            symbol = "ETH",
            address = checksumAddress,
            privateKey = privKeyHex,
            derivationPath = "m/44'/60'/0'/0/0",
            publicKey = uncompressedPubKey.toHexString()
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

    private fun toEip55Address(rawAddress: ByteArray): String {
        val cleanHex = rawAddress.toHexString().lowercase()
        val hashHex = Keccak256.digest(cleanHex.toByteArray(Charsets.UTF_8)).toHexString()

        val sb = StringBuilder("0x")
        for (i in cleanHex.indices) {
            val c = cleanHex[i]
            val hashChar = hashHex[i]
            if (c in 'a'..'f' && hashChar in '8'..'f') {
                sb.append(c.uppercaseChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
