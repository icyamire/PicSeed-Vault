package com.icyamire.valut.model

data class CryptoResult(
    val seedBytes: ByteArray,
    val seedHex: String,
    val mnemonic: String,
    val btcWallet: ChainWallet,
    val ethWallet: ChainWallet,
    val xmrWallet: ChainWallet,
    val xmrMnemonic: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CryptoResult

        if (!seedBytes.contentEquals(other.seedBytes)) return false
        if (seedHex != other.seedHex) return false
        if (mnemonic != other.mnemonic) return false
        if (btcWallet != other.btcWallet) return false
        if (ethWallet != other.ethWallet) return false
        if (xmrWallet != other.xmrWallet) return false
        if (xmrMnemonic != other.xmrMnemonic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = seedBytes.contentHashCode()
        result = 31 * result + seedHex.hashCode()
        result = 31 * result + mnemonic.hashCode()
        result = 31 * result + btcWallet.hashCode()
        result = 31 * result + ethWallet.hashCode()
        result = 31 * result + xmrWallet.hashCode()
        result = 31 * result + (xmrMnemonic?.hashCode() ?: 0)
        return result
    }
}
