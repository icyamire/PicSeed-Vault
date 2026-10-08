package com.icyamire.valut.model

data class SelectedMediaFile(
    val uri: String,
    val name: String,
    val size: Long,
    val mimeType: String? = null
)
