package com.example.springvalidation.presentation

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.springvalidation.presentation.interaction.FirstMomentUiState
import org.junit.Rule
import org.junit.Test

class NewChapterScreenRootTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialState_rendersPromptAndButton() {
        composeTestRule.setContent {
            NewChapterScreen(
                uiState = FirstMomentUiState(
                    screenState = FirstMomentUiState.ScreenState.Initial
                ),
                onAction = {}
            )
        }

        composeTestRule
            .onNodeWithText(text = "Capture your first spring moment")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Take Spring Photo")
            .assertIsDisplayed()


        composeTestRule
            .onNodeWithText("The beginning of a new season")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText("Update moment")
            .assertIsNotDisplayed()

        composeTestRule
            .onNodeWithText("Camera access needed")
            .assertIsNotDisplayed()
    }

    @Test
    fun initialState_rendersSuccessUI() {
        composeTestRule.setContent {
            NewChapterScreen(
                uiState = FirstMomentUiState(
                    screenState = FirstMomentUiState.ScreenState.Captured(Uri.EMPTY)
                ),
                onAction = {}
            )
        }

        composeTestRule
            .onNodeWithText("First Spring Moment")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("The beginning of a new season")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Update moment")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Capture your first spring moment")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText("Take Spring Photo")
            .assertIsNotDisplayed()

    }

    @Test
    fun initialState_rendersWhenShown() {
        composeTestRule.setContent {
            NewChapterScreen(
                uiState = FirstMomentUiState(
                    screenState = FirstMomentUiState.ScreenState.Captured(Uri.EMPTY),
                    showPermissionExplanationDialog = true
                ),
                onAction = {}
            )
        }

        composeTestRule
            .onNodeWithText("Camera access needed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Open Settings")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cancel")
            .assertIsDisplayed()
    }


}
