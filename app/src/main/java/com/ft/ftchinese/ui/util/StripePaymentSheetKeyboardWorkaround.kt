package com.ft.ftchinese.ui.util

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Stripe 19.3.1 uses translucent bottom-sheet Activities that do not reliably
 * resize above OEM keyboards. Keep this scoped to Stripe's sheet Activities.
 */
object StripePaymentSheetKeyboardWorkaround {
    private const val PAYMENT_SHEET_ACTIVITY = "com.stripe.android.paymentsheet.PaymentSheetActivity"
    private const val PAYMENT_OPTIONS_ACTIVITY = "com.stripe.android.paymentsheet.PaymentOptionsActivity"
    private const val BOTTOM_SHEET_ID = "bottom_sheet"
    private const val KEYBOARD_MIN_HEIGHT_DP = 120

    private val registrations = mutableMapOf<Activity, KeyboardAvoidanceRegistration>()

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    apply(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = apply(activity)
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    if (!isStripePaymentSheetActivity(activity)) {
                        return
                    }

                    val registration = registrations.remove(activity) ?: return
                    registration.globalLayoutListener?.let {
                        registration.bottomSheet.viewTreeObserver.removeOnGlobalLayoutListener(it)
                    }
                }
            }
        )
    }

    private fun apply(activity: Activity) {
        if (!isStripePaymentSheetActivity(activity)) {
            return
        }

        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val decorView = activity.window.decorView
        decorView.post {
            val bottomSheet = decorView.findStripeBottomSheet(activity) ?: return@post
            val registration = registrations.getOrPut(activity) {
                KeyboardAvoidanceRegistration(bottomSheet = bottomSheet)
            }
            attachInsetsListener(decorView, registration)
            attachGlobalLayoutListener(decorView, registration)
        }
    }

    private fun attachInsetsListener(decorView: View, registration: KeyboardAvoidanceRegistration) {
        if (registration.insetsAttached) {
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            registration.insetsKeyboardHeight = max(0, imeBottom - navBottom)
            registration.updateTranslation()
            insets
        }
        registration.insetsAttached = true
        ViewCompat.requestApplyInsets(decorView)
    }

    private fun attachGlobalLayoutListener(
        decorView: View,
        registration: KeyboardAvoidanceRegistration
    ) {
        if (registration.globalLayoutListener != null) {
            return
        }

        val minKeyboardHeight =
            (registration.bottomSheet.resources.displayMetrics.density * KEYBOARD_MIN_HEIGHT_DP).toInt()
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val visibleFrame = Rect()
            decorView.getWindowVisibleDisplayFrame(visibleFrame)
            val keyboardHeight = decorView.rootView.height - visibleFrame.bottom

            registration.frameKeyboardHeight =
                if (keyboardHeight > minKeyboardHeight) {
                    keyboardHeight
                } else {
                    0
                }
            registration.updateTranslation()
        }

        registration.bottomSheet.viewTreeObserver.addOnGlobalLayoutListener(listener)
        registration.globalLayoutListener = listener
    }

    private fun View.findStripeBottomSheet(activity: Activity): View? {
        val bottomSheetId = listOf(
            activity.packageName,
            "com.stripe.android.paymentsheet"
        )
            .asSequence()
            .map { activity.resources.getIdentifier(BOTTOM_SHEET_ID, "id", it) }
            .firstOrNull { it != 0 } ?: 0

        if (bottomSheetId == 0) {
            return null
        }

        return findViewById(bottomSheetId)
    }

    private fun isStripePaymentSheetActivity(activity: Activity): Boolean {
        return when (activity.javaClass.name) {
            PAYMENT_SHEET_ACTIVITY,
            PAYMENT_OPTIONS_ACTIVITY -> true
            else -> false
        }
    }

    private data class KeyboardAvoidanceRegistration(
        val bottomSheet: View,
        var insetsAttached: Boolean = false,
        var insetsKeyboardHeight: Int = 0,
        var frameKeyboardHeight: Int = 0,
        var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    ) {
        fun updateTranslation() {
            bottomSheet.translationY = -max(insetsKeyboardHeight, frameKeyboardHeight).toFloat()
        }
    }
}
