package com.kuru.featureflow.component.googleplay

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.InstallationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex

class DFGooglePlayInstallerBootStrap : DFGooglePlayInstallerBoot {
    private val _installStates = MutableSharedFlow<Pair<String, InstallationState>>(extraBufferCapacity = 10)
    override fun initialize(
        context: Context,
        componentRegistry: DFComponentRegistry,
        scope: CoroutineScope
    ): DFComponentInstaller {
        val manager = SplitInstallManagerFactory.create(context)
        val mutex = Mutex()
        val installer = DFComponentInstallerManager(manager, scope, mutex, _installStates)
        return installer
    }

    companion object {
        private const val TAG = "DynamicFeatureManager"
    }
}