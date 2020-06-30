package com.ft.ftchinese.ui.login

/**
 * Control the state of a UI with input, button and progress.
 * Those elements have various combinations at different stage,
 * which cannot be described with a simple true/false.
 * Here's the matrix:
 *           Input box | Submit button | Progress indicator
 * Initial     Y             N               N
 * Invalid     Y             N               N
 * Valid       Y             Y               N
 * Progress    N             N               Y
 * API error   Y             Y               N
 * Success     Y             Y               N
 */
enum class FormUIState {
    Initial, // The initial and invalid state
    Progress, // In progress after submit button clicked
    Ready, // Data valid, http request finished.
    Success // Hide progress, disabled all input.
}
