@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.springvalidation.presentation.screens

import app.cash.turbine.test
import com.example.springvalidation.domain.DraftState
import com.example.springvalidation.domain.DraftStorage
import com.example.springvalidation.presentation.interaction.NewNoteUiAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NewNoteViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: NewNoteViewModel
    private lateinit var draftStorage: DraftStorage


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        draftStorage = FakeDraftStorage()
        viewModel = NewNoteViewModel(draftStorage)
    }

    @After
    fun tearDown() {
        // Critical: Must reset or subsequent tests will crash
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState_isEmptyByDefault`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals("", initialState.title)
            assertEquals("", initialState.description)
            assertEquals(false, initialState.keepDraft)
        }
    }

    @Test
    fun `draft_isNotRestored_whenKeepDraftIsOff`() = runTest {
        viewModel.onAction(NewNoteUiAction.TitleChanged("Test Title"))
        viewModel.onAction(NewNoteUiAction.DescriptionChanged("Test Description"))
        advanceUntilIdle()

        val newViewModel = NewNoteViewModel(draftStorage)
        newViewModel.uiState.test {
            val lastState = expectMostRecentItem()
            assertEquals("", lastState.title)
            assertEquals("", lastState.description)
            assertEquals(false, lastState.keepDraft)
        }
    }

    @Test
    fun `draft_isRestored_whenKeepDraftIsOn`() = runTest {
        viewModel.onAction(NewNoteUiAction.TitleChanged("Test Title"))
        viewModel.onAction(NewNoteUiAction.DescriptionChanged("Test Description"))
        viewModel.onAction(NewNoteUiAction.KeepDraftChanged(true))
        advanceUntilIdle()

        val newViewModel = NewNoteViewModel(draftStorage)

        newViewModel.uiState.test {
            val lastState = expectMostRecentItem()
            assertEquals("Test Title", lastState.title)
            assertEquals("Test Description", lastState.description)
            assertEquals(true, lastState.keepDraft)
        }
    }


    @Test
    fun `draft_isNotCleared_whenKeepDraftToggledOff`() = runTest {
        val noteTitle = "My Note Title"
        val noteDescription = "My Note Description"
        viewModel.uiState.test {
            viewModel.onAction(NewNoteUiAction.TitleChanged(noteTitle))
            viewModel.onAction(NewNoteUiAction.DescriptionChanged(noteDescription))
            viewModel.onAction(NewNoteUiAction.KeepDraftChanged(true))
            viewModel.onAction(NewNoteUiAction.KeepDraftChanged(false))
            advanceUntilIdle()
            val lastState = expectMostRecentItem()
            assertEquals(noteTitle, lastState.title)
            assertEquals(noteDescription, lastState.description)
            assertEquals(false, lastState.keepDraft)
        }
    }
}

class FakeDraftStorage : DraftStorage {

    private val _draftFlow = MutableSharedFlow<DraftState>(replay = 1)

    override val draftFlow: Flow<DraftState>
        get() = _draftFlow

    override suspend fun saveDraft(title: String, description: String) {
        _draftFlow.emit(DraftState(title, description))
    }

    override suspend fun clearDraft() {
        _draftFlow.emit(DraftState())
    }
}