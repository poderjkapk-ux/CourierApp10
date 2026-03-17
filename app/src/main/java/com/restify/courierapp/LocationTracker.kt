package com.restify.courierapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationTracker : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // ID для нашого сповіщення
    private val NOTIFICATION_CHANNEL_ID = "courier_location_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // Що робити, коли телефон отримує нові координати від супутника
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToServer(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // Дозволи ми будемо запитувати в MainActivity
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Запускаємо службу на передньому плані (з постійним сповіщенням)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // Налаштовуємо частоту оновлення GPS (кожні 10-15 секунд)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setMinUpdateIntervalMillis(10000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // START_STICKY означає, що якщо Android випадково вб'є службу через нестачу пам'яті, він спробує її перезапустити
        return START_STICKY
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        // --- ЗАХИСТ ВІД РЕБ (GPS SPOOFING) ---
        // Одеська область приблизно в межах Lat 45.0 - 48.0 та Lon 29.0 - 32.0
        val isRealLocation = (lat > 45.0 && lat < 48.0) && (lon > 29.0 && lon < 32.0)

        if (!isRealLocation) {
            Log.w("LocationTracker", "РЕБ DETECTED! Фейкова локація проігнорована: $lat, $lon")
            return // Перериваємо відправку, чекаємо наступного оновлення GPS
        }
        // -------------------------------------

        val sharedPref = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        val cookie = sharedPref.getString("cookie", null)

        if (cookie != null) {
            // 1. ШВИДКА ВІДПРАВКА: через WebSocket (для миттєвого відображення, якщо WS підключений)
            try {
                RetrofitClient.webSocketManager.sendLocation(lat, lon)
                Log.d("LocationTracker", "Відправлено GPS через WS: $lat, $lon")
            } catch (e: Exception) {
                Log.e("LocationTracker", "Помилка відправки GPS через WS: ${e.message}")
            }

            // 2. НАДІЙНА ВІДПРАВКА: через звичайний REST API POST-запит
            // Запускаємо в окремому фоновому потоці (IO Dispatcher), щоб не блокувати GPS-сервіс
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.sendLocation(cookie, lat, lon)
                    Log.d("LocationTracker", "Відправлено GPS через REST API: $lat, $lon (Status: ${response.status})")
                } catch (e: Exception) {
                    Log.e("LocationTracker", "Помилка REST API: ${e.message}")
                }
            }
        } else {
            Log.e("LocationTracker", "Помилка: Немає збереженого токену (Cookie)")
        }
    }

    // Створюємо канал сповіщень (обов'язково для Android 8.0 і вище)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Геолокація кур'єра",
                NotificationManager.IMPORTANCE_LOW // Низька важливість, щоб не пікало постійно
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Створюємо саме сповіщення, яке буде "висіти" в шторці
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Restify Кур'єр")
            .setContentText("Відстеження геолокації активно")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Стандартна іконка прицілу
            .setOngoing(true) // Користувач не може змахнути це сповіщення
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}