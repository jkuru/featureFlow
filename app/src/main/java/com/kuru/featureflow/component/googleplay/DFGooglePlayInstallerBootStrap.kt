package com.kuru.featureflow.component.googleplay

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.DFComponentStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

class DFGooglePlayInstallerBootStrap : DFGooglePlayInstallerBoot {

    override fun initialize(
        context: Context,
        sharedPreferencesFileName: String,
        DFComponentRegistry: DFComponentRegistry,
    ): DFComponentInstaller {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val manager = SplitInstallManagerFactory.create(context)
        val states = mutableMapOf<String, MutableStateFlow<DFComponentStateStore>>()
        val prefs: SharedPreferences? =
            context.getSharedPreferences(sharedPreferencesFileName, Context.MODE_PRIVATE)
        val mutex = Mutex()
        manager.registerListener(buildSplitInstallListener(states))
        return DFComponentInstallerManager(manager, scope, states, mutex, prefs,DFComponentRegistry)
    }

    /**
     * Who will call clean up ? TODO
     */
    override fun cleanup(
        manager: SplitInstallManager,
        scope: CoroutineScope,
        listener: SplitInstallStateUpdatedListener
    ) {
        Log.d(TAG, "📱  cleanup unregisterListener ")
        scope.cancel()
        manager.unregisterListener(listener)
    }

    private fun buildSplitInstallListener(states: MutableMap<String, MutableStateFlow<DFComponentStateStore>>): SplitInstallStateUpdatedListener {

        val listener = SplitInstallStateUpdatedListener { state ->
            val moduleName =
                state.moduleNames().firstOrNull() ?: return@SplitInstallStateUpdatedListener
            val stateFlow = states[moduleName] ?: return@SplitInstallStateUpdatedListener

            stateFlow.value = when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    Log.d(TAG, "📱  SplitInstallSessionStatus.DOWNLOADING ")
                    val progress = state.bytesDownloaded().toFloat() / state.totalBytesToDownload()
                    DFComponentStateStore.Downloading(progress)
                }

                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(TAG, "📱  SplitInstallSessionStatus.INSTALLED ")
                    //  loadPlantsFeature()
                    // TODO trigger dynamic link post install
                    DFComponentStateStore.Installed
                }

                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    Log.d(TAG, "📱  SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION ")
                    DFComponentStateStore.ConfirmationRequired {
                        //TODO Not sure when to use this ?
                    }
                }

                SplitInstallSessionStatus.FAILED -> {
                    Log.e(TAG, "📱  SplitInstallSessionStatus.FAILED ")
                    DFComponentStateStore.Error("Installation failed")
                    //TODO get module name from state and update the state store
                }

                else -> stateFlow.value // Keep current state for other statuses
            }
        }
        return listener
    }


    companion object {
        private const val TAG = "DynamicFeatureManager"
    }

}