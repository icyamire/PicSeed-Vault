package com.icyamire.valut.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface NavRoute : NavKey {
    @Serializable
    data object MediaSelection : NavRoute

    @Serializable
    data object PinSetup : NavRoute

    @Serializable
    data object Processing : NavRoute

    @Serializable
    data object Result : NavRoute
}
