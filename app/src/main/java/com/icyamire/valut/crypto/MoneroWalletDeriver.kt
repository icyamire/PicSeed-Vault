package com.icyamire.valut.crypto

import com.icyamire.valut.model.ChainWallet
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

data class MoneroWalletResult(
    val wallet: ChainWallet,
    val mnemonic: String
)

object MoneroWalletDeriver {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58)

    fun deriveXmrWallet(seed: ByteArray): MoneroWalletResult {
        // 1. Private spend key: Keccak256(seed) reduced mod L (little-endian)
        val spendKeyHash = Keccak256.digest(seed)
        val spendKeyScalar = BigInteger(1, spendKeyHash.reversedArray()).mod(Ed25519.L)
        val spendKeyBytes = spendKeyScalar.toByteArrayUnsignedLE(32)

        // 2. Private view key: Keccak256(spendKeyBytes) reduced mod L (little-endian)
        val viewKeyHash = Keccak256.digest(spendKeyBytes)
        val viewKeyScalar = BigInteger(1, viewKeyHash.reversedArray()).mod(Ed25519.L)
        val viewKeyBytes = viewKeyScalar.toByteArrayUnsignedLE(32)

        // 3. Public keys on Ed25519
        val spendPoint = Ed25519.multiply(spendKeyScalar)
        val pubSpendKey = Ed25519.encodePoint(spendPoint)

        val viewPoint = Ed25519.multiply(viewKeyScalar)
        val pubViewKey = Ed25519.encodePoint(viewPoint)

        // 4. Build Monero mainnet address (Network byte 0x12 = 18)
        val payload = ByteArray(65)
        payload[0] = 0x12.toByte()
        System.arraycopy(pubSpendKey, 0, payload, 1, 32)
        System.arraycopy(pubViewKey, 0, payload, 33, 32)

        val checksum = Keccak256.digest(payload).copyOfRange(0, 4)
        val fullAddressBytes = ByteArray(69)
        System.arraycopy(payload, 0, fullAddressBytes, 0, 65)
        System.arraycopy(checksum, 0, fullAddressBytes, 65, 4)

        val address = encodeMoneroBase58(fullAddressBytes)

        // 5. Generate 25-word Monero Electrum mnemonic from spend key bytes
        val xmrMnemonic = generate25WordMnemonic(spendKeyBytes)

        val wallet = ChainWallet(
            chainName = "Monero (XMR)",
            symbol = "XMR",
            address = address,
            privateKey = spendKeyBytes.toHexString(),
            derivationPath = "m/44'/128'/0'/0/0",
            publicKey = "spend:${pubSpendKey.toHexString()},view:${pubViewKey.toHexString()}"
        )

        return MoneroWalletResult(wallet, xmrMnemonic)
    }

    private fun generate25WordMnemonic(spendKeyBytes: ByteArray): String {
        val words = ArrayList<String>(25)
        val wordList = MoneroWordlist.WORDS
        val listSize = wordList.size

        val buffer = ByteBuffer.wrap(spendKeyBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 8) {
            val uintVal = buffer.int.toLong() and 0xFFFFFFFFL
            val w1 = (uintVal % listSize).toInt()
            val w2 = ((uintVal / listSize + w1) % listSize).toInt()
            val w3 = ((uintVal / listSize / listSize + w2) % listSize).toInt()

            words.add(wordList[w1])
            words.add(wordList[w2])
            words.add(wordList[w3])
        }

        // 25th checksum word
        val crc = CRC32()
        for (i in 0 until 24) {
            val prefix = words[i].take(3)
            crc.update(prefix.toByteArray(Charsets.UTF_8))
        }
        val checksumIndex = (crc.value % 24).toInt()
        words.add(words[checksumIndex])

        return words.joinToString(" ")
    }

    private fun encodeMoneroBase58(data: ByteArray): String {
        val sb = StringBuilder()
        var offset = 0

        // Process 8-byte blocks -> 11 chars each
        while (offset + 8 <= data.size) {
            val block = data.copyOfRange(offset, offset + 8)
            sb.append(encodeBlock(block, 11))
            offset += 8
        }

        // Remainder 5 bytes -> 7 chars
        if (offset < data.size) {
            val block = data.copyOfRange(offset, data.size)
            sb.append(encodeBlock(block, 7))
        }

        return sb.toString()
    }

    private fun encodeBlock(block: ByteArray, expectedLen: Int): String {
        var num = BigInteger(1, block)
        val sb = StringBuilder()
        while (num > BigInteger.ZERO) {
            val mod = num.mod(BASE).toInt()
            sb.append(ALPHABET[mod])
            num = num.divide(BASE)
        }
        while (sb.length < expectedLen) {
            sb.append(ALPHABET[0])
        }
        return sb.reverse().toString()
    }

    private fun BigInteger.toByteArrayUnsignedLE(length: Int): ByteArray {
        val bytes = this.toByteArray()
        val result = ByteArray(length)
        val count = minOf(bytes.size, length)
        for (i in 0 until count) {
            result[i] = bytes[bytes.size - 1 - i]
        }
        return result
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
