package com.example.demo002

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ═══════════════════════════════════════════════════════════════
//  Bmob REST API 接口定义
// ═══════════════════════════════════════════════════════════════

interface BmobApi {

    // ── 用户注册 ──
    @POST("/1/users")
    suspend fun register(@Body body: Map<String, String>): Response<BmobRegisterResponse>

    // ── 用户登录 ──
    @GET("/1/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<BmobLoginResponse>

    // ── 创建任务 ──
    @POST("/1/classes/Task")
    suspend fun createTask(@Body task: BmobTaskBody): Response<BmobCreateResponse>

    // ── 更新任务 ──
    @PUT("/1/classes/Task/{objectId}")
    suspend fun updateTask(
        @Path("objectId") objectId: String,
        @Body task: BmobTaskBody
    ): Response<BmobUpdateResponse>

    // ── 删除任务 ──
    @DELETE("/1/classes/Task/{objectId}")
    suspend fun deleteTask(@Path("objectId") objectId: String): Response<ResponseBody>

    // ── 查询任务列表 ──
    @GET("/1/classes/Task")
    suspend fun fetchTasks(
        @Query("order") order: String = "-updatedAt"
    ): Response<BmobTaskListResponse>

    // ── 查询单个任务 ──
    @GET("/1/classes/Task/{objectId}")
    suspend fun fetchTaskById(@Path("objectId") objectId: String): Response<BmobTaskItem>

    // ── 请求密码重置 ──
    @POST("/1/requestPasswordReset")
    suspend fun requestPasswordReset(@Body body: Map<String, String>): Response<ResponseBody>
}

// ═══════════════════════════════════════════════════════════════
//  Bmob 请求/响应 数据类
// ═══════════════════════════════════════════════════════════════

data class BmobRegisterResponse(
    @SerializedName("objectId") val objectId: String?,
    @SerializedName("sessionToken") val sessionToken: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class BmobLoginResponse(
    @SerializedName("objectId") val objectId: String?,
    @SerializedName("sessionToken") val sessionToken: String?,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("emailVerified") val emailVerified: Boolean? = null,
    @SerializedName("mobilePhoneNumber") val mobilePhoneNumber: String? = null,
    @SerializedName("mobilePhoneVerified") val mobilePhoneVerified: Boolean? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class BmobCreateResponse(
    @SerializedName("objectId") val objectId: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class BmobUpdateResponse(
    @SerializedName("updatedAt") val updatedAt: String?
)

data class BmobTaskBody(
    val title: String,
    val note: String?,
    val dueDate: String?,
    val startTime: String?,
    val endTime: String?,
    val priority: String?,
    val isDone: Boolean?,
    val tagLabels: String?,
    val location: String?,
    val reminderOffset: Int?,
    // 本地 ID 用于映射
    val localId: Int?
)

data class BmobTaskItem(
    @SerializedName("objectId") val objectId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("dueDate") val dueDate: String?,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("isDone") val isDone: Boolean?,
    @SerializedName("tagLabels") val tagLabels: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("reminderOffset") val reminderOffset: Int?,
    @SerializedName("localId") val localId: Int?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class BmobTaskListResponse(
    @SerializedName("results") val results: List<BmobTaskItem>?
)

data class BmobErrorResponse(
    val code: Int?,
    val error: String?
)

// ═══════════════════════════════════════════════════════════════
//  BmobManager 单例
// ═══════════════════════════════════════════════════════════════

object BmobManager {

    private const val BASE_URL = "https://api.bmobcloud.com/"
    private const val APP_ID = "c0208e29d17f82b5f7e20d6526dc047e"
    private const val REST_API_KEY = "9a98ac18e0b0878ce9d70a4df103862e"

    private const val PREF_NAME = "bmob_secure_prefs"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"

    private var prefs: SharedPreferences? = null
    private var _sessionToken: String? = null
    private var _userId: String? = null
    private var _userEmail: String? = null

    private val gson = Gson()

    // 信任所有证书的 TrustManager（Bmob 服务器 SSL 证书在部分网络上握手失败，此为临时兼容方案）
    private val trustAllCerts: Array<TrustManager> = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    )

    private val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, java.security.SecureRandom())
    }

    /**
     * 解析 Bmob REST API 返回的 JSON 错误体，提取可读的错误信息
     */
    private fun parseError(code: Int, errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return when (code) {
                400 -> "请求参数有误 (400)"
                401 -> "API Key 无效或未授权 (401)"
                403 -> "无权限访问该资源 (403)"
                404 -> "请求的资源不存在 (404)"
                500 -> "Bmob 服务器内部错误 (500)"
                else -> "HTTP $code"
            }
        }
        return try {
            val err = gson.fromJson(errorBody, BmobErrorResponse::class.java)
            buildString {
                append("Bmob错误")
                err.code?.let { append(" [$it]") }
                err.error?.let { append(": $it") }
            }
        } catch (_: Exception) {
            "HTTP $code: $errorBody"
        }
    }

    /** 网络异常转为中文可读信息 */
    private fun netError(e: Exception): String {
        val msg = e.message ?: "未知错误"
        return when {
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                "网络不通：无法解析 Bmob 服务器域名，请检查设备网络连接"
            msg.contains("connection closed", ignoreCase = true) ->
                "连接被关闭：API Key 无效或 Bmob 服务器拒绝了请求，请检查 APP_ID 和 REST_API_KEY"
            msg.contains("timeout", ignoreCase = true) ->
                "连接超时：Bmob 服务器无响应，请稍后重试"
            msg.contains("401") || msg.contains("Unauthorized") ->
                "认证失败：API Key 无效，请在 Bmob 控制台获取正确的 Key"
            else -> "网络请求失败: $msg"
        }
    }

    /** Bmob 101: 表尚未创建（新用户正常情况），应静默返回空数据 */
    private fun isBmobObjectNotFound(code: Int, errorBody: String?): Boolean {
        if (code != 400) return false
        if (errorBody.isNullOrBlank()) return false
        return try {
            val err = gson.fromJson(errorBody, BmobErrorResponse::class.java)
            err.code == 101
        } catch (_: Exception) {
            false
        }
    }

    // Retrofit 实例（懒加载）
    val api: BmobApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Bmob-Application-Id", APP_ID)
                .addHeader("X-Bmob-REST-API-Key", REST_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BmobApi::class.java)
    }

