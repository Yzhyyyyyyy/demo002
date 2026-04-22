package com.example.demo002

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════════
//  LeanCloud REST API 数据类
// ══════════════════════════════════════════════

data class CloudUser(
    val objectId: String = "",
    val username: String = "",
    val email: String = "",
    val mobilePhoneNumber: String = "",
    val sessionToken: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CloudTask(
    val objectId: String = "",
    val title: String = "",
    val note: String = "",
    val dueDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val priority: String = "MEDIUM",
    val isDone: Boolean = false,
    val tagLabels: String = "",
    val location: String = "",
    val reminderOffset: Int? = null,
    val localId: Int = 0,
    val syncStatus: String = "synced",
    val deleted: Boolean = false,
    val userId: String = "",
    @SerializedName("clientUpdatedAt")
    val clientUpdatedAt: Long = 0L,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class LeanCloudSignUpRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val mobilePhoneNumber: String? = null
)

data class LeanCloudLoginRequest(
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val mobilePhoneNumber: String? = null,
    val smsCode: String? = null
)

data class LeanCloudSmsCodeRequest(
    val mobilePhoneNumber: String
)

data class LeanCloudBatchRequestItem(
    val method: String,
    val path: String,
    val body: Any?
)

data class LeanCloudQueryResult(
    val results: List<CloudTask> = emptyList()
)

data class LeanCloudBatchResult(
    val success: CloudTask? = null,
    val error: Any? = null
)

// ══════════════════════════════════════════════
//  Retrofit 接口
// ══════════════════════════════════════════════

interface LeanCloudApi {

    @POST("1.1/users")
    suspend fun signUp(@Body request: LeanCloudSignUpRequest): CloudUser

    @POST("1.1/login")
    suspend fun loginWithUsername(@Body request: LeanCloudLoginRequest): CloudUser

    @GET("1.1/users/me")
    suspend fun getCurrentUser(@Header("X-LC-Session") sessionToken: String): CloudUser

    @POST("1.1/requestLoginSmsCode")
    suspend fun requestLoginSmsCode(@Body request: LeanCloudSmsCodeRequest)

    @POST("1.1/usersByMobilePhone")
    suspend fun loginByMobilePhone(@Body request: LeanCloudLoginRequest): CloudUser

    @POST("1.1/classes/Task")
    suspend fun createTask(
        @Header("X-LC-Session") sessionToken: String,
        @Body task: CloudTask
    ): CloudTask

    @PUT("1.1/classes/Task/{objectId}")
    suspend fun updateTask(
        @Path("objectId") objectId: String,
        @Header("X-LC-Session") sessionToken: String,
        @Body task: CloudTask
    ): CloudTask

    @GET("1.1/classes/Task")
    suspend fun queryTasks(
        @Header("X-LC-Session") sessionToken: String,
        @Query("where") where: String,
        @Query("limit") limit: Int = 1000,
        @Query("order") order: String = "-updatedAt"
    ): LeanCloudQueryResult

    @POST("1.1/batch")
    suspend fun batch(
        @Header("X-LC-Session") sessionToken: String,
        @Body requests: List<LeanCloudBatchRequestItem>
    ): List<LeanCloudBatchResult>
}

// ══════════════════════════════════════════════
//  LeanCloud 管理器
// ══════════════════════════════════════════════

object LeanCloudManager {

    private var isInitialized = false
    private var api: LeanCloudApi? = null
    private var appId: String = ""
    private var appKey: String = ""
    private var serverUrl: String = ""

    private val gson = Gson()

    // 加密存储 session 信息
    private var authPrefs: SharedPreferences? = null

    // 内存缓存
    private var cachedSessionToken: String? = null
    private var cachedUserId: String? = null
    private var cachedUserEmail: String? = null
    private var cachedUsername: String? = null

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "LeanCloud 未初始化：请配置 LEANCLOUD_APP_ID / LEANCLOUD_APP_KEY / LEANCLOUD_SERVER_URL"
            )
        }
    }

    /**
     * 初始化
     * 应在 Application.onCreate 中调用
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        appId = BuildConfig.LEANCLOUD_APP_ID
        appKey = BuildConfig.LEANCLOUD_APP_KEY
        serverUrl = BuildConfig.LEANCLOUD_SERVER_URL

        if (appId.isBlank() || appKey.isBlank() || serverUrl.isBlank()) {
            isInitialized = false
            return
        }

        // 初始化加密存储
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        authPrefs = EncryptedSharedPreferences.create(
            context,
            "lc_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // 恢复缓存的登录态
        cachedSessionToken = authPrefs?.getString("session_token", null)
        cachedUserId = authPrefs?.getString("user_id", null)
        cachedUserEmail = authPrefs?.getString("user_email", null)
        cachedUsername = authPrefs?.getString("user_name", null)

        // 构建 OkHttp + Retrofit
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("X-LC-Id", appId)
                    .header("X-LC-Key", appKey)
                    .header("Content-Type", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(LeanCloudApi::class.java)
        isInitialized = true
    }

    fun isInitialized(): Boolean = isInitialized

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return cachedSessionToken != null
    }

    /**
     * 获取当前用户的邮箱（供 Setting 页面显示）
     */
    fun getCurrentUserEmail(): String? {
        return cachedUserEmail
    }

    /**
     * 用户登录（邮箱/密码）
     */
    suspend fun loginWithEmail(email: String, password: String): CloudUser {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val request = LeanCloudLoginRequest(email = email, password = password)
            val user = api!!.loginWithUsername(request)
            saveSession(user)
            user
        }
    }

    /**
     * 用户登录（手机号/验证码）
     */
    suspend fun loginWithMobilePhone(phone: String, code: String): CloudUser {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val request = LeanCloudLoginRequest(mobilePhoneNumber = phone, smsCode = code)
            val user = api!!.loginByMobilePhone(request)
            saveSession(user)
            user
        }
    }

    /**
     * 用户注册
     */
    suspend fun signUp(username: String, email: String, password: String): CloudUser {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val request = LeanCloudSignUpRequest(
                username = username,
                password = password,
                email = email
            )
            val user = api!!.signUp(request)
            saveSession(user)
            user
        }
    }

    /**
     * 用户登出
     */
    fun logout() {
        cachedSessionToken = null
        cachedUserId = null
        cachedUserEmail = null
        cachedUsername = null
        authPrefs?.edit()?.clear()?.apply()
    }

    /**
     * 保存任务到 LeanCloud
     * 返回 objectId
     */
    suspend fun saveTask(task: Task): String {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val token = cachedSessionToken
                ?: throw IllegalStateException("未登录")

            val cloudTask = CloudTask(
                title = task.title,
                note = task.note,
                dueDate = task.dueDate?.toString(),
                startTime = task.startTime?.toString(),
                endTime = task.endTime?.toString(),
                priority = task.priority.name,
                isDone = task.isDone,
                tagLabels = task.tags.joinToString(",") { it.label },
                location = task.location,
                reminderOffset = task.reminderOffset,
                localId = task.id,
                syncStatus = task.syncStatus,
                deleted = task.deleted,
                userId = cachedUserId ?: "",
                clientUpdatedAt = task.updatedAt
            )

            if (!task.serverId.isNullOrEmpty()) {
                val result = api!!.updateTask(task.serverId, token, cloudTask)
                result.objectId.ifBlank { task.serverId }
            } else {
                val result = api!!.createTask(token, cloudTask)
                result.objectId
            }
        }
    }

    /**
     * 软删除任务
     */
    suspend fun softDeleteTask(serverId: String) {
        ensureInitialized()
        withContext(Dispatchers.IO) {
            val token = cachedSessionToken
                ?: throw IllegalStateException("未登录")
            api!!.updateTask(
                serverId, token,
                CloudTask(deleted = true, syncStatus = "synced")
            )
        }
    }

    suspend fun deleteTask(serverId: String) {
        softDeleteTask(serverId)
    }

    /**
     * 获取用户的所有任务
     */
    suspend fun fetchUserTasks(): List<CloudTask> {
        ensureInitialized()
        return withContext(Dispatchers.IO) {
            val token = cachedSessionToken
                ?: throw IllegalStateException("未登录")
            val userId = cachedUserId ?: throw IllegalStateException("未登录")

            val where = gson.toJson(mapOf(
                "userId" to userId,
                "deleted" to false
            ))
            val result = api!!.queryTasks(token, where)
            result.results
        }
    }

    /**
     * 将云端任务转换为本地 Task
     */
    fun convertToTask(cloudTask: CloudTask): Task {
        val tagList = if (cloudTask.tagLabels.isBlank()) emptyList()
        else cloudTask.tagLabels.split(",").mapNotNull { label ->
            PRESET_TAGS.find { it.label == label.trim() }
        }

        val serverUpdatedAt = try {
            parseLeanCloudDate(cloudTask.updatedAt)
        } catch (_: Exception) {
            0L
        }
        val clientUpdatedAt = cloudTask.clientUpdatedAt
        val mergedUpdatedAt = maxOf(serverUpdatedAt, clientUpdatedAt, System.currentTimeMillis())

        return Task(
            id = cloudTask.localId,
            title = cloudTask.title,
            note = cloudTask.note,
            dueDate = cloudTask.dueDate?.let {
                try { java.time.LocalDate.parse(it) } catch (_: Exception) { null }
            },
            startTime = cloudTask.startTime?.let {
                try { java.time.LocalTime.parse(it) } catch (_: Exception) { null }
            },
            endTime = cloudTask.endTime?.let {
                try { java.time.LocalTime.parse(it) } catch (_: Exception) { null }
            },
            priority = try {
                Priority.valueOf(cloudTask.priority)
            } catch (_: Exception) {
                Priority.MEDIUM
            },
            isDone = cloudTask.isDone,
            tags = tagList,
            location = cloudTask.location,
            reminderOffset = cloudTask.reminderOffset,
            serverId = cloudTask.objectId,
            syncStatus = cloudTask.syncStatus,
            updatedAt = mergedUpdatedAt,
            deleted = cloudTask.deleted
        )
    }

    /**
     * 发送短信验证码
     */
    suspend fun requestSMSCode(phone: String) {
        ensureInitialized()
        withContext(Dispatchers.IO) {
            api!!.requestLoginSmsCode(
                LeanCloudSmsCodeRequest(mobilePhoneNumber = phone)
            )
        }
    }

    // ══════════════════════════════════════════════
    //  内部工具
    // ══════════════════════════════════════════════

    private fun saveSession(user: CloudUser) {
        cachedSessionToken = user.sessionToken
        cachedUserId = user.objectId
        cachedUserEmail = user.email
        cachedUsername = user.username

        authPrefs?.edit()?.apply {
            putString("session_token", user.sessionToken)
            putString("user_id", user.objectId)
            putString("user_email", user.email)
            putString("user_name", user.username)
            apply()
        }
    }

    private fun parseLeanCloudDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val pattern = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        return try {
            java.time.Instant.from(pattern.parse(dateStr)).toEpochMilli()
        } catch (_: Exception) {
            try {
                dateStr.substring(0, 19).replace("T", " ")
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(dateStr.substring(0, 19))?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
