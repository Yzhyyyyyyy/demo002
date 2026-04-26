package com.example.demo002

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var loginState by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    // 忘记密码相关
    var resetEmail by mutableStateOf("")
        private set
    var showResetDialog by mutableStateOf(false)
        private set
    var resetState by mutableStateOf<ResetPasswordState>(ResetPasswordState.Idle)
        private set

    fun onEmailChanged(value: String) {
        email = value
    }

    fun onPasswordChanged(value: String) {
        password = value
    }

    fun onResetEmailChanged(value: String) {
        resetEmail = value
    }

    fun showResetPasswordDialog() {
        resetEmail = email
        resetState = ResetPasswordState.Idle
        showResetDialog = true
    }

    fun dismissResetPasswordDialog() {
        showResetDialog = false
    }

    private fun isValidEmail(input: String): Boolean {
        return input.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            loginState = LoginState.Error("邮箱和密码不能为空")
            return
        }
        if (!isValidEmail(email)) {
            loginState = LoginState.Error("请输入有效的邮箱地址")
            return
        }
        loginState = LoginState.Loading
        viewModelScope.launch {
            val result = BmobManager.login(email, password)
            loginState = result.fold(
                onSuccess = { resp ->
                    LoginState.Success(resp.objectId ?: "")
                },
                onFailure = { e ->
                    LoginState.Error(e.message ?: "登录失败")
                }
            )
        }
    }

    fun register() {
        if (email.isBlank() || password.isBlank()) {
            loginState = LoginState.Error("邮箱和密码不能为空")
            return
        }
        if (!isValidEmail(email)) {
            loginState = LoginState.Error("请输入有效的邮箱地址")
            return
        }
        if (password.length < 6) {
            loginState = LoginState.Error("密码长度至少6位")
            return
        }
        loginState = LoginState.Loading
        viewModelScope.launch {
            val result = BmobManager.register(email, password)
            loginState = result.fold(
                onSuccess = { resp ->
                    LoginState.Success(resp.objectId ?: "")
                },
                onFailure = { e ->
                    LoginState.Error(e.message ?: "注册失败")
                }
            )
        }
    }

    fun sendResetPasswordEmail() {
        if (resetEmail.isBlank()) {
            resetState = ResetPasswordState.Error("请输入邮箱地址")
            return
        }
        if (!isValidEmail(resetEmail)) {
            resetState = ResetPasswordState.Error("请输入有效的邮箱地址")
            return
        }
        resetState = ResetPasswordState.Loading
        viewModelScope.launch {
            val result = BmobManager.requestPasswordReset(resetEmail)
            resetState = result.fold(
                onSuccess = { ResetPasswordState.Success },
                onFailure = { e ->
                    ResetPasswordState.Error(e.message ?: "发送失败")
                }
            )
        }
    }

    fun resetLoginState() {
        loginState = LoginState.Idle
    }
}
