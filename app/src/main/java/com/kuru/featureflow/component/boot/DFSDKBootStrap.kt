package com.kuru.featureflow.component.boot

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.kuru.featureflow.component.googleplay.DFGooglePlayInstallerBootStrap
import com.kuru.featureflow.component.googleplay.DFComponentInstaller
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.register.DFComponentConfig
import com.kuru.featureflow.component.register.DFComponentRegistryManager
import com.kuru.featureflow.component.state.DFComponentStateStore
import com.kuru.featureflow.component.state.DFComponentStateStoreImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Dynamic feature Initialize the Dynamic Feature Component.
 * Dynamic Feature Component is a singleton
 * Bootstrap interface will be called by Cold/warm bootstrapping function
 */
object DFSDKBootStrap : DFSDKBoot, DFSDK {
    private lateinit var manager: DFComponentInstaller
    private lateinit var componentRegistry: DFComponentRegistry
    private var isInitialized = false
    private lateinit var stateStore: DFComponentStateStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val services = mutableMapOf<Class<*>, Any>()


    override fun initialize(context: Context, sharedPreferencesFileName: String) {
        if (!isInitialized) {
            val prefs: SharedPreferences? =
                context.getSharedPreferences(sharedPreferencesFileName, Context.MODE_PRIVATE)
            val componentRegistryCollection: MutableMap<DFComponentConfig, @Composable (NavController) -> Unit> =
                mutableMapOf()
            componentRegistry = DFComponentRegistryManager(componentRegistryCollection,services)
            manager = DFGooglePlayInstallerBootStrap().initialize(
                context,
                componentRegistry,
                scope
            )
            stateStore = DFComponentStateStoreImpl(scope,prefs)
            isInitialized = true
        }
    }

    override fun getComponentInstaller(
        context: Context,
        sharedPreferencesFileName: String
    ): DFComponentInstaller {
        if (!isInitialized) {
            initialize(context, sharedPreferencesFileName)
        }
        return manager
    }


    override fun getStateStore(): DFComponentStateStore {
        return stateStore
    }

    override fun getComponentRegistry(): DFComponentRegistry {
        return componentRegistry
    }

}