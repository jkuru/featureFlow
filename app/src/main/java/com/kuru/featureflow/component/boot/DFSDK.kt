package com.kuru.featureflow.component.boot

import android.content.Context
import com.kuru.featureflow.component.googleplay.DFComponentInstaller

interface DFSDK {
    fun getComponentInstaller(context: Context, sharedPreferencesFileName: String): DFComponentInstaller
}
