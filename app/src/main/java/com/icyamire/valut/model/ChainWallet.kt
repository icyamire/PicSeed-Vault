package com.icyamire.valut.model

data class ChainWallet(
    val chainName: String,
    val symbol: String,
    val address: String,
    val privateKey: String,
    val derivationPath: String,
    val publicKey: String? = null
)
