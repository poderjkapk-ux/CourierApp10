package com.restify.courierapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Вызывается, когда Firebase выдает или обновляет токен устройства
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Новый токен: $token")
        sendTokenToServer(token)
    }

    // Вызывается при получении уведомления
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Твой бэкенд (FastAPI) отправляет пуши в блоке "data",
        // поэтому мы берем "title" и "body" именно оттуда!
        val title = remoteMessage.data["title"] ?: "Нове сповіщення"
        val body = remoteMessage.data["body"] ?: ""

        Log.d("FCM", "Отримано пуш: Title=$title, Body=$body")

        showNotification(title, body)
    }

    private fun sendTokenToServer(token: String) {
        val sharedPref = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        val cookie = sharedPref.getString("cookie", null)

        if (cookie != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.apiService.sendFcmToken(cookie, token)
                    Log.d("FCM", "Токен успішно відправлено на сервер")
                } catch (e: Exception) {
                    Log.e("FCM", "Помилка відправки токена: ${e.message}")
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "courier_push_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Для Android 8.0+ обязательно нужен канал уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Сповіщення для кур'єрів",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}