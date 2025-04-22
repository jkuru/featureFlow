package com.kuru.featureflow.component.boot

import android.content.Context

/**
 * Dynamic feature Initialize the Dynamic Feature Component.
 * Dynamic Feature Component is a singleton
 * Bootstrap interface will be called by Cold/warm bootstrapping function
 */
interface DFSDKBoot {

    fun initialize(context: Context,sharedPreferencesFileName:String)

}