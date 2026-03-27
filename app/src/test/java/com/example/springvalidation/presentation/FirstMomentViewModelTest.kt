@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.springvalidation.presentation

import android.net.Uri
import app.cash.turbine.Event
import app.cash.turbine.test
import com.example.springvalidation.domain.CapturedPhotoStorage
import com.example.springvalidation.presentation.interaction.FirstMomentUiAction
import com.example.springvalidation.presentation.interaction.FirstMomentUiEvent
import com.example.springvalidation.presentation.interaction.FirstMomentUiState
import com.example.springvalidation.presentation.permission.CameraPermissionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class FirstMomentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FirstMomentViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState_whenNoSavedPhoto`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())
            advanceUntilIdle()
            viewModel.uiState.test {
                val initialState = awaitItem()
                assert(initialState.screenState is FirstMomentUiState.ScreenState.Initial)
                assert(initialState.permission is CameraPermissionState.Unknown)
                assert(!initialState.showPermissionExplanationDialog)
            }
        }

    @Test
    fun `initialState_doesNotRequestPermissionAutomatically`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())
            advanceUntilIdle()
            viewModel.events.test {

                // This collects all events currently in the buffer
                val events: List<Event<FirstMomentUiEvent>> = cancelAndConsumeRemainingEvents()
                assertEquals(0, events.size)
            }
        }

    @Test
    fun `stateRestoration_loadsSavedPhoto`() =
        runTest {
            mockkStatic(Uri::class)
            // 1. Create ONE mock instance
            val mockUri = mockk<Uri>()

            // 2. Always return that SAME instance
            every { Uri.parse(any()) } returns mockUri

            val capturedPhotoStorage = FakeCapturedPhotoStorage()
            capturedPhotoStorage.savePhotoUri(mockUri)

            viewModel = FirstMomentViewModel(capturedPhotoStorage)
            advanceUntilIdle()

            viewModel.uiState.test {
                val initialState = awaitItem()
                assert(initialState.screenState is FirstMomentUiState.ScreenState.Captured)
                assert((initialState.screenState as FirstMomentUiState.ScreenState.Captured).photo == mockUri)
                assert(initialState.screenState.photo != Uri.EMPTY)
            }
        }

    @Test
    fun `primaryButton_withGrantedPermission_emitsCameraEvent`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())

            viewModel.events.test {
                viewModel.onAction(FirstMomentUiAction.OnPermissionSynced(CameraPermissionState.Granted))
                advanceUntilIdle()

                viewModel.onAction(FirstMomentUiAction.OnPrimaryButtonClicked)
                advanceUntilIdle()

                // This collects all events currently in the buffer
                val events: List<Event<FirstMomentUiEvent>> = cancelAndConsumeRemainingEvents()
                val openCameraCount = events.count { it is Event.Item && it.value == FirstMomentUiEvent.OpenCamera }
                assertEquals(1, openCameraCount)
            }
        }

    @Test
    fun `primaryButton_withUnknownPermission_requestPermission`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())

            viewModel.events.test {
                viewModel.onAction(FirstMomentUiAction.OnPrimaryButtonClicked)
                advanceUntilIdle()

                // This collects all events currently in the buffer
                val events: List<Event<FirstMomentUiEvent>> = cancelAndConsumeRemainingEvents()
                val requestCameraPermissionCount = events.count {
                    it is Event.Item && it.value == FirstMomentUiEvent.RequestCameraPermission
                }
                assertEquals(1, requestCameraPermissionCount)
            }
        }

    @Test
    fun `primaryButton_withPermanentDenial_showsDialog`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())

            viewModel.uiState.test {
                viewModel.onAction(FirstMomentUiAction.OnPermissionSynced(CameraPermissionState.PermanentlyDenied))
                advanceUntilIdle()

                viewModel.onAction(FirstMomentUiAction.OnPrimaryButtonClicked)
                advanceUntilIdle()

                val lastState = expectMostRecentItem()
                assert(lastState.showPermissionExplanationDialog)
            }
        }

    @Test
    fun `permissionGrantedResult_opensCamera`() =
        runTest {
            viewModel = FirstMomentViewModel(FakeCapturedPhotoStorage())

            viewModel.events.test {
                viewModel.onAction(FirstMomentUiAction.OnPermissionResult(CameraPermissionState.Granted))
                advanceUntilIdle()

                // This collects all events currently in the buffer
                val events: List<Event<FirstMomentUiEvent>> = cancelAndConsumeRemainingEvents()
                val openCameraCount = events.count {
                    it is Event.Item && it.value == FirstMomentUiEvent.OpenCamera
                }
                assertEquals(1, openCameraCount)
            }
        }

    @Test
    fun `photoCaptured_savesToStorageAndUpdatesState`() =
        runTest {
            mockkStatic(Uri::class)
            // 1. Create ONE mock instance
            val mockUri = mockk<Uri>()

            // 2. Always return that SAME instance
            every { Uri.parse(any()) } returns mockUri

            val capturedPhotoStorage = FakeCapturedPhotoStorage()
            viewModel = FirstMomentViewModel(capturedPhotoStorage)
            advanceUntilIdle()

            // Use our mockUri for the action
            viewModel.onAction(FirstMomentUiAction.OnPhotoCaptured(mockUri))
            advanceUntilIdle()

            viewModel.uiState.test {
                val lastState = expectMostRecentItem()
                assert(lastState.screenState is FirstMomentUiState.ScreenState.Captured)
                assert((lastState.screenState as FirstMomentUiState.ScreenState.Captured).photo == mockUri)
                assert(capturedPhotoStorage.getSavedPhotoUri() == mockUri)
            }
            unmockkStatic(Uri::class)
        }
}

class FakeCapturedPhotoStorage: CapturedPhotoStorage {
    private var savedUri: Uri? = null

    override suspend fun savePhotoUri(uri: Uri) {
        savedUri = uri
    }

    override suspend fun getSavedPhotoUri(): Uri? {
        return savedUri
    }

}