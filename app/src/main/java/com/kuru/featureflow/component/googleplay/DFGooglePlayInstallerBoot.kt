package com.kuru.featureflow.component.googleplay

import android.content.Context
import android.content.SharedPreferences
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
        componentRegistry: DFComponentRegistry,
        scope: CoroutineScope
    ): DFComponentInstaller

}