package com.example.demo002

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val loginState by viewModel::loginState

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    // 忘记密码弹窗
    if (viewModel.showResetDialog) {
        val resetState by viewModel::resetState

        LaunchedEffect(resetState) {
            when (resetState) {
                is ResetPasswordState.Success -> {
                    Log.i("MindHunter", "邮件发送成功")
                    Toast.makeText(context, "重置邮件已发送，请注意查收（包含垃圾箱）", Toast.LENGTH_LONG).show()
                    viewModel.dismissResetPasswordDialog()
                }
                is ResetPasswordState.Error -> {
                    val msg = (resetState as ResetPasswordState.Error).message
                    Log.e("MindHunter", "重置邮件失败: $msg")
                    Toast.makeText(context, "发送失败: $msg", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissResetPasswordDialog() },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            title = {
                Text(
                    text = "重置密码",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "请输入您注册时的邮箱，我们将向您发送一封密码重置邮件。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = viewModel.resetEmail,
                        onValueChange = viewModel::onResetEmailChanged,
                        label = { Text("邮箱地址") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = resetState !is ResetPasswordState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    if (resetState is ResetPasswordState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendResetPasswordEmail()
                    },
                    enabled = resetState !is ResetPasswordState.Loading
                ) {
                    Text("发送邮件")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissResetPasswordDialog() },
                    enabled = resetState !is ResetPasswordState.Loading
                ) {
                    Text("取消")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "任务管理器",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请登录或注册账号",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 邮箱输入框
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("邮箱地址") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = loginState !is LoginState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 密码输入框
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("密码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = loginState !is LoginState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            // 忘记密码
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    viewModel.showResetPasswordDialog()
                }) {
                    Text(
                        text = "忘记密码？",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 加载中
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 错误提示
            if (loginState is LoginState.Error) {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 登录按钮
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = loginState !is LoginState.Loading
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 注册按钮
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.register()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = loginState !is LoginState.Loading
            ) {
                Text("注册")
            }
        }
    }
}
