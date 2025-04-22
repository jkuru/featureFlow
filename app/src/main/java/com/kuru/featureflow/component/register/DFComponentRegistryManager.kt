package com.kuru.featureflow.component.register

import androidx.compose.runtime.Composable
import androidx.navigation.NavController


class DFComponentRegistryManager(private val componentRegistry: MutableMap<DFComponentConfig, @Composable (NavController) -> Unit>) : DFComponentRegistry {
    override fun register(dfComponentConfig: DFComponentConfig, screen: @Composable (NavController) -> Unit) {
        componentRegistry[dfComponentConfig] = screen
    }

    override fun getScreen(dfComponentConfig: DFComponentConfig): (@Composable (NavController) -> Unit)? =
        componentRegistry[dfComponentConfig]

    override fun unregister(dfComponentConfig: DFComponentConfig): Boolean {
        componentRegistry.remove(dfComponentConfig)
        return componentRegistry.contains(dfComponentConfig)
    }

    override fun isRegistrationValid(dfComponentConfig: DFComponentConfig): Boolean {
        return componentRegistry.contains(dfComponentConfig)
    }

    companion object {
        private const val TAG = "DynamicFeatureManager"
    }
}

/**
 * // --- Feature Module (:plants) ---
 *
 * // Implementation within the :plants module
 * object PlantsFeatureRegistrar : FeatureRegistrar {
 *
 *     override fun getDefinition(): DFComponentDefinitions {
 *         return DFComponentDefinitions(
 *             moduleName = "plants",
 *             uri = "app://plants",
 *             route = "plants", // The route this registrar handles
 *             // initializerClassName = this::class.java.name // Self-reference if needed
 *         )
 *     }
 *
 *     override fun registerComponents(registry: ScreenRegistry) {
 *         // Use the definition's route for consistency
 *         val route = getDefinition().route
 *         if (route != null) {
 *              registry.register(route) { navController ->
 *                  PlantsScreen(navController) // Access the actual composable from within the feature module
 *              }
 *         }
 *         // Could register other things here too (dialogs, services, etc.)
 *     }
 * }
 *
 */