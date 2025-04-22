package com.kuru.featureflow.component.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.kuru.featureflow.R
import com.kuru.featureflow.component.boot.DFSDKBootStrap
import com.kuru.featureflow.component.route.DFComponentRoute
import com.kuru.featureflow.component.route.DFComponentUriRouteParser

/**
 * Entry Point for Dynamic Features via Deep Links or Standard Intents
 *
 * This activity serves as the primary entry point for dynamic features within the application. It supports two invocation methods:
 * 1. **Deep Links**: Triggered by external URIs (e.g., from a browser or another app), directing the user to a specific dynamic feature.
 * 2. **Standard Intents**: Initiated internally by the app, typically passing a URI via intent extras to access a dynamic feature.
 *
 * Key responsibilities include:
 * - Initializing the dynamic feature SDK to prepare for module management.
 * - Parsing incoming URIs to identify the target dynamic feature.
 * - Verifying the installation status of required dynamic modules, installing them on-demand if needed.
 * - Facilitating navigation to the dynamic feature using Jetpack Navigation or legacy activity-based approaches.
 *
 * The activity ensures seamless loading and navigation to dynamic features, enhancing modularity and user experience.
 */
class DFComponentActivity : AppCompatActivity() {
    // Singleton instance of the URI route parser used to parse and extract routing information from URIs
    private val router = DFComponentUriRouteParser

    companion object {
        // Constants used throughout the class for consistency and readability
        const val SHARED_PREFS_NAME = "featureflow_prefs" // Name of the shared preferences file used to store app settings
        const val EXTRA_TARGET = "uri" // Key used in intent extras to pass the target URI for standard intents
        private const val TAG = "DFComponentActivity" // Logging tag for debugging and tracking this activity's events
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        try {
            // Initialize the dynamic feature SDK with the application context and shared preferences
            // This prepares the environment to handle dynamic features from deep links or standard intents
            DFSDKBootStrap.initialize(this, SHARED_PREFS_NAME)

            // Determine the intent type: deep link (ACTION_VIEW) or standard intent, and handle accordingly
            if (intent?.action == Intent.ACTION_VIEW) {
                // Entry via deep link; process the external URI
                handleDeepLink(intent)
            } else {
                // Entry via standard intent; process the internal URI from extras
                handleRegularIntent(intent)
            }
        } catch (e: Exception) {
            // Log and handle any initialization errors, closing the activity to avoid unstable states
            Log.e(TAG, "Failed to initialize DFSDKBootStrap due to an unexpected error", e)
            finish() // Exit gracefully
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intents when the activity is already running, typically for deep link updates
        this.intent = intent
        handleDeepLink(intent)
    }

    /**
     * Processes deep link intents to navigate to dynamic features.
     *
     * Called when the activity is launched via a deep link (ACTION_VIEW). It extracts the URI from the intent's data
     * and routes it to the appropriate dynamic feature.
     *
     * @param intent The intent containing the deep link URI in its data field.
     */
    private fun handleDeepLink(intent: Intent?) {
        // Extract the URI from the deep link intent; exit if missing
        val uri = intent?.data?.toString() ?: run {
            Log.w(TAG, "Deep link intent received but contains no URI data")
            finish() // Nothing to process, so close the activity
            return
        }
        // Route the URI to load and navigate to the dynamic feature
        processRoute(uri)
    }

    /**
     * Processes standard intents to navigate to dynamic features.
     *
     * Handles internal app requests via standard intents, extracting the target URI from extras to direct to a dynamic feature.
     *
     * @param intent The intent containing the URI in its extras under EXTRA_TARGET.
     */
    private fun handleRegularIntent(intent: Intent?) {
        // Ensure the intent is valid and contains the required URI extra
        if (intent == null || !intent.hasExtra(EXTRA_TARGET)) {
            Log.w(TAG, "Standard intent is either null or missing the required URI extra")
            finish() // Invalid intent, so exit
            return
        }
        // Extract and process the URI for dynamic feature navigation
        val uri = intent.getStringExtra(EXTRA_TARGET)
        uri?.let { processRoute(it) }
    }

    /**
     * Routes the URI to load and navigate to a dynamic feature.
     *
     * Parses the URI to determine the dynamic feature, ensures the module is installed, and navigates to it.
     *
     * @param uri The URI string specifying the target dynamic feature.
     */
    private fun processRoute(uri: String) {
        // Parse the URI to get the dynamic feature route
        val dynamicRoute = router.extractRoute(uri)

        // Validate the parsed route
        if (dynamicRoute.route.isNotEmpty() && dynamicRoute.status != "failed") {
            val installer = DFSDKBootStrap.getComponentInstaller(this, SHARED_PREFS_NAME)

            // Install the dynamic module if not already present
            if (!installer.isComponentInstalled(dynamicRoute.route)) {
                try {
                    installer.installComponent(dynamicRoute.route)
                    Log.d(TAG, "Successfully installed dynamic module: ${dynamicRoute.route}")
                    navigateToComponent(dynamicRoute)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to install or navigate to dynamic module: ${dynamicRoute.route}", e)
                    finish() // Exit on failure
                }
            } else {
                // Module already installed; proceed to navigation
                Log.d(TAG, "Dynamic module already installed, proceeding to navigation: ${dynamicRoute.route}")
                navigateToComponent(dynamicRoute)
            }
        } else {
            Log.w(TAG, "Route parsing failed or URI is invalid: $uri, status: ${dynamicRoute.status}")
            finish() // Invalid route, so close
        }
    }

    /**
     * Navigates to the dynamic feature based on the parsed route.
     *
     * Supports two navigation methods:
     * 1. **Legacy Navigation**: Uses navigationKey for activity-based navigation.
     * 2. **Jetpack Navigation**: Uses route for NavController-based navigation.
     *
     * @param dynamicRoute The parsed route containing navigation details.
     */
    private fun navigateToComponent(dynamicRoute: DFComponentRoute) {
        try {
            if (dynamicRoute.navigationKey.isNotEmpty()) {
                // Launch legacy activity-based navigation
                startActivity(Intent().apply {
                    setClassName(this@DFComponentActivity, dynamicRoute.navigationKey)
                })
            } else {
                // Use Jetpack Navigation for modern routing
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(dynamicRoute.route)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed for dynamic feature: ${dynamicRoute.route}", e)
            finish() // Exit on navigation failure
        }
    }
}