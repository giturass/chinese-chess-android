package com.ericlee.chess.network

import com.google.gson.Gson
import com.ericlee.chess.model.Side
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OnlineGameClient(
    serverUrl: String,
    private val gson: Gson = Gson()
) {
    private val baseUrl = serverUrl.trim().trimEnd('/')

    fun join(roomId: String, playerId: String? = null, preferredSide: Side? = null): OnlineSnapshot {
        val body = OnlineJoinRequest(
            playerId = playerId.takeUnless { it.isNullOrBlank() },
            preferredSide = preferredSide
        )
        return request(
            path = "/api/rooms/${roomId.pathPart()}/join",
            method = "POST",
            body = body,
            responseClass = OnlineSnapshot::class.java
        )
    }

    fun snapshot(
        roomId: String,
        playerId: String,
        sinceRevision: Long? = null,
        waitMs: Int = 0
    ): OnlineSnapshot {
        val waitQuery = if (sinceRevision != null && waitMs > 0) {
            "&since=$sinceRevision&wait=$waitMs"
        } else {
            ""
        }
        return request(
            path = "/api/rooms/${roomId.pathPart()}?playerId=${playerId.queryPart()}$waitQuery",
            method = "GET",
            body = null,
            responseClass = OnlineSnapshot::class.java,
            readTimeoutMs = if (waitMs > 0) waitMs + 5000 else DEFAULT_READ_TIMEOUT_MS
        )
    }

    fun sendMove(roomId: String, playerId: String, move: OnlineMoveDto): OnlineSnapshot =
        request(
            path = "/api/rooms/${roomId.pathPart()}/move",
            method = "POST",
            body = OnlineMoveRequest(playerId = playerId, move = move),
            responseClass = OnlineSnapshot::class.java
        )

    fun sendAction(roomId: String, playerId: String, action: String): OnlineSnapshot =
        request(
            path = "/api/rooms/${roomId.pathPart()}/action",
            method = "POST",
            body = OnlineActionRequest(playerId = playerId, action = action),
            responseClass = OnlineSnapshot::class.java
        )

    fun leave(roomId: String, playerId: String) {
        request(
            path = "/api/rooms/${roomId.pathPart()}/leave",
            method = "POST",
            body = OnlineLeaveRequest(playerId = playerId),
            responseClass = LeaveResponse::class.java
        )
    }

    private fun <T> request(
        path: String,
        method: String,
        body: Any?,
        responseClass: Class<T>,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
    ): T {
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
            "服务端地址无效"
        }

        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    gson.toJson(body, writer)
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException(parseError(text).ifBlank { "服务端返回 $code" })
            }

            return gson.fromJson(text, responseClass)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseError(text: String): String {
        return runCatching {
            gson.fromJson(text, ErrorResponse::class.java).error
        }.getOrNull().orEmpty()
    }

    private fun String.pathPart(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    private fun String.queryPart(): String = URLEncoder.encode(this, "UTF-8")

    private data class ErrorResponse(val error: String = "")

    private data class LeaveResponse(val ok: Boolean = false)

    private companion object {
        const val DEFAULT_READ_TIMEOUT_MS = 8000
    }
}
