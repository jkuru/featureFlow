package com.kuru.featureflow.plant

import com.kuru.featureflow.component.boot.DFSDKBootStrap
import com.kuru.featureflow.component.register.DFComponentConfig
import com.kuru.featureflow.component.register.DFComponentEntry
import com.kuru.featureflow.component.register.DFComponentInterceptor

object PlantEntry : DFComponentEntry {
    override fun initialize() {
        val config = plantConfig()
        val dynamicInstallSDK =  DFSDKBootStrap.getComponentRegistry().register(config) { navController ->
            PlantsScreen(navController) // Composable screen for the feature
        }
    }

    /**
     *  Build Plant Config
     */
    private fun plantConfig(): DFComponentConfig {
        return DFComponentConfig(
            route = "plants",
            listOfDFComponentInterceptor = listOf(
                // Pre-install interceptor: Check network
                DFComponentInterceptor(preInstall = true) {
                    val isNetworkAvailable = true // Hypothetical function
                    if (!isNetworkAvailable) println("No network for plants feature")
                    isNetworkAvailable
                },
                // Post-install interceptor
                DFComponentInterceptor(preInstall = false) {
                    try {
                        // Analytics
                        println("Analytics for plants feature")
                        true
                    } catch (e: Exception) {
                        println("Failed  $e")
                        false
                    }
                }
            )
        )
    }
}