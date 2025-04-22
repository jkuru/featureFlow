package com.kuru.featureflow.component.register

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

// Interface for the registry (abstraction)
interface DFComponentRegistry {
    fun register(dfComponentConfig: DFComponentConfig, screen: @Composable (NavController) -> Unit)
    fun getScreen(dfComponentConfig: DFComponentConfig): (@Composable (NavController) -> Unit)?
    fun unregister(dfComponentConfig: DFComponentConfig): Boolean
    fun isRegistrationValid(dfComponentConfig: DFComponentConfig): Boolean
}