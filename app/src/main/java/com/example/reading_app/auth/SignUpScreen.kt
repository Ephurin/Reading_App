package com.example.reading_app.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignUpScreen(authViewModel: AuthViewModel = viewModel(), onLoginClicked: () -> Unit) {
    val username by authViewModel.username.collectAsState()
    val password by authViewModel.password.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { authViewModel.onUsernameChange(it) },
            label = { Text("Username") },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { authViewModel.onPasswordChange(it) },
            label = { Text("Password") },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = { authViewModel.signUp() }) {
            Text("Sign Up")
        }
        TextButton(onClick = onLoginClicked) {
            Text("Already have an account? Login")
        }
    }
}
