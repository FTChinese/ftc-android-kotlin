package com.ft.ftchinese.user

/**
 * Used by SignInFragment and SignUpFragment to tell
 * host activity what to do at various stages of
 * checking user account.
 * Host activities are CredentialsActivity and BindEmailActivity.
 */
interface OnCredentialsListener {
    fun onProgress(show: Boolean)
    /**
     * What to do if email already exists
     */
    fun onEmailFound(email: String)

    /**
     * What to do if email does not exist
     */
    fun onEmailNotFound(email: String)

    /**
     * What to do if user logged in
     */
    fun onLoggedIn(userId: String)

    /**
     * What to do if user signed up
     */
    fun onSignedUp(userId: String)
}