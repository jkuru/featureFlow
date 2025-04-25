package com.kuru.featureflow.component.register

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DFComponentRegistryManager @Inject constructor(
    private val componentRegistry: MutableMap<DFComponentConfig, @Composable (NavController) -> Unit>,
    private val services: MutableMap<Class<*>, Any>
) : DFComponentRegistry {

    companion object {
        private const val TAG = "DFRegistryManager"
    }

    override fun register(dfComponentConfig: DFComponentConfig, screen: @Composable (NavController) -> Unit) {
        componentRegistry[dfComponentConfig] = screen
        Log.d(TAG, "Registered screen for route: ${dfComponentConfig.route}")
    }

    override fun getScreen(dfComponentConfig: DFComponentConfig): (@Composable (NavController) -> Unit)? {
        return componentRegistry[dfComponentConfig]
    }

    override fun unregister(dfComponentConfig: DFComponentConfig): Boolean {
        val removed = componentRegistry.remove(dfComponentConfig) != null
        if (removed) {
            Log.d(TAG, "Unregistered screen for route: ${dfComponentConfig.route}")
        }
        return removed
    }

    override fun isRegistrationValid(dfComponentConfig: DFComponentConfig): Boolean {
        return componentRegistry.containsKey(dfComponentConfig)
    }

    override fun getConfig(route: String): DFComponentConfig? {
        return componentRegistry.keys.find { it.route == route }
    }
}