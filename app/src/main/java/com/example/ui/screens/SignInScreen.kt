package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.VeloraLogo
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val AUTH_TAG = "VeloraAuth"

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google sign-in failed. Please try again.")
        }
    }
}

fun signOutFromVelora(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun SignInScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var authMethodTab by remember { mutableIntStateOf(0) } // 0 = Email, 1 = Phone
    var isCreateAccount by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope
        )
    }

    fun submitEmailAuth() {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (isCreateAccount && password != confirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }
        errorMessage = null
        infoMessage = null
        isLoading = true

        scope.launch {
            try {
                if (isCreateAccount) {
                    val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    val cleanName = displayName.trim()
                    if (cleanName.isNotBlank()) {
                        result.user?.updateProfile(
                            userProfileChangeRequest { this.displayName = cleanName }
                        )?.await()
                    }
                    result.user?.sendEmailVerification()
                } else {
                    Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
                }
                isLoading = false
                onAuthSuccess()
            } catch (e: Exception) {
                Log.e(AUTH_TAG, "Email auth failed", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Authentication failed. Please check your credentials."
            }
        }
    }

    fun startPhoneVerification(useResend: Boolean = false) {
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = "Phone verification requires an active screen."
            return
        }
        val cleanPhone = phoneNumber.trim().replace(" ", "")
        if (!cleanPhone.startsWith("+") || cleanPhone.length < 8) {
            errorMessage = "Enter phone number with country code (e.g. +15551234567)."
            return
        }
        errorMessage = null
        infoMessage = null
        isLoading = true

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                scope.launch {
                    try {
                        Firebase.auth.signInWithCredential(credential).await()
                        isLoading = false
                        onAuthSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Phone sign-in failed."
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(AUTH_TAG, "Phone verification failed", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Phone verification failed. Ensure Phone Sign-In is enabled in Firebase Console."
            }

            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isLoading = false
                verificationId = verId
                resendToken = token
                infoMessage = "Verification code sent to $cleanPhone."
            }
        }

        val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(cleanPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (useResend && resendToken != null) {
            builder.setForceResendingToken(resendToken!!)
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun submitPhoneCode() {
        val verId = verificationId ?: return
        val cleanCode = smsCode.trim()
        if (cleanCode.length < 4) {
            errorMessage = "Please enter the verification code from SMS."
            return
        }
        errorMessage = null
        isLoading = true
        scope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(verId, cleanCode)
                Firebase.auth.signInWithCredential(credential).await()
                isLoading = false
                onAuthSuccess()
            } catch (e: Exception) {
                Log.e(AUTH_TAG, "Phone OTP sign-in failed", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Invalid verification code."
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VeloraCrimson,
        unfocusedBorderColor = VeloraOutline,
        focusedTextColor = VeloraOnBackground,
        unfocusedTextColor = VeloraOnBackground,
        focusedLabelColor = VeloraCrimson,
        unfocusedLabelColor = VeloraOnSurfaceMuted,
        cursorColor = VeloraCrimson
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_nebula),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to VeloraBackground.copy(alpha = 0.6f),
                        0.38f to VeloraBackground.copy(alpha = 0.85f),
                        1f to VeloraBackground
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeloraLogo()
                Surface(
                    color = VeloraMatchGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "100% FREE",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraMatchGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Unlimited cinema.\nZero subscriptions.",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = VeloraOnBackground
                )
                Text(
                    text = "Sign in or create your free Notflix account to unlock curated movies, series, profiles, watchlists, and HLS playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeloraOnSurfaceMuted
                )

                Surface(
                    color = VeloraSurfaceElevated.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = authMethodTab,
                            containerColor = Color.Transparent,
                            contentColor = VeloraOnBackground,
                            indicator = { positions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[authMethodTab]),
                                    color = VeloraCrimson
                                )
                            }
                        ) {
                            Tab(
                                selected = authMethodTab == 0,
                                onClick = {
                                    authMethodTab = 0
                                    errorMessage = null
                                    infoMessage = null
                                },
                                modifier = Modifier.testTag("auth_tab_email"),
                                icon = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                text = { Text("Email") }
                            )
                            Tab(
                                selected = authMethodTab == 1,
                                onClick = {
                                    authMethodTab = 1
                                    errorMessage = null
                                    infoMessage = null
                                },
                                modifier = Modifier.testTag("auth_tab_phone"),
                                icon = { Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                text = { Text("Phone") }
                            )
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = VeloraError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }

                        if (infoMessage != null) {
                            Surface(
                                color = VeloraMatchGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = infoMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraMatchGreen,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }

                        if (authMethodTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isCreateAccount = false
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("email_signin_mode_button"),
                                    border = BorderStroke(1.dp, if (!isCreateAccount) VeloraCrimson else VeloraOutline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!isCreateAccount) VeloraCrimson.copy(alpha = 0.16f) else Color.Transparent,
                                        contentColor = VeloraOnBackground
                                    )
                                ) {
                                    Text("Sign In")
                                }
                                OutlinedButton(
                                    onClick = {
                                        isCreateAccount = true
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("email_signup_mode_button"),
                                    border = BorderStroke(1.dp, if (isCreateAccount) VeloraCrimson else VeloraOutline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isCreateAccount) VeloraCrimson.copy(alpha = 0.16f) else Color.Transparent,
                                        contentColor = VeloraOnBackground
                                    )
                                ) {
                                    Text("Create Account")
                                }
                            }

                            if (isCreateAccount) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Display Name") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_name_input")
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input")
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (isCreateAccount) ImeAction.Next else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { if (!isCreateAccount) submitEmailAuth() }
                                ),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input")
                            )

                            if (isCreateAccount) {
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { submitEmailAuth() }),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_confirm_password_input")
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { showForgotPasswordDialog = true },
                                        modifier = Modifier.testTag("forgot_password_button")
                                    ) {
                                        Text("Forgot Password?", color = VeloraOnSurfaceMuted)
                                    }
                                }
                            }

                            Button(
                                onClick = { submitEmailAuth() },
                                enabled = !isLoading && !isCheckingAutoSignIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("email_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VeloraCrimson,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Please wait...")
                                } else {
                                    Text(
                                        text = if (isCreateAccount) "Create Account with Email" else "Sign In with Email",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Enter your phone number with country code (e.g. +15551234567) to sign in or create an account via SMS code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number (+CountryCode)") },
                                placeholder = { Text("+15551234567") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                singleLine = true,
                                enabled = verificationId == null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_phone_input")
                            )

                            if (verificationId == null) {
                                Button(
                                    onClick = { startPhoneVerification(useResend = false) },
                                    enabled = !isLoading && !isCheckingAutoSignIn,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("send_phone_code_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeloraCrimson,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Send Verification Code",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = smsCode,
                                    onValueChange = { smsCode = it },
                                    label = { Text("6-Digit SMS Code") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { submitPhoneCode() }),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_sms_code_input")
                                )

                                Button(
                                    onClick = { submitPhoneCode() },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("verify_phone_code_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeloraCrimson,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Verify & Sign In",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        verificationId = null
                                        smsCode = ""
                                        infoMessage = null
                                    }) {
                                        Text("Change Number", color = VeloraOnSurfaceMuted)
                                    }
                                    TextButton(onClick = { startPhoneVerification(useResend = true) }) {
                                        Text("Resend Code", color = VeloraCrimson)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = VeloraOutline)
                            Text("OR", style = MaterialTheme.typography.labelMedium, color = VeloraOnSurfaceMuted)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = VeloraOutline)
                        }

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading && !isCheckingAutoSignIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VeloraOutline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = VeloraBackground.copy(alpha = 0.65f),
                                contentColor = VeloraOnBackground
                            )
                        ) {
                            if (isCheckingAutoSignIn) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VeloraOnBackground, strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Checking Google session...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email.trim()) }
        var resetError by remember { mutableStateOf<String?>(null) }
        var isSendingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Reset Password", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the email address associated with your Notflix account to receive a password reset link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    resetError?.let {
                        Text(it, color = VeloraError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = resetEmail.trim()
                        if (!trimmed.contains("@")) {
                            resetError = "Please enter a valid email address."
                            return@Button
                        }
                        isSendingReset = true
                        resetError = null
                        scope.launch {
                            try {
                                Firebase.auth.sendPasswordResetEmail(trimmed).await()
                                isSendingReset = false
                                showForgotPasswordDialog = false
                                infoMessage = "Password reset email sent to $trimmed."
                            } catch (e: Exception) {
                                isSendingReset = false
                                resetError = e.localizedMessage ?: "Failed to send reset email."
                            }
                        }
                    },
                    enabled = !isSendingReset,
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) {
                    Text(if (isSendingReset) "Sending..." else "Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingReset
                ) {
                    Text("Cancel", color = VeloraOnSurfaceMuted)
                }
            }
        )
    }
}
