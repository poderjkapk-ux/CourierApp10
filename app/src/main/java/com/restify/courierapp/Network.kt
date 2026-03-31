package com.restify.courierapp

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ==========================================
// 1. МОДЕЛІ ДАНИХ (Data Classes)
// ==========================================

// Безпечний пустий клас для POST-запитів, який не видаляється системою стиснення
class EmptyRequest

data class Announcement(
    val id: Int,
    val title: String,
    val message: String,
    val style: String
)

data class OpenOrder(
    val id: Int,
    @SerializedName("restaurant_name") val restaurantName: String,
    @SerializedName("restaurant_address") val restaurantAddress: String,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    val fee: Double,
    val price: Double,
    @SerializedName("dist_to_rest") val distToRest: Double?,
    @SerializedName("dist_trip") val distTrip: String?,
    @SerializedName("payment_type") val paymentType: String,
    @SerializedName("is_return") val isReturn: Boolean,
    val comment: String?,
    @SerializedName("estimated_ready_at") val readyAt: String? // ДОДАНО ДЛЯ ТАЙМЕРІВ
)

data class ActiveJobResponse(
    val active: Boolean,
    val job: ActiveJobDetail?
)

data class ActiveJobDetail(
    val id: Int,
    val status: String,
    @SerializedName("server_status") val serverStatus: String,
    @SerializedName("is_ready") val isReady: Boolean,
    @SerializedName("estimated_ready_at") val readyAt: String?, // ДОДАНО ДЛЯ ТАЙМЕРІВ

    @SerializedName("assigned_at") val assignedAt: String?,
    @SerializedName("picked_up_at") val pickedUpAt: String?,
    @SerializedName("delivered_at") val deliveredAt: String?,
    @SerializedName("completed_at") val completedAt: String?,

    @SerializedName("partner_name") val partnerName: String,
    @SerializedName("partner_address") val partnerAddress: String,
    @SerializedName("partner_phone") val partnerPhone: String?,
    @SerializedName("customer_address") val customerAddress: String,
    @SerializedName("customer_lat") val customerLat: Double?,
    @SerializedName("customer_lon") val customerLon: Double?,
    @SerializedName("customer_phone") val customerPhone: String,
    @SerializedName("customer_name") val customerName: String?,
    val comment: String?,
    @SerializedName("order_price") val orderPrice: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double,
    @SerializedName("payment_type") val paymentType: String,
    @SerializedName("is_return_required") val isReturnRequired: Boolean
)

data class StatusResponse(
    val status: String,
    val message: String? = null
)

data class ToggleResponse(
    @SerializedName("is_online") val isOnline: Boolean
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("text") val text: String,
    @SerializedName("time") val time: String
)

data class SendMessageResponse(
    val status: String
)

data class HistoryOrder(
    val id: Int,
    val date: String,
    val address: String,
    val price: Double,
    val status: String,
    val commission: Double? = 0.0 // <- ДОДАНО ПОЛЕ КОМІСІЇ
)

data class CourierProfile(
    val id: Int,
    val name: String,
    val phone: String,
    val balance: Double?,
    @SerializedName("commission_rate") val commissionRate: Double?,
    val rating: Double?,
    @SerializedName("rating_count") val ratingCount: Int?,
    @SerializedName("is_online") val isOnline: Boolean
)

data class VerificationInitResponse(
    @SerializedName("token") val token: String,
    @SerializedName("link") val link: String
)

data class VerificationCheckResponse(
    @SerializedName("status") val status: String,
    @SerializedName("phone") val phone: String?
)

data class RegisterResponse(
    val status: String,
    val detail: String?
)

data class AppUpdateResponse(
    val success: Boolean,
    val app: String,
    @SerializedName("latest_version_code") val latestVersionCode: Int,
    @SerializedName("latest_version_name") val latestVersionName: String,
    @SerializedName("download_url") val downloadUrl: String
)

// ==========================================
// 2. ІНТЕРФЕЙС API (Retrofit)
// ==========================================

interface ApiService {

    @FormUrlEncoded
    @POST("/api/courier/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): retrofit2.Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/courier/reset_password")
    suspend fun resetCourierPassword(
        @Field("phone") phone: String
    ): retrofit2.Response<StatusResponse>

    @GET("/api/courier/open_orders")
    suspend fun getOpenOrders(
        @Header("Cookie") cookie: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): List<OpenOrder>

    @GET("/api/courier/active_job")
    suspend fun getActiveJob(
        @Header("Cookie") cookie: String
    ): ActiveJobResponse

    @FormUrlEncoded
    @POST("/api/courier/accept_order")
    suspend fun acceptOrder(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): retrofit2.Response<StatusResponse>

    @FormUrlEncoded
    @POST("/api/courier/arrived_pickup")
    suspend fun arrivedAtPickup(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): StatusResponse

    @FormUrlEncoded
    @POST("/api/courier/update_job_status")
    suspend fun updateJobStatus(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("status") status: String
    ): StatusResponse

    @FormUrlEncoded
    @POST("/api/courier/location")
    suspend fun sendLocation(
        @Header("Cookie") cookie: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): StatusResponse

    @FormUrlEncoded
    @POST("/api/courier/fcm_token")
    suspend fun sendFcmToken(
        @Header("Cookie") cookie: String,
        @Field("token") token: String
    ): StatusResponse

    @POST("/api/courier/toggle_status")
    suspend fun toggleStatus(
        @Header("Cookie") cookie: String,
        @Body empty: EmptyRequest
    ): ToggleResponse

    @GET("/api/chat/history/{job_id}")
    suspend fun getChatMessages(
        @Header("Cookie") cookie: String,
        @Path("job_id") jobId: Int
    ): List<ChatMessage>

    @FormUrlEncoded
    @POST("/api/chat/send")
    suspend fun sendChatMessage(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("message") message: String,
        @Field("role") role: String = "courier"
    ): SendMessageResponse

    @GET("/api/courier/history")
    suspend fun getHistory(
        @Header("Cookie") cookie: String
    ): List<HistoryOrder>

    @GET("/api/courier/profile")
    suspend fun getProfile(
        @Header("Cookie") cookie: String
    ): CourierProfile

    @POST("/api/auth/init_verification")
    suspend fun initVerification(
        @Body empty: EmptyRequest
    ): retrofit2.Response<VerificationInitResponse>

    @GET("/api/auth/check_verification/{token}")
    suspend fun checkVerification(
        @Path("token") token: String
    ): retrofit2.Response<VerificationCheckResponse>

    @Multipart
    @POST("/api/courier/register")
    suspend fun registerCourier(
        @Part("name") name: RequestBody,
        @Part("password") password: RequestBody,
        @Part("verification_token") verificationToken: RequestBody,
        @Part documentPhoto: MultipartBody.Part,
        @Part selfiePhoto: MultipartBody.Part
    ): retrofit2.Response<RegisterResponse>

    @GET("/api/check-update/courier")
    suspend fun checkUpdate(): retrofit2.Response<AppUpdateResponse>

    // --- СИСТЕМА ОГОЛОШЕНЬ ---
    @GET("/api/courier/announcements")
    suspend fun getAnnouncements(
        @Header("Cookie") cookie: String
    ): List<Announcement>

    @POST("/api/courier/announcements/{ann_id}/dismiss")
    suspend fun dismissAnnouncement(
        @Header("Cookie") cookie: String,
        @Path("ann_id") annId: Int
    ): StatusResponse
    // -------------------------
}

// ==========================================
// 3. МЕНЕДЖЕР WEBSOCKET З АВТО-РЕКОНЕКТОМ ТА ПІНГОМ
// ==========================================

class WebSocketManager(private val client: OkHttpClient) {
    private var webSocket: WebSocket? = null

    // Flow для прослуховування вхідних повідомлень у UI
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages = _messages.asSharedFlow()

    private var currentCookie: String? = null
    private var isIntentionallyClosed = false

    // Корутина для фонового пінгу
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null

    fun connect(cookie: String) {
        currentCookie = cookie
        isIntentionallyClosed = false
        startConnection()
    }

    private fun startConnection() {
        if (webSocket != null) return
        val cookie = currentCookie ?: return

        val request = Request.Builder()
            .url("wss://restify.site/ws/courier")
            .addHeader("Cookie", cookie)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                startPingJob()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message received: $text")
                if (text != "pong") {
                    _messages.tryEmit(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason (code: $code)")
                this@WebSocketManager.webSocket = null
                stopPingJob()

                // Якщо закрили не ми і код НЕ пов'язаний з помилкою авторизації (1008) або нормальним закриттям (1000)
                if (!isIntentionallyClosed && code != 1008 && code != 1000) {
                    scheduleReconnect()
                } else if (code == 1008) {
                    // Сервер примусово розірвав з'єднання через права доступу (Policy Violation)
                    _messages.tryEmit("{\"type\": \"auth_error\"}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error", t)
                this@WebSocketManager.webSocket = null
                stopPingJob()

                if (!isIntentionallyClosed) {
                    // ТУТ ИСПРАВЛЕНИЕ: добавлены скобки code()
                    val httpCode = response?.code() ?: 0

                    // Перевіряємо, чи не викликана помилка простроченою сесією (401 або 403)
                    if (httpCode == 401 || httpCode == 403) {
                        Log.e("WebSocket", "Auth failed ($httpCode). Stopping reconnect spam.")
                        // Відправляємо фейкове повідомлення в UI для негайного логауту
                        _messages.tryEmit("{\"type\": \"auth_error\"}")
                    } else {
                        // Якщо це просто обрив інтернету – пробуємо перепідключитись
                        scheduleReconnect()
                    }
                }
            }
        })
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(15000) // Шлемо пінг кожні 15 секунд
                sendPing()
            }
        }
    }

    private fun stopPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000) // Чекаємо 5 секунд перед спробою реконекту
            if (!isIntentionallyClosed && webSocket == null) {
                Log.d("WebSocket", "Attempting to reconnect...")
                startConnection()
            }
        }
    }

    fun sendLocation(lat: Double, lon: Double) {
        val json = JSONObject().apply {
            put("type", "init_location")
            put("lat", lat)
            put("lon", lon)
        }
        webSocket?.send(json.toString())
    }

    fun sendPing() {
        // Відправляємо просту строку "ping", як це очікує сервер
        webSocket?.send("ping")
    }

    fun disconnect() {
        isIntentionallyClosed = true
        stopPingJob()
        webSocket?.close(1000, "App closed/Logout")
        webSocket = null
        currentCookie = null
    }
}

// ==========================================
// 4. КЛІЄНТ RETROFIT (Singleton)
// ==========================================

object RetrofitClient {
    private const val BASE_URL = "https://restify.site/"

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val webSocketManager = WebSocketManager(okHttpClient)
}