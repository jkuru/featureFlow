package com.kuru.featureflow.component.googleplay

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.kuru.featureflow.component.register.DFComponentRegistry
import kotlinx.coroutines.CoroutineScope

/**
 * Called by Bootstrap 
 */
interface DFGooglePlayInstallerBoot {

    /**
     * Start Split
     * Register Listener
     */
    fun initialize(
        context: Context,
        sharedPreferencesFileName: String,
        DFComponentRegistry: DFComponentRegistry
    ): DFComponentInstaller

    /**
     * Should clean up at Boot level T
     * TODO
     */
    fun cleanup(
        manager: SplitInstallManager,
        scope: CoroutineScope,
        listener: SplitInstallStateUpdatedListener
    )
}