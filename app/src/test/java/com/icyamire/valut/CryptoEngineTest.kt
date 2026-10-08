package com.icyamire.valut

import com.icyamire.valut.crypto.Bip39
import com.icyamire.valut.crypto.Bip39Wordlist
import com.icyamire.valut.crypto.DeterministicSeedEngine
import com.icyamire.valut.crypto.Keccak256
import com.icyamire.valut.crypto.Ripemd160
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CryptoEngineTest {

    @Test
    fun testBip39WordlistSize() {
        assertEquals(2048, Bip39Wordlist.WORDS.size)
        assertEquals(2048, Bip39Wordlist.WORDS.distinct().size)
        for (i in 0 until Bip39Wordlist.WORDS.size - 1) {
            val w1 = Bip39Wordlist.WORDS[i]
            val w2 = Bip39Wordlist.WORDS[i + 1]
            if (w1 >= w2) {
                println("Out of order at index $i: $w1 >= $w2 (index ${i+1})")
            }
        }
    }

    @Test
    fun testBip39TestVector3() {
        val entropy = "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val mnemonic = Bip39.entropyToMnemonic(entropy)
        assertEquals("legal winner text year wave sausage worth useful legal winner text yellow", mnemonic)

        val seed = Bip39.mnemonicToSeed(mnemonic, "TREZOR")
        val seedHex = seed.joinToString("") { "%02x".format(it) }
        val spec = javax.crypto.spec.PBEKeySpec(mnemonic.toCharArray(), "mnemonicTREZOR".toByteArray(Charsets.UTF_8), 2048, 512)
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val expectedSeedHex = skf.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }

        assertEquals(expectedSeedHex, seedHex)
    }

    @Test
    fun testBip39TestVector4_24Words() {
        val entropy = "8080808080808080808080808080808080808080808080808080808080808080".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val mnemonic = Bip39.entropyToMnemonic(entropy)
        assertEquals(
            "letter advice call absurd amused doctor acoustic awake letter advice call absurd amused doctor acoustic awake letter advice call absurd amused doctor acoustic blood",
            mnemonic
        )

        val seed = Bip39.mnemonicToSeed(mnemonic, "TREZOR")
        val seedHex = seed.joinToString("") { "%02x".format(it) }
        val spec = javax.crypto.spec.PBEKeySpec(mnemonic.toCharArray(), "mnemonicTREZOR".toByteArray(Charsets.UTF_8), 2048, 512)
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val expectedSeedHex = skf.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }

        assertEquals(expectedSeedHex, seedHex)
    }

    @Test
    fun testEthereumWalletDeriverWithFixedBip39() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed = Bip39.mnemonicToSeed(mnemonic, "")

        val ethWallet = com.icyamire.valut.crypto.EthereumWalletDeriver.deriveEthWallet(seed)

        assertEquals("0x1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727", ethWallet.privateKey)
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", ethWallet.address)
    }

    @Test
    fun testBitcoinWalletDeriverWithFixedBip39() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed = Bip39.mnemonicToSeed(mnemonic, "")

        val btcWallet = com.icyamire.valut.crypto.BitcoinWalletDeriver.deriveBip84Wallet(seed)

        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", btcWallet.address)
        assertEquals("KyZpNDKnfs94vbrwhJneDi77V6jF64PWPF8x5cdJb8ifgg2DUc9d", btcWallet.privateKey)
    }

    @Test
    fun testKeccak256() {
        // Ethereum Keccak-256("hello")
        val hash = Keccak256.digest("hello".toByteArray(Charsets.UTF_8))
        val hexHash = hash.joinToString("") { "%02x".format(it) }
        assertEquals("1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8", hexHash)
    }

    @Test
    fun testRipemd160() {
        // RIPEMD-160("")
        val hash = Ripemd160.digest(ByteArray(0))
        val hexHash = hash.joinToString("") { "%02x".format(it) }
        assertEquals("9c1185a5c5e9fc54612808977ee8f548b2258d31", hexHash)
    }

    @Test
    fun testDeterministicSeedEngine_SameInputsProduceSameResult() {
        val engine = DeterministicSeedEngine()

        val file1Data = "Sample Image Content 1".toByteArray(Charsets.UTF_8)
        val file2Data = "Sample Audio Content 2".toByteArray(Charsets.UTF_8)

        val pin = "123456"

        val streams1 = listOf(
            ByteArrayInputStream(file1Data),
            ByteArrayInputStream(file2Data)
        )
        val streams2 = listOf(
            ByteArrayInputStream(file1Data),
            ByteArrayInputStream(file2Data)
        )

        val result1 = engine.generateFromStreams(streams1, pin, mnemonicWordCount = 24)
        val result2 = engine.generateFromStreams(streams2, pin, mnemonicWordCount = 24)

        assertEquals(result1.mnemonic, result2.mnemonic)
        assertEquals(result1.seedHex, result2.seedHex)
        assertEquals(result1.btcWallet.address, result2.btcWallet.address)
        assertEquals(result1.ethWallet.address, result2.ethWallet.address)
        assertEquals(result1.xmrWallet.address, result2.xmrWallet.address)
    }

    @Test
    fun testBitcoinAddressFormat() {
        val engine = DeterministicSeedEngine()
        val streams = listOf(ByteArrayInputStream("test content".toByteArray(Charsets.UTF_8)))
        val result = engine.generateFromStreams(streams, "1234", mnemonicWordCount = 24)

        assertTrue("BTC address should start with bc1q", result.btcWallet.address.startsWith("bc1q"))
        assertEquals("Bitcoin (BTC)", result.btcWallet.chainName)
        assertEquals("BTC", result.btcWallet.symbol)
        assertEquals("m/84'/0'/0'/0/0", result.btcWallet.derivationPath)
        assertNotNull(result.btcWallet.privateKey)
    }

    @Test
    fun testEthereumAddressFormat() {
        val engine = DeterministicSeedEngine()
        val streams = listOf(ByteArrayInputStream("test content".toByteArray(Charsets.UTF_8)))
        val result = engine.generateFromStreams(streams, "1234", mnemonicWordCount = 24)

        assertTrue("ETH address should start with 0x", result.ethWallet.address.startsWith("0x"))
        assertEquals(42, result.ethWallet.address.length)
        assertEquals("Ethereum (ETH / USDT)", result.ethWallet.chainName)
        assertEquals("ETH", result.ethWallet.symbol)
        assertEquals("m/44'/60'/0'/0/0", result.ethWallet.derivationPath)
        assertTrue("ETH private key should start with 0x", result.ethWallet.privateKey.startsWith("0x"))
    }

    @Test(expected = java.io.IOException::class)
    fun testEmptyStreamsThrowsIOException() {
        val engine = DeterministicSeedEngine()
        engine.generateFromStreams(emptyList(), "1234", mnemonicWordCount = 24)
    }

    @Test
    fun testMoneroAddressAndMnemonic() {
        val engine = DeterministicSeedEngine()
        val streams = listOf(ByteArrayInputStream("test content".toByteArray(Charsets.UTF_8)))
        val result = engine.generateFromStreams(streams, "1234", mnemonicWordCount = 24)

        assertTrue("XMR address should start with 4", result.xmrWallet.address.startsWith("4"))
        assertEquals(95, result.xmrWallet.address.length)
        assertEquals("Monero (XMR)", result.xmrWallet.chainName)
        assertEquals("XMR", result.xmrWallet.symbol)
        assertNotNull(result.xmrMnemonic)
        assertEquals(25, result.xmrMnemonic!!.split(" ").size)
    }
}
