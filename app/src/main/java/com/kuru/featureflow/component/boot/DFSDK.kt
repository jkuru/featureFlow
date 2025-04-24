package com.kuru.featureflow.component.boot

import android.content.Context
import com.kuru.featureflow.component.googleplay.DFComponentInstaller
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.DFComponentStateStore

interface DFSDK {
    fun getComponentInstaller(context: Context, sharedPreferencesFileName: String): DFComponentInstaller
    fun getStateStore(): DFComponentStateStore
    fun getComponentRegistry(): DFComponentRegistry
}
