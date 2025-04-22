package com.kuru.featureflow.component.googleplay

import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.DFComponentStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This is exposed to Component, state will be moved to state store TODO
 */
class DFComponentInstallerManager(
    private val manager: SplitInstallManager,
    private val scope: CoroutineScope,
    private val states: MutableMap<String, MutableStateFlow<DFComponentStateStore>>,
    private val mutex: Mutex,
    private val prefs: SharedPreferences?, //TODO why ?
    private val DFComponentRegistry: DFComponentRegistry
) : DFComponentInstaller {

    override fun isComponentInstalled(componentName: String): Boolean {
        return manager.installedModules.contains(componentName)
    }

    override fun getComponentState(componentName: String): Flow<DFComponentStateStore> {
        Log.e(TAG, "📱  getModuleStateV1 componentName = $componentName ")
        return states.getOrPut(componentName) {
            MutableStateFlow(
                if (isComponentInstalled(componentName)) DFComponentStateStore.Installed
                else DFComponentStateStore.NotInstalled
            )
        }
    }

    override  fun installComponent(componentName: String) {
        scope.launch {
            Log.d(TAG, "📱  loadModule componentName = $componentName ")
            mutex.withLock {
                if (isComponentInstalled(componentName)) return@launch

                val stateFlow = states.getOrPut(componentName) {
                    MutableStateFlow(DFComponentStateStore.NotInstalled)
                }

                stateFlow.value = DFComponentStateStore.Loading
                Log.d(TAG, "📱  loadModule isModuleInstalled = false ")
                try {
                    val request = SplitInstallRequest.newBuilder()
                        .addModule(componentName)
                        .build()
                    Log.d(TAG, "📱  loadModule manager.startInstall(request)")
                    manager.startInstall(request)
                } catch (e: Exception) {
                    Log.e(TAG, "📱  loadModule Exception $e")
                    stateFlow.value =
                        DFComponentStateStore.Error(e.message ?: "Installation failed")
                }
            }
        }
    }

    override fun retryComponentInstall(componentName: String) {
        Log.d(TAG, "📱  retryModuleLoad =  $componentName")
        scope.launch { installComponent(componentName) }
    }

    override fun getComponentRegistry(): DFComponentRegistry {
        return DFComponentRegistry
    }

    companion object {
        private const val TAG = "DynamicFeatureManager"
    }
}