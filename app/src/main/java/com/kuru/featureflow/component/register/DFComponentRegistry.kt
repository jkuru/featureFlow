package com.kuru.featureflow.component.register

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface DFComponentRegistry {
    // This function will be invoked by Feature module
    fun register(dfComponentConfig: DFComponentConfig, screen: @Composable (NavController) -> Unit)
    // This function will be invoked by App module in this case ViewModel of the framework
    fun getScreen(dfComponentConfig: DFComponentConfig): (@Composable (NavController) -> Unit)?

    // TODO
    fun unregister(dfComponentConfig: DFComponentConfig): Boolean
    //TODO
    fun isRegistrationValid(dfComponentConfig: DFComponentConfig): Boolean
    /**
     * Global Service Locator , this is for dependency injection from core to dynamic modules
     */
    fun <T : Any> registerService(serviceClass: Class<T>, instance: T)
    fun <T : Any> getService(serviceClass: Class<T>): T?
    fun getConfig(route: String): DFComponentConfig? // Added for config retrieval
}
