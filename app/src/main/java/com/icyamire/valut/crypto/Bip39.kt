package com.icyamire.valut.crypto

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

object Bip39 {

    fun entropyToMnemonic(entropy: ByteArray): String {
        val entBits = entropy.size * 8
        require(entBits in listOf(128, 160, 192, 224, 256)) {
            "Entropy length must be 128, 160, 192, 224, or 256 bits, but was $entBits"
        }

        val csBits = entBits / 32
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(entropy)

        // Combine entropy bits + checksum bits
        val totalBits = entBits + csBits
        val bitArray = BooleanArray(totalBits)

        var bitIndex = 0
        for (b in entropy) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitArray[bitIndex++] = ((byteVal shr i) and 1) == 1
            }
        }

        for (i in 0 until csBits) {
            val byteIdx = i / 8
            val bitPos = 7 - (i % 8)
            val bit = ((hash[byteIdx].toInt() and 0xFF) shr bitPos) and 1
            bitArray[bitIndex++] = (bit == 1)
        }

        // Convert every 11 bits into a word index
        val numWords = totalBits / 11
        val words = ArrayList<String>(numWords)
        for (i in 0 until numWords) {
            var wordIndex = 0
            for (j in 0 until 11) {
                wordIndex = (wordIndex shl 1) or (if (bitArray[i * 11 + j]) 1 else 0)
            }
            words.add(Bip39Wordlist.WORDS[wordIndex])
        }

        return words.joinToString(" ")
    }

    fun validateMnemonic(mnemonic: String): Boolean {
        return try {
            val words = mnemonic.trim().split("\\s+".toRegex())
            if (words.size !in listOf(12, 15, 18, 21, 24)) return false

            val totalBits = words.size * 11
            val bitArray = BooleanArray(totalBits)

            for ((i, word) in words.withIndex()) {
                val index = Bip39Wordlist.WORD_INDEX_MAP[word] ?: return false
                for (j in 0 until 11) {
                    val bitPos = 10 - j
                    val bit = (index shr bitPos) and 1
                    bitArray[i * 11 + j] = (bit == 1)
                }
            }

            val csBits = totalBits / 33
            val entBits = totalBits - csBits
            val entropyBytesCount = entBits / 8

            val entropy = ByteArray(entropyBytesCount)
            for (i in 0 until entBits) {
                val byteIdx = i / 8
                val bitPos = 7 - (i % 8)
                if (bitArray[i]) {
                    entropy[byteIdx] = (entropy[byteIdx].toInt() or (1 shl bitPos)).toByte()
                }
            }

            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(entropy)

            for (i in 0 until csBits) {
                val byteIdx = i / 8
                val bitPos = 7 - (i % 8)
                val expectedBit = ((hash[byteIdx].toInt() and 0xFF) shr bitPos) and 1 == 1
                val actualBit = bitArray[entBits + i]
                if (expectedBit != actualBit) return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val normalizedMnemonic = java.text.Normalizer.normalize(
            mnemonic.trim().replace("\\s+".toRegex(), " "),
            java.text.Normalizer.Form.NFKD
        )
        val normalizedPassphrase = java.text.Normalizer.normalize(
            passphrase,
            java.text.Normalizer.Form.NFKD
        )
        val passwordBytes = normalizedMnemonic.toByteArray(Charsets.UTF_8)
        val saltBytes = ("mnemonic$normalizedPassphrase").toByteArray(Charsets.UTF_8)

        return pbkdf2HmacSha512(passwordBytes, saltBytes, iterations = 2048, derivedKeyLength = 64)
    }

    private fun pbkdf2HmacSha512(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        derivedKeyLength: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        val hLen = mac.macLength // 64 bytes
        val l = ceil(derivedKeyLength.toDouble() / hLen).toInt()
        val r = derivedKeyLength - (l - 1) * hLen
        val derivedKey = ByteArray(derivedKeyLength)
        val keySpec = SecretKeySpec(password, "HmacSHA512")

        for (i in 1..l) {
            mac.init(keySpec)
            val blockIndex = ByteBuffer.allocate(4).putInt(i).array()
            mac.update(salt)
            mac.update(blockIndex)
            var u = mac.doFinal()
            val t = u.clone()

            for (j in 2..iterations) {
                mac.init(keySpec)
                u = mac.doFinal(u)
                for (k in u.indices) {
                    t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
                }
            }

            val offset = (i - 1) * hLen
            val length = if (i == l) r else hLen
            System.arraycopy(t, 0, derivedKey, offset, length)
        }
        return derivedKey
    }
}
