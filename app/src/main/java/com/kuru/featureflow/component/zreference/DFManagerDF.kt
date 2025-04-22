/*
package com.kuru.featureflow.component.googleplay

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.kuru.featureflow.component.register.FeatureRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

*/
/**
 * Who will call the DynamicFeatureComponent to install module ?
 *  *    Two use case
 *  *    a) Within App navigation using intent or navigation graph
 *  *    b) Deep link using intent , recommend using new intent filters pattern
 *//*

class DFManagerDF(
    context: Context
) : DFModuleManager, DFManagerBoot {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manager = SplitInstallManagerFactory.create(context)
    private val states = mutableMapOf<String, MutableStateFlow<DFModuleState>>()
    private val mutex = Mutex()
    private val prefs: SharedPreferences? = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val listener = SplitInstallStateUpdatedListener { state ->
        val moduleName =
            state.moduleNames().firstOrNull() ?: return@SplitInstallStateUpdatedListener
        val stateFlow = states[moduleName] ?: return@SplitInstallStateUpdatedListener

        stateFlow.value = when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                Log.d(TAG, "📱  SplitInstallSessionStatus.DOWNLOADING ")
                val progress = state.bytesDownloaded().toFloat() / state.totalBytesToDownload()
                DFModuleState.Downloading(progress)
            }

            SplitInstallSessionStatus.INSTALLED -> {
                Log.d(TAG, "📱  SplitInstallSessionStatus.INSTALLED ")
                loadPlantsFeature()
                DFModuleState.Installed
            }

            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                Log.d(TAG, "📱  SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION ")
                DFModuleState.ConfirmationRequired {
//                    currentActivity?.startIntentSender(
//                        state.resolutionIntent()?.intentSender,
//                        null, 0, 0, 0
//                    )
                }
            }

            SplitInstallSessionStatus.FAILED -> {
                Log.e(TAG, "📱  SplitInstallSessionStatus.FAILED ")
                DFModuleState.Error("Installation failed")
            }

            else -> stateFlow.value // Keep current state for other statuses
        }
    }

    init {
        manager.registerListener(listener)
    }

    override fun isModuleInstalled(moduleName: String): Boolean {
        return manager.installedModules.contains(moduleName)
    }

    override fun getModuleStateV1(moduleName: String): Flow<DFModuleState> {
        Log.e(TAG, "📱  getModuleStateV1 moduleName = $moduleName ")
        return states.getOrPut(moduleName) {
            MutableStateFlow(
                if (isModuleInstalled(moduleName)) DFModuleState.Installed
                else DFModuleState.NotInstalled
            )
        }
    }

    override suspend fun loadModule(moduleName: String) {
        Log.d(TAG, "📱  loadModule moduleName = $moduleName ")
        mutex.withLock {
            if (isModuleInstalled(moduleName)) return

            val stateFlow = states.getOrPut(moduleName) {
                MutableStateFlow(DFModuleState.NotInstalled)
            }

            stateFlow.value = DFModuleState.Loading
            Log.d(TAG, "📱  loadModule isModuleInstalled = false ")
            try {
                val request = SplitInstallRequest.newBuilder()
                    .addModule(moduleName)
                    .build()
                Log.d(TAG, "📱  loadModule manager.startInstall(request)")
                manager.startInstall(request)
            } catch (e: Exception) {
                Log.e(TAG, "📱  loadModule Exception $e")
                stateFlow.value = DFModuleState.Error(e.message ?: "Installation failed")
            }
        }
    }

    override fun retryModuleLoad(moduleName: String) {
        Log.d(TAG, "📱  retryModuleLoad =  $moduleName")
        scope.launch { loadModule(moduleName) }
    }

    override fun cleanup() {
        Log.d(TAG, "📱  cleanup unregisterListener ")
        scope.cancel()
        manager.unregisterListener(listener)
    }

    private fun loadPlantsFeature() {
        try {
            val clazz = Class.forName("com.kuru.nextgen.plants.PlantsFeature")
            // Accessing the class will trigger the init block
            val instance =
                clazz.getDeclaredField("INSTANCE").get(null) // If PlantsFeature is an object
            Log.d(TAG, "PlantsFeature loadPlantsFeature successfully")
            prefs?.edit()?.putBoolean("isFeatureInitialized", true)?.apply()
            Log.d(
                TAG,
                "isFeatureInitialized =  ${prefs?.getBoolean("isFeatureInitialized", false)}"
            )
        } catch (e: Exception) {
            Log.d(TAG, "Failed to load PlantsFeature: ${e.message}", e)
        }
        FeatureRegistry.initializeAll()
    }

    companion object {
        private const val TAG = "DynamicFeatureManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: DFManagerDF? = null

        fun getInstance(context: Context): DFManagerDF {
            Log.d(TAG, "📱 Getting DynamicFeatureManager instance")
            return instance ?: synchronized(this) {
                instance ?: DFManagerDF(context).also { instance = it }
            }
        }
    }

    override fun initialize(context: Context): DFModuleManager {
        TODO("Not yet implemented")
    }
}




*/
