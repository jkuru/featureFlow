package com.kuru.featureflow.component.googleplay

import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.DFComponentStateStore
import kotlinx.coroutines.flow.Flow

/**
 * This is exposed to Component, state will be moved to state store TODO
 */
interface DFComponentInstaller {
    fun isComponentInstalled(componentName: String): Boolean
    fun getComponentState(componentName: String): Flow<DFComponentStateStore>
    fun installComponent(componentName: String)
    fun retryComponentInstall(componentName: String)
    fun getComponentRegistry(): DFComponentRegistry
}