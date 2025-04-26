package com.kuru.featureflow.component.googleplay

// Keep existing imports
// Assuming these are correct based on your project structure
// Import your production modules to replace them
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.kuru.featureflow.component.state.DFErrorCode
import com.kuru.featureflow.component.state.DFInstallProgress
import com.kuru.featureflow.component.state.DFInstallationState
import com.kuru.featureflow.di.AppModule
import com.kuru.featureflow.di.FrameworkBindingsModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class) // Added for Robolectric compatibility
@UninstallModules(AppModule::class, FrameworkBindingsModule::class)
@HiltAndroidTest
class DFComponentInstallerManagerTest {

  //  @get:Rule
   var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var installerManager: DFComponentInstallerManager

    @Inject
    lateinit var splitInstallManager: SplitInstallManager

    @Inject
    lateinit var context: Context

    @Captor
    private lateinit var listenerCaptor: ArgumentCaptor<SplitInstallStateUpdatedListener>

    @Captor
    private lateinit var requestCaptor: ArgumentCaptor<SplitInstallRequest>

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testModuleName = "testModule"
    private val testSessionId = 123

    private lateinit var mocksClosable: AutoCloseable

    @Before
    fun setUp() {
        mocksClosable = MockitoAnnotations.openMocks(this)
        hiltRule.inject()
        // Setup default success task for startInstall IF it's always expected to succeed initially
        // Can be overridden in specific tests if needed
        val mockTask: Task<Int> = Tasks.forResult(testSessionId)
        `when`(splitInstallManager.startInstall(ArgumentMatchers.any(SplitInstallRequest::class.java)))
            .thenReturn(mockTask)
        // Mock listener registration to return true (common practice)
       // `when`(splitInstallManager.registerListener(ArgumentMatchers.any())).thenReturn(true)
    }

    @After
    fun tearDown() {
        mocksClosable.close()
    }

    // --- Test Cases ---

    @Test
    fun isComponentInstalled_returnsTrue_whenModuleIsInstalled() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(setOf(testModuleName))
        val result = installerManager.isComponentInstalled(testModuleName)
        assertTrue(result)
        verify(splitInstallManager).installedModules
    }

    @Test
    fun isComponentInstalled_returnsFalse_whenModuleIsNotInstalled() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(emptySet())
        val result = installerManager.isComponentInstalled(testModuleName)
        assertFalse(result)
        verify(splitInstallManager).installedModules
    }

    @Test
    fun installComponent_emitsInstalled_ifModuleAlreadyInstalled() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(setOf(testModuleName))
        val flow = installerManager.installComponent(testModuleName)
        // Using first() might be slightly cleaner if only one item is truly expected
        // val resultProgress = flow.first()
        // assertEquals(InstallationState.Installed, resultProgress.frameworkState)
        // Or keep using toList if multiple "Installed" emissions are possible (unlikely)
        val results = flow.toList()
        assertEquals(1, results.size)
        assertEquals(DFInstallationState.Installed, results.first().frameworkState)

        verify(splitInstallManager).installedModules
        verify(splitInstallManager, never()).startInstall(ArgumentMatchers.any(SplitInstallRequest::class.java))
        verify(splitInstallManager, never()).registerListener(ArgumentMatchers.any(SplitInstallStateUpdatedListener::class.java))
    }

    @Test
    fun installComponent_emitsCorrectStates_forSuccessfulInstallation() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(emptySet())
        // startInstall mock setup moved to @Before for common case

        val sessionStatePending = createMockSessionState(testSessionId, SplitInstallSessionStatus.PENDING)
        val sessionStateDownloading = createMockSessionState(testSessionId, SplitInstallSessionStatus.DOWNLOADING, bytesDownloaded = 500, totalBytes = 1000)
        val sessionStateInstalling = createMockSessionState(testSessionId, SplitInstallSessionStatus.INSTALLING, bytesDownloaded = 1000, totalBytes = 1000)
        val sessionStateInstalled = createMockSessionState(testSessionId, SplitInstallSessionStatus.INSTALLED, bytesDownloaded = 1000, totalBytes = 1000)

        // **FIX 3:** Collect results directly into this list
        val collectedStates = mutableListOf<DFInstallProgress>()

        val job = installerManager.installComponent(testModuleName)
            .onEach { collectedStates.add(it) } // Collect items here
            .onCompletion {
                // Check for unregister only if listener was captured (i.e., install started)
                if (::listenerCaptor.isInitialized && listenerCaptor.value != null) {
                    verify(splitInstallManager).unregisterListener(listenerCaptor.value)
                }
                println("Flow completed or cancelled (Success test)")
            }
            .launchIn(this)

        testDispatcher.scheduler.advanceUntilIdle()

        verify(splitInstallManager).startInstall(requestCaptor.capture())
        verify(splitInstallManager).registerListener(listenerCaptor.capture())
        assertEquals(testModuleName, requestCaptor.value.moduleNames.first())

        val listener = listenerCaptor.value
        listener.onStateUpdate(sessionStatePending)
        testDispatcher.scheduler.advanceUntilIdle()

        listener.onStateUpdate(sessionStateDownloading)
        testDispatcher.scheduler.advanceUntilIdle()

        listener.onStateUpdate(sessionStateInstalling)
        testDispatcher.scheduler.advanceUntilIdle()

        listener.onStateUpdate(sessionStateInstalled)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert against the collected states
        assertEquals(4, collectedStates.size) // PENDING, DOWNLOADING, INSTALLING, INSTALLED
        assertEquals(DFInstallationState.Pending, collectedStates[0].frameworkState)
        assertEquals(DFInstallationState.Downloading(50), collectedStates[1].frameworkState)
        // **FIX 2:** Uncommented this assertion (assuming Installing state IS expected)
        assertEquals(DFInstallationState.Installing(10), collectedStates[2].frameworkState)
        assertEquals(DFInstallationState.Installed, collectedStates[3].frameworkState)

        job.cancel()
    }

    @Test
    fun installComponent_emitsFailedState_onInstallationError() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(emptySet())
        // startInstall mock setup moved to @Before

        val sessionStatePending = createMockSessionState(testSessionId, SplitInstallSessionStatus.PENDING)
        val sessionStateFailed = createMockSessionState(testSessionId, SplitInstallSessionStatus.FAILED, errorCode = com.google.android.play.core.splitinstall.model.SplitInstallErrorCode.NETWORK_ERROR)

        // **FIX 3:** Collect results directly into this list
        val collectedStates = mutableListOf<DFInstallProgress>()

        val job = installerManager.installComponent(testModuleName)
            .onEach { collectedStates.add(it) } // Collect items here
            .onCompletion {
                if (::listenerCaptor.isInitialized && listenerCaptor.value != null) {
                    verify(splitInstallManager).unregisterListener(listenerCaptor.value)
                }
                println("Flow completed or cancelled (Failure test)")
            }
            .launchIn(this)

        testDispatcher.scheduler.advanceUntilIdle()

        verify(splitInstallManager).startInstall(requestCaptor.capture())
        verify(splitInstallManager).registerListener(listenerCaptor.capture())
        assertEquals(testModuleName, requestCaptor.value.moduleNames.first())

        val listener = listenerCaptor.value
        listener.onStateUpdate(sessionStatePending)
        testDispatcher.scheduler.advanceUntilIdle()

        listener.onStateUpdate(sessionStateFailed)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert against the collected states
        assertEquals(2, collectedStates.size) // PENDING, FAILED
        assertEquals(DFInstallationState.Pending, collectedStates[0].frameworkState)
        assertTrue(collectedStates[1].frameworkState is DFInstallationState.Failed)
        assertEquals(DFErrorCode.NETWORK_ERROR, (collectedStates[1].frameworkState as DFInstallationState.Failed).errorCode)

        job.cancel()
    }

    @Test
    fun retryComponentInstall_delegatesAndStartsInstall_whenModuleNotInstalled() = testScope.runTest {
        `when`(splitInstallManager.installedModules).thenReturn(emptySet())
        // startInstall mock setup moved to @Before

        val sessionStatePending = createMockSessionState(testSessionId, SplitInstallSessionStatus.PENDING)

        // **FIX 3:** Collect results directly into this list
        val collectedStates = mutableListOf<DFInstallProgress>()

        val flow = installerManager.retryComponentInstall(testModuleName) // Get the flow first
        val job = flow
            .onEach { collectedStates.add(it) } // Collect items
            .onCompletion {
                if (::listenerCaptor.isInitialized && listenerCaptor.value != null) {
                    verify(splitInstallManager).unregisterListener(listenerCaptor.value)
                }
                println("Flow completed or cancelled (Retry test)")
            }
            .launchIn(this)

        testDispatcher.scheduler.advanceUntilIdle()

        verify(splitInstallManager).startInstall(requestCaptor.capture())
        verify(splitInstallManager).registerListener(listenerCaptor.capture())
        assertEquals(testModuleName, requestCaptor.value.moduleNames.first())

        listenerCaptor.value.onStateUpdate(sessionStatePending)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert against the collected states
        assertTrue(collectedStates.isNotEmpty()) // Ensure something was emitted
        assertEquals(DFInstallationState.Pending, collectedStates.first().frameworkState) // Check the first state

        job.cancel()
    }



    // --- Corrected Helper Function ---
    private fun createMockSessionState(
        // Define parameters matching the variables used below
        sessionId: Int,
        status: Int, // From SplitInstallSessionStatus constants
        errorCode: Int = 0, // Default error code (no error)
        bytesDownloaded: Long = 0L, // Default progress
        totalBytes: Long = 0L, // Default total size
        // Use the testModuleName defined in the class for the default list
        moduleNames: List<String> = listOf(testModuleName)
    ): SplitInstallSessionState {
        // Use Mockito to create a mock, as SplitInstallSessionState.create is limited
        val mockState = Mockito.mock(SplitInstallSessionState::class.java)

        // Setup the mock using the function parameters
        `when`(mockState.sessionId()).thenReturn(sessionId)
        `when`(mockState.status()).thenReturn(status)
        `when`(mockState.errorCode()).thenReturn(errorCode)
        `when`(mockState.bytesDownloaded()).thenReturn(bytesDownloaded)
        `when`(mockState.totalBytesToDownload()).thenReturn(totalBytes)
        `when`(mockState.moduleNames()).thenReturn(moduleNames)

        // Mock other methods IF your code under test uses them (e.g., resolutionIntent)
        // `when`(mockState.resolutionIntent()).thenReturn(null) // Example

        return mockState
    }

}