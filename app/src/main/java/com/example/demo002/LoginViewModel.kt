package com.example.demo002

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 登录视图模型
 * 使用 EncryptedSharedPreferences 加密存储 Token
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.NotLoggedIn)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 加密存储
    private val masterKey = MasterKey.Builder(getApplication())
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        getApplication(),
        "encrypted_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        viewModelScope.launch {
            checkSavedLoginState()
        }
    }

    /**
     * 检查本地保存的登录状态
     */
    private fun checkSavedLoginState() {
        val token = encryptedPrefs.getString("auth_token", null)
        val userId = encryptedPrefs.getString("user_id", null)
        val userEmail = encryptedPrefs.getString("user_email", null)

        if (token != null && userId != null) {
            _loginState.value = LoginState.LoggedIn(
                userId = userId,
                userEmail = userEmail ?: "",
                token = token
            )
        }
    }

    /**
     * 使用邮箱和密码登录
     */
    suspend fun loginWithEmail(email: String, password: String) {
        _isLoading.value = true
        try {
            val user = LeanCloudManager.loginWithEmail(email, password)

            saveLoginInfo(
                userId = user.objectId,
                userEmail = user.email ?: email,
                token = user.sessionToken ?: ""
            )

            _loginState.value = LoginState.LoggedIn(
                userId = user.objectId,
                userEmail = user.email ?: email,
                token = user.sessionToken ?: ""
            )

        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 使用手机号和验证码登录
     */
    suspend fun loginWithPhone(phone: String, code: String) {
        _isLoading.value = true
        try {
            val user = LeanCloudManager.loginWithMobilePhone(phone, code)

            saveLoginInfo(
                userId = user.objectId,
                userEmail = user.email ?: "",
                token = user.sessionToken ?: ""
            )

            _loginState.value = LoginState.LoggedIn(
                userId = user.objectId,
                userEmail = user.email ?: "",
                token = user.sessionToken ?: ""
            )

        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 注册新用户
     */
    suspend fun signUp(username: String, email: String, password: String) {
        _isLoading.value = true
        try {
            val user = LeanCloudManager.signUp(username, email, password)

            saveLoginInfo(
                userId = user.objectId,
                userEmail = email,
                token = user.sessionToken ?: ""
            )

            _loginState.value = LoginState.LoggedIn(
                userId = user.objectId,
                userEmail = email,
                token = user.sessionToken ?: ""
            )

        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 请求短信验证码
     */
    suspend fun requestSMSCode(phone: String) {
        LeanCloudManager.requestSMSCode(phone)
    }

    /**
     * 登出
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                LeanCloudManager.logout()
                clearLoginInfo()
                _loginState.value = LoginState.NotLoggedIn
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加密保存登录信息
     */
    private fun saveLoginInfo(userId: String, userEmail: String, token: String) {
        encryptedPrefs.edit()
            .putString("user_id", userId)
            .putString("user_email", userEmail)
            .putString("auth_token", token)
            .apply()
    }

    /**
     * 清除登录信息
     */
    private fun clearLoginInfo() {
        encryptedPrefs.edit()
            .remove("user_id")
            .remove("user_email")
            .remove("auth_token")
            .apply()
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return _loginState.value is LoginState.LoggedIn
    }

    /**
     * 获取当前用户信息
     */
    fun getCurrentUserInfo(): UserInfo? {
        return when (val state = _loginState.value) {
            is LoginState.LoggedIn -> UserInfo(
                userId = state.userId,
                email = state.userEmail
            )
            else -> null
        }
    }
}

/**
 * 登录状态
 */
sealed class LoginState {
    object NotLoggedIn : LoginState()
    data class LoggedIn(
        val userId: String,
        val userEmail: String,
        val token: String
    ) : LoginState()
}

/**
 * 用户信息
 */
data class UserInfo(
    val userId: String,
    val email: String
)
