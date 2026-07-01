package com.routineremind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.routineremind.app.ui.theme.Danger
import com.routineremind.app.ui.theme.TextSecondary

private enum class Mode { SIGN_IN, SIGN_UP, RESET }

@Composable
fun LoginScreen(vm: AppViewModel) {
    var mode by remember { mutableStateOf(Mode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("RoutineRemind", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text(
            "Your daily routine, on track.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    when (mode) {
                        Mode.SIGN_IN -> "Welcome back"
                        Mode.SIGN_UP -> "Create your account"
                        Mode.RESET -> "Reset password"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (mode != Mode.RESET) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                vm.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Danger, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        when (mode) {
                            Mode.SIGN_IN -> vm.signIn(email, password)
                            Mode.SIGN_UP -> vm.signUp(email, password)
                            Mode.RESET -> vm.resetPassword(email)
                        }
                    },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when (mode) {
                            Mode.SIGN_IN -> "Sign in"
                            Mode.SIGN_UP -> "Create account"
                            Mode.RESET -> "Send reset link"
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (mode == Mode.SIGN_IN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = { mode = Mode.RESET }) { Text("Forgot password?") }
                        TextButton(onClick = { mode = Mode.SIGN_UP }) { Text("Create account") }
                    }
                } else {
                    TextButton(onClick = { mode = Mode.SIGN_IN }) { Text("Back to sign in") }
                }
            }
        }
    }
}
