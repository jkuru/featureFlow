package com.kuru.featureflow.component.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.kuru.featureflow.component.boot.DFSDKBootStrap
import com.kuru.featureflow.component.route.DFComponentRoute
import com.kuru.featureflow.component.route.DFComponentUriRouteParser
import com.kuru.featureflow.component.state.InstallationState
import com.kuru.featureflow.ui.theme.FeatureFlowTheme

class DFComponentActivity : ComponentActivity() {
    private val router = DFComponentUriRouteParser
    private lateinit var viewModel: DFComponentViewModel

    companion object {
        const val SHARED_PREFS_NAME = "featureflow_prefs"
        const val EXTRA_TARGET = "uri"
        private const val TAG = "DFComponentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            DFSDKBootStrap.initialize(this, SHARED_PREFS_NAME)
            viewModel = DFComponentViewModelFactory(this).create(DFComponentViewModel::class.java)
            handleIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DFSDKBootStrap", e)
            finish()
        }

        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            FeatureFlowTheme(darkTheme = isDarkTheme) {
                DFComponentScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_VIEW -> handleDeepLink(intent)
            intent?.hasExtra(EXTRA_TARGET) == true -> handleRegularIntent(intent)
            else -> {
                Log.w(TAG, "Invalid intent received")
                finish()
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data?.toString() ?: run {
            Log.w(TAG, "Deep link intent received but contains no URI data")
            finish()
            return
        }
        processRoute(uri)
    }

    private fun handleRegularIntent(intent: Intent?) {
        val uri = intent?.getStringExtra(EXTRA_TARGET) ?: run {
            Log.w(TAG, "Standard intent is missing the required URI extra")
            finish()
            return
        }
        processRoute(uri)
    }

    private fun processRoute(uri: String) {
        val dynamicRoute = router.extractRoute(uri)

        if (dynamicRoute.route.isNotEmpty() && dynamicRoute.status != "failed") {
            val stateStore = DFSDKBootStrap.getStateStore()

            if (stateStore.getInstallationState(dynamicRoute.route) !is InstallationState.Installed) {
                try {
                    // Dispatch intent to ViewModel to handle installation
                    viewModel.processIntent(DFComponentIntent.InstallComponent(dynamicRoute.route))
                } catch (e: Exception) {
                    stateStore.setInstallationState(
                        dynamicRoute.route,
                        InstallationState.Failed(e.message ?: "Unknown error")
                    )
                    Log.e(
                        TAG,
                        "Failed to start installation for dynamic module: ${dynamicRoute.route}",
                        e
                    )
                }
            } else {
                Log.d(TAG, "Dynamic module already installed: ${dynamicRoute.route}")
                navigateToComponent(dynamicRoute)
            }
        } else {
            Log.w(TAG, "Invalid route: $uri, status: ${dynamicRoute.status}")
        }
    }

    private fun navigateToComponent(dynamicRoute: DFComponentRoute) {
        try {
            viewModel.processIntent(DFComponentIntent.LoadFeature(dynamicRoute.route))
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed for dynamic feature: ${dynamicRoute.route}", e)
        }
    }
}