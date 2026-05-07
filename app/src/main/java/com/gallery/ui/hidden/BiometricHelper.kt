package com.gallery.ui.hidden

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class BiometricHelper(private val activity: FragmentActivity) {

    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        object NotEnrolled : AuthResult()
        object HardwareUnavailable : AuthResult()
    }

    suspend fun authenticate(): AuthResult = suspendCancellableCoroutine { cont ->
        val manager = BiometricManager.from(activity)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            cont.resume(AuthResult.Success)
                        }
                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            cont.resume(AuthResult.Error(errorCode, errString.toString()))
                        }
                        override fun onAuthenticationFailed() {}
                    }
                )
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Hidden Album")
                    .setSubtitle("Authenticate to view hidden photos")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(promptInfo)
                cont.invokeOnCancellation { prompt.cancelAuthentication() }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                cont.resume(AuthResult.NotEnrolled)
            else ->
                cont.resume(AuthResult.HardwareUnavailable)
        }
    }
}
