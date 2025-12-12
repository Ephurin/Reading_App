package com.example.reading_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reading_app.auth.AuthViewModel
import com.example.reading_app.auth.PasswordChangeStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onNavigateBack: () -> Unit, authViewModel: AuthViewModel = viewModel()) {
    val currentUsername by authViewModel.username.collectAsState()
    val currentEmail by authViewModel.email.collectAsState()
    val passwordChangeStatus by authViewModel.passwordChangeStatus.collectAsState()

    var newUsername by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordMismatchError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUsername, currentEmail) {
        newUsername = currentUsername
        newEmail = currentEmail
    }

    LaunchedEffect(passwordChangeStatus) {
        when (passwordChangeStatus) {
            PasswordChangeStatus.SUCCESS -> {
                scope.launch { snackbarHostState.showSnackbar("Mật khẩu đã được thay đổi thành công") }
                authViewModel.resetPasswordChangeStatus()
                onNavigateBack()
            }
            PasswordChangeStatus.INCORRECT_PASSWORD -> {
                scope.launch { snackbarHostState.showSnackbar("Mật khẩu cũ không chính xác") }
                authViewModel.resetPasswordChangeStatus()
            }
            PasswordChangeStatus.NONE -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sửa thông tin tài khoản") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Tên người dùng") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Password fields
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Mật khẩu cũ") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it 
                    passwordMismatchError = it != confirmNewPassword && confirmNewPassword.isNotEmpty()
                },
                label = { Text("Mật khẩu mới") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { 
                    confirmNewPassword = it 
                    passwordMismatchError = newPassword != it
                },
                label = { Text("Xác nhận mật khẩu mới") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordMismatchError
            )
            if (passwordMismatchError) {
                Text(
                    text = "Mật khẩu mới không khớp",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (newPassword.isNotEmpty() && newPassword == confirmNewPassword) {
                        authViewModel.changePassword(oldPassword, newPassword)
                    }
                    authViewModel.updateUserProfile(newUsername, newEmail)
                    if (newPassword.isEmpty()) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !passwordMismatchError
            ) {
                Text("Lưu thay đổi")
            }
        }
    }
}
