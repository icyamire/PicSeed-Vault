package com.icyamire.valut.crypto

import android.content.Context
import android.net.Uri
import com.icyamire.valut.model.CryptoResult
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

class DeterministicSeedEngine {

    companion object {
        val DEFAULT_SALT: ByteArray = "Valut_Deterministic_Seed_Salt_v1".toByteArray(Charsets.UTF_8)
    }

    fun generateFromStreams(
        inputStreams: List<InputStream>,
        pin: String,
        salt: ByteArray = DEFAULT_SALT,
        mnemonicWordCount: Int = 24
    ): CryptoResult {
        require(mnemonicWordCount == 12 || mnemonicWordCount == 24) {
            "mnemonicWordCount must be 12 or 24"
        }
        if (inputStreams.isEmpty()) {
            throw IOException("No valid file input streams provided.")
        }

        try {
            // 1. Incremental SHA-256 over all input streams in exact array order + PIN
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)

            for (stream in inputStreams) {
                stream.use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            }

            digest.update(pin.toByteArray(Charsets.UTF_8))
            val combinedHash = digest.digest()

            // 2. PBKDF2 HMAC-SHA256 (10,000 rounds)
            val entropyLengthBytes = if (mnemonicWordCount == 12) 16 else 32
            val entropyBytes = pbkdf2HmacSha256(
                password = combinedHash,
                salt = salt,
                iterations = 10_000,
                derivedKeyLength = entropyLengthBytes
            )

            // 3. Transform entropy into BIP-39 Mnemonic Phrase (12 or 24 words)
            val mnemonic = Bip39.entropyToMnemonic(entropyBytes)

            // 4. Derive BIP-39 512-bit seed
            val bip39Seed = Bip39.mnemonicToSeed(mnemonic, passphrase = "")

            // 5. Derive multi-chain wallets
            val btcWallet = BitcoinWalletDeriver.deriveBip84Wallet(bip39Seed)
            val ethWallet = EthereumWalletDeriver.deriveEthWallet(bip39Seed)
            val xmrResult = MoneroWalletDeriver.deriveXmrWallet(bip39Seed)

            return CryptoResult(
                seedBytes = bip39Seed,
                seedHex = bip39Seed.toHexString(),
                mnemonic = mnemonic,
                btcWallet = btcWallet,
                ethWallet = ethWallet,
                xmrWallet = xmrResult.wallet,
                xmrMnemonic = xmrResult.mnemonic
            )
        } finally {
            inputStreams.forEach { stream ->
                try {
                    stream.close()
                } catch (_: Exception) { }
            }
        }
    }

    fun generateFromUris(
        context: Context,
        uris: List<Uri>,
        pin: String,
        salt: ByteArray = DEFAULT_SALT,
        mnemonicWordCount: Int = 24
    ): CryptoResult {
        if (uris.isEmpty()) {
            throw IOException("No media file URIs provided.")
        }

        val streams = ArrayList<InputStream>()
        for (uri in uris) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    streams.add(stream)
                }
            } catch (e: Exception) {
                // If opening a stream throws exception, close any streams opened so far and rethrow/report
                streams.forEach { try { it.close() } catch (_: Exception) {} }
                throw IOException("Failed to open stream for URI: $uri. Error: ${e.localizedMessage}", e)
            }
        }

        if (streams.isEmpty()) {
            throw IOException("Failed to open selected files. Permissions may have expired or files were removed.")
        }

        return generateFromStreams(streams, pin, salt, mnemonicWordCount)
    }

    private fun pbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        derivedKeyLength: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val hLen = mac.macLength // 32 bytes
        val l = ceil(derivedKeyLength.toDouble() / hLen).toInt()
        val r = derivedKeyLength - (l - 1) * hLen
        val derivedKey = ByteArray(derivedKeyLength)
        val keySpec = SecretKeySpec(password, "HmacSHA256")

        for (i in 1..l) {
            mac.init(keySpec)
            val blockIndex = ByteBuffer.allocate(4).putInt(i).array()
            mac.update(salt)
            mac.update(blockIndex)
            var u = mac.doFinal()
            val t = u.clone()

            for (j in 2..iterations) {
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

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}