    /**
     * 初始化：必须在 Application 或 Activity 中调用一次
     */
    fun init(context: Context) {
        if (prefs == null) {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            prefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            _sessionToken = prefs?.getString(KEY_SESSION_TOKEN, null)
            _userId = prefs?.getString(KEY_USER_ID, null)
            _userEmail = prefs?.getString(KEY_USER_EMAIL, null)
        }
    }

    /**
     * 是否已登录（有有效的 sessionToken）
     */
    fun isLoggedIn(): Boolean = !_sessionToken.isNullOrBlank()

    /**
     * 获取当前 sessionToken
     */
    fun getSessionToken(): String? = _sessionToken

    /**
     * 获取当前用户 ID
     */
    fun getUserId(): String? = _userId

    /**
     * 获取当前用户邮箱
     */
    fun getUserEmail(): String? = _userEmail

    /**
     * 注册新用户（邮箱即账号）
     */
    suspend fun register(email: String, password: String): Result<BmobLoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf("username" to email, "password" to password, "email" to email)
                val response = api.register(body)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        body.sessionToken?.let { saveSession(it, body.objectId ?: "", email) }
                        Result.success(BmobLoginResponse(
                            objectId = body.objectId,
                            sessionToken = body.sessionToken,
                            username = email,
                            createdAt = body.createdAt
                        ))
                    } else {
                        Result.failure(Exception("注册返回为空"))
                    }
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception(parseError(code, errorBody)))
                }
            } catch (e: Exception) {
                Result.failure(Exception(netError(e)))
            }
        }

    /**
     * 用户登录
     */
    suspend fun login(username: String, password: String): Result<BmobLoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(username, password)
                if (response.isSuccessful) {
                    val respBody = response.body()
                    if (respBody != null) {
                        respBody.sessionToken?.let { saveSession(it, respBody.objectId ?: "", username) }
                        Result.success(respBody)
                    } else {
                        Result.failure(Exception("登录返回为空"))
                    }
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception(parseError(code, errorBody)))
                }
            } catch (e: Exception) {
                Result.failure(Exception(netError(e)))
            }
        }

    /**
     * 请求密码重置（发送重置邮件到指定邮箱）
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf("email" to email)
                val response = api.requestPasswordReset(body)
                if (response.isSuccessful) {
                    Log.i("MindHunter", "密码重置邮件发送成功: $email")
                    Result.success(Unit)
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string()
                    val msg = parseError(code, errorBody)
                    Log.e("MindHunter", "密码重置邮件发送失败 — HTTP $code, errorBody=$errorBody, parsed=$msg")
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Log.e("MindHunter", "密码重置邮件请求异常: ${e.message}", e)
                Result.failure(Exception(netError(e)))
            }
        }

    /**
     * 上传任务到云端
     */
    suspend fun uploadTask(task: TaskEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = BmobTaskBody(
                title = task.title,
                note = task.note.ifBlank { null },
                dueDate = task.dueDate,
                startTime = task.startTime,
                endTime = task.endTime,
                priority = task.priority,
                isDone = task.isDone,
                tagLabels = task.tagLabels.ifBlank { null },
                location = task.location.ifBlank { null },
                reminderOffset = task.reminderOffset,
                localId = if (task.id > 0) task.id else null
            )

            when (task.syncStatus) {
                1 -> { // 待新增
                    val resp = api.createTask(body)
                    if (!resp.isSuccessful) {
                        val code = resp.code()
                        val errorBody = resp.errorBody()?.string()
                        return@withContext Result.failure(Exception(parseError(code, errorBody)))
                    }
                }
                2 -> { // 待更新
                    val oid = task.objectId ?: return@withContext Result.failure(Exception("objectId 为空，无法更新"))
                    val resp = api.updateTask(oid, body)
                    if (!resp.isSuccessful) {
                        val code = resp.code()
                        val errorBody = resp.errorBody()?.string()
                        return@withContext Result.failure(Exception(parseError(code, errorBody)))
                    }
                }
                3 -> { // 待删除
                    val oid = task.objectId ?: return@withContext Result.failure(Exception("objectId 为空，无法删除"))
                    val resp = api.deleteTask(oid)
                    if (!resp.isSuccessful) {
                        val code = resp.code()
                        val errorBody = resp.errorBody()?.string()
                        return@withContext Result.failure(Exception(parseError(code, errorBody)))
                    }
                }
                else -> { /* 已同步，无需操作 */ }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(netError(e)))
        }
    }

    /**
     * 从云端拉取所有任务
     */
    suspend fun fetchTasks(): Result<List<BmobTaskItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.fetchTasks()
            if (response.isSuccessful) {
                val list = response.body()?.results ?: emptyList()
                Result.success(list)
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string()
                // Bmob 101: "object not found for Task." — 新用户还没有 Task 表，视为空数据
                if (isBmobObjectNotFound(code, errorBody)) {
                    Result.success(emptyList())
                } else {
                    Result.failure(Exception(parseError(code, errorBody)))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(netError(e)))
        }
    }

    /**
     * 清除登录状态（退出登录）
     */
    fun logout() {
        _sessionToken = null
        _userId = null
        _userEmail = null
        prefs?.edit()?.clear()?.apply()
    }

    // ── 内部方法 ──

    private fun saveSession(sessionToken: String, userId: String, email: String) {
        _sessionToken = sessionToken
        _userId = userId
        _userEmail = email
        prefs?.edit()?.putString(KEY_SESSION_TOKEN, sessionToken)?.apply()
        prefs?.edit()?.putString(KEY_USER_ID, userId)?.apply()
        prefs?.edit()?.putString(KEY_USER_EMAIL, email)?.apply()
    }
}
