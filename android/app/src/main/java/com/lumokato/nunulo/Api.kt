package com.lumokato.nunulo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class ApiException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)

internal class JsonHttpClient(private val client: OkHttpClient = defaultNunuloHttpClient()) {
    fun authorizedBuilder(apiBase: String, path: String, token: String): Request.Builder = Request.Builder()
        .url(apiUrl(apiBase, path))
        .header("Authorization", "Bearer $token")

    fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
                throw ApiException(response.code, detail?.ifBlank { null } ?: "HTTP ${response.code}")
            }
            return JSONObject(text)
        }
    }
}

internal class TokenRefreshCoordinator {
    private val mutex = Mutex()

    suspend fun <T> run(
        currentAccessToken: () -> String,
        currentRefreshToken: () -> String,
        persistAccessToken: (String) -> Unit,
        refreshAccessToken: suspend (String) -> String,
        block: suspend (String) -> T,
    ): T {
        val attemptedToken = currentAccessToken().takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("请先登录")
        return try {
            block(attemptedToken)
        } catch (firstError: Exception) {
            if (!looksLikeExpiredToken(firstError)) throw firstError
            val retryToken = mutex.withLock {
                val latestToken = currentAccessToken()
                if (latestToken.isNotBlank() && latestToken != attemptedToken) {
                    latestToken
                } else {
                    val refreshToken = currentRefreshToken().takeIf { it.isNotBlank() } ?: throw firstError
                    refreshAccessToken(refreshToken).also(persistAccessToken)
                }
            }
            block(retryToken)
        }
    }
}

internal fun apiUrl(apiBase: String, path: String): String = apiBase.trim().trimEnd('/') + path

internal fun looksLikeExpiredToken(error: Throwable): Boolean =
    error is ApiException && error.statusCode == 401

internal fun defaultNunuloHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(90, TimeUnit.SECONDS)
    .build()
