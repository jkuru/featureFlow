package com.kuru.featureflow.component.register

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

class DFComponentRegistryManager(
    private val componentRegistry: MutableMap<DFComponentConfig, @Composable (NavController) -> Unit>,
    private val services: MutableMap<Class<*>, Any>
) : DFComponentRegistry {
    override fun register(dfComponentConfig: DFComponentConfig, screen: @Composable (NavController) -> Unit) {
        componentRegistry[dfComponentConfig] = screen
    }

    override fun getScreen(dfComponentConfig: DFComponentConfig): (@Composable (NavController) -> Unit)? =
        componentRegistry[dfComponentConfig]

    override fun unregister(dfComponentConfig: DFComponentConfig): Boolean {
        componentRegistry.remove(dfComponentConfig)
        return !componentRegistry.contains(dfComponentConfig)
    }

    override fun isRegistrationValid(dfComponentConfig: DFComponentConfig): Boolean {
        return componentRegistry.contains(dfComponentConfig)
    }

    override fun <T : Any> registerService(serviceClass: Class<T>, instance: T) {
        services[serviceClass] = instance
    }

    override fun <T : Any> getService(serviceClass: Class<T>): T? {
        return services[serviceClass] as T?
    }

    override fun getConfig(route: String): DFComponentConfig? {
        return componentRegistry.keys.find { it.route == route }
    }

    companion object {
        private const val TAG = "DynamicFeatureManager"
    }
}