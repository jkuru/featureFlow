package com.kuru.featureflow.component.boot

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.kuru.featureflow.component.googleplay.DFGooglePlayInstallerBootStrap
import com.kuru.featureflow.component.googleplay.DFComponentInstaller
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.register.DFComponentConfig
import com.kuru.featureflow.component.register.DFComponentRegistryManager

/**
 * Dynamic feature Initialize the Dynamic Feature Component.
 * Dynamic Feature Component is a singleton
 * Bootstrap interface will be called by Cold/warm bootstrapping function
 */
object DFSDKBootStrap : DFSDKBoot, DFSDK {
    private lateinit var manager: DFComponentInstaller
    private lateinit var DFComponentRegistry: DFComponentRegistry
    private var isInitialized = false

    override fun initialize(context: Context, sharedPreferencesFileName: String) {
        if (!isInitialized) {
            val componentRegistryCollection: MutableMap<DFComponentConfig, @Composable (NavController) -> Unit> =
                mutableMapOf()
            DFComponentRegistry = DFComponentRegistryManager(componentRegistryCollection)
            manager = DFGooglePlayInstallerBootStrap().initialize(context, sharedPreferencesFileName, DFComponentRegistry)
            isInitialized = true
        }
    }

    override fun getComponentInstaller(context: Context, sharedPreferencesFileName: String): DFComponentInstaller {
        if (!isInitialized) {
            initialize(context, sharedPreferencesFileName)
        }
        return manager
    }
}