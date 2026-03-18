package com.example.springvalidation.presentation

import app.cash.turbine.test
import com.example.springvalidation.presentation.interaction.NewChapterUiAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class NewChapterViewModelTest {
    private lateinit var viewModel: NewChapterViewModel

    @Before
    fun setup() {
        viewModel = NewChapterViewModel()
    }

    @Test
    fun `beginButton_disabled_whenTitleIsEmpty`() = runTest {
        viewModel.uiState.test {
            viewModel.onAction(NewChapterUiAction.OnChapterTitleChange("  "))
            viewModel.onAction(NewChapterUiAction.OnConfidenceLevelChange(5f))
            viewModel.onAction(NewChapterUiAction.OnReadinessCheckboxChange(true))
            val newState = expectMostRecentItem()
            assertFalse(newState.isBeginButtonEnabled)
        }
    }

    @Test
    fun `beginButton_disabled_whenConfidenceBelowThreshold`() = runTest {
        viewModel.uiState.test {
            viewModel.onAction(NewChapterUiAction.OnChapterTitleChange("Chapter 1"))
            viewModel.onAction(NewChapterUiAction.OnConfidenceLevelChange(3f))
            viewModel.onAction(NewChapterUiAction.OnReadinessCheckboxChange(true))
            val newState = expectMostRecentItem()
            assertFalse(newState.isBeginButtonEnabled)
        }
    }

    @Test
    fun `beginButton_enabled_whenConfidenceAtThreshold`() = runTest {
        viewModel.uiState.test {
            viewModel.onAction(NewChapterUiAction.OnChapterTitleChange("Chapter 1"))
            viewModel.onAction(NewChapterUiAction.OnConfidenceLevelChange(4f))
            viewModel.onAction(NewChapterUiAction.OnReadinessCheckboxChange(true))
            val newState = expectMostRecentItem()
            assertTrue(newState.isBeginButtonEnabled)
        }

    }

    @Test
    fun `beginButton_disabled_whenCheckboxIsUnchecked`() = runTest {
        viewModel.uiState.test {
            viewModel.onAction(NewChapterUiAction.OnChapterTitleChange("Chapter 1"))
            viewModel.onAction(NewChapterUiAction.OnConfidenceLevelChange(5f))
            viewModel.onAction(NewChapterUiAction.OnReadinessCheckboxChange(false))
            val newState = expectMostRecentItem()
            assertFalse(newState.isBeginButtonEnabled)
        }
    }

    @Test
    fun `beginButton_enabled_whenAllInputsAreValid`() = runTest {
        viewModel.uiState.test {
            viewModel.onAction(NewChapterUiAction.OnChapterTitleChange("Chapter 1"))
            viewModel.onAction(NewChapterUiAction.OnConfidenceLevelChange(5f))
            viewModel.onAction(NewChapterUiAction.OnReadinessCheckboxChange(true))
            val newState = expectMostRecentItem()
            assertTrue(newState.isBeginButtonEnabled)
        }
    }

}