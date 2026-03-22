package com.restify.courierapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.osmdroid.config.Configuration
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Функція для запуску служби геолокації
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTracker::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // Допоміжна функція для отримання координат (безпечно для корутин)
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // ЗАХИСТ ВІД КРАШУ: Перевіряємо, чи корутина ще активна перед тим як повернути результат
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація OpenStreetMap (потрібно для відображення карти)
        Configuration.getInstance().userAgentValue = packageName

        // Ініціалізація клієнта геолокації
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sharedPref = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val coroutineScope = rememberCoroutineScope()
                    val savedCookie = sharedPref.getString("cookie", null)

                    // ГЛОБАЛЬНА ЗМІННА СТАТУСУ: Тепер статус не втрачається при переходах!
                    var isOnline by rememberSaveable { mutableStateOf(false) }

                    // --- Глобальна функція для примусового логауту або виходу з акаунту ---
                    fun forceLogout(isExplicitLogout: Boolean = false) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val currentCookie = sharedPref.getString("cookie", null)

                            // Якщо курьєр натиснув "Вийти" і він зараз онлайн — перемикаємо статус на сервері
                            if (isExplicitLogout && currentCookie != null && isOnline) {
                                try {
                                    RetrofitClient.apiService.toggleStatus(currentCookie, EmptyRequest())
                                } catch (e: Exception) {
                                    Log.e("Logout", "Не вдалося переключити статус на бекенді")
                                }
                            }

                            // Повертаємось у головний потік для оновлення UI
                            launch(Dispatchers.Main) {
                                isOnline = false // Обов'язково скидаємо локальний стан!
                                sharedPref.edit().remove("cookie").apply()
                                RetrofitClient.webSocketManager.disconnect()
                                stopService(Intent(this@MainActivity, LocationTracker::class.java))

                                // Показуємо тост тільки якщо це викид (наприклад, 401), а не добровільний вихід
                                if (!isExplicitLogout) {
                                    Toast.makeText(this@MainActivity, "Сесія закінчилась, увійдіть знову", Toast.LENGTH_LONG).show()
                                }

                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    // --- Управління життєвим циклом WebSocket та FCM-токеном ---
                    LaunchedEffect(Unit) {
                        if (savedCookie != null) {
                            // Підключаємо WebSocket
                            RetrofitClient.webSocketManager.connect(savedCookie)

                            // Відправляємо FCM-токен на сервер при кожному старті додатку
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.sendFcmToken(savedCookie, token)
                                            Log.d("FCM_TOKEN", "Токен успішно оновлено при старті: $token")
                                        } catch (e: Exception) {
                                            Log.e("FCM_TOKEN", "Помилка відправки токена при старті: ${e.message}")
                                        }
                                    }
                                } else {
                                    Log.e("FCM_TOKEN", "Не вдалося отримати токен від Firebase", task.exception)
                                }
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            RetrofitClient.webSocketManager.disconnect()
                        }
                    }
                    // ---------------------------------------------------

                    // БЛОК ЗАПИТУ ДОЗВОЛІВ (GPS та Сповіщення)
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

                    // Перевіряємо чи є хоча б якийсь дозвіл на локацію (щоб відмова від пушів не блокувала роботу)
                    val hasLocationPermission = permissionsState.permissions.any {
                        (it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                                it.permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                                it.status.isGranted
                    }

                    var showLocationDisclosure by rememberSaveable { mutableStateOf(false) }

                    // --- Глобальний контроль GPS: автоматично запускаємо/зупиняємо сервіс ---
                    LaunchedEffect(permissionsState.allPermissionsGranted, isOnline, hasLocationPermission) {
                        if (!permissionsState.allPermissionsGranted) {
                            val hasSeenDisclosure = sharedPref.getBoolean("has_seen_location_disclosure", false)
                            if (!hasSeenDisclosure) {
                                // Показуємо наше власне пояснення перед системним запитом
                                showLocationDisclosure = true
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        }

                        // Запускаємо сервіс, якщо є дозвіл САМЕ на локацію, незалежно від дозволу на сповіщення
                        if (hasLocationPermission) {
                            if (isOnline) {
                                startLocationService()
                            } else {
                                stopService(Intent(this@MainActivity, LocationTracker::class.java))
                            }
                        } else if (!isOnline) {
                            stopService(Intent(this@MainActivity, LocationTracker::class.java))
                        }
                    }

                    // Діалогове вікно (Prominent Disclosure) для Google Play
                    if (showLocationDisclosure) {
                        AlertDialog(
                            onDismissRequest = { /* Не закриваємо по кліку поза вікном */ },
                            title = { Text(text = "Доступ до геолокації") },
                            text = {
                                Text(text = "Додаток CourierApp збирає дані про ваше місцезнаходження у фоновому режимі.\n\nЦе необхідно для того, щоб розраховувати відстань до клієнта та інформувати заклади про ваше наближення, навіть коли додаток згорнуто або не використовується.")
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    sharedPref.edit().putBoolean("has_seen_location_disclosure", true).apply()
                                    showLocationDisclosure = false
                                    permissionsState.launchMultiplePermissionRequest()
                                }) {
                                    Text("Погоджуюсь")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showLocationDisclosure = false
                                }) {
                                    Text("Відхилити")
                                }
                            }
                        )
                    }

                    // Визначаємо стартовий екран з урахуванням онбордингу
                    val startDestination = if (isFirstLaunch()) {
                        "onboarding"
                    } else if (savedCookie != null) {
                        "orders"
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {

                        // РОУТ 0: ОНБОРДИНГ
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinish = {
                                    setFirstLaunchCompleted()
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // РОУТ 1: ЛОГІН
                        composable("login") {
                            var isLoading by remember { mutableStateOf(false) }
                            var errorMessage by remember { mutableStateOf<String?>(null) }

                            LoginScreen(
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onLoginClick = { phone, password ->
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.login(phone, password)
                                            if (response.isSuccessful || response.code() == 302 || response.code() == 303) {
                                                val tokenCookie = response.headers().values("Set-Cookie").firstOrNull { it.contains("courier_token") }

                                                if (tokenCookie != null) {
                                                    val cookieValue = tokenCookie.split(";")[0]
                                                    sharedPref.edit().putString("cookie", cookieValue).apply()

                                                    // Підключаємо WebSocket одразу після успішного входу
                                                    RetrofitClient.webSocketManager.connect(cookieValue)

                                                    // Відправляємо FCM токен після успішного логіну
                                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val token = task.result
                                                            coroutineScope.launch {
                                                                try {
                                                                    RetrofitClient.apiService.sendFcmToken(cookieValue, token)
                                                                    Log.d("FCM_TOKEN", "Токен успішно відправлено після логіну: $token")
                                                                } catch (e: Exception) {
                                                                    Log.e("FCM_TOKEN", "Помилка відправки токена після логіну: ${e.message}")
                                                                }
                                                            }
                                                        }
                                                    }

                                                    navController.navigate("orders") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                } else {
                                                    errorMessage = "Помилка: Немає токена"
                                                }
                                            } else {
                                                errorMessage = "Невірний телефон або пароль"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Помилка мережі"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }

                        // РОУТ 1.5: РЕЄСТРАЦІЯ
                        composable("register") {
                            RegistrationScreen(
                                onRegisterSuccess = {
                                    Toast.makeText(this@MainActivity, "Реєстрація успішна! Очікуйте активації акаунта адміністратором.", Toast.LENGTH_LONG).show()
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // РОУТ 2: СПИСОК ЗАМОВЛЕНЬ
                        composable("orders") {
                            var ordersList by remember { mutableStateOf<List<OpenOrder>>(emptyList()) }
                            var announcementsList by remember { mutableStateOf<List<Announcement>>(emptyList()) }
                            var isLoading by remember { mutableStateOf(true) }

                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            // --- СТАН ДЛЯ ВІДСТЕЖЕННЯ УВІМКНЕНОГО GPS ---
                            val context = LocalContext.current
                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            var isGpsEnabled by remember { mutableStateOf(true) }

                            // параметри для "тихого" оновлення
                            fun fetchData(isSilent: Boolean = false) {
                                if (!isSilent) isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val activeJobRes = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (activeJobRes.active) {
                                            navController.navigate("active_order") {
                                                popUpTo("orders") { inclusive = true }
                                            }
                                            return@launch
                                        }

                                        // Завантажуємо оголошення
                                        try {
                                            announcementsList = RetrofitClient.apiService.getAnnouncements(currentCookie)
                                        } catch (e: Exception) {
                                            Log.e("Announcements", "Failed to load announcements: ${e.message}")
                                        }

                                        // Отримуємо реальні координати
                                        var currentLat = 46.4825
                                        var currentLon = 30.7233

                                        if (hasLocationPermission) {
                                            val location = getLastKnownLocation()
                                            if (location != null) {
                                                // --- ЗАХИСТ ВІД РЕБ (GPS SPOOFING) ---
                                                if (location.latitude > 45.0 && location.latitude < 48.0 && location.longitude > 29.0 && location.longitude < 32.0) {
                                                    currentLat = location.latitude
                                                    currentLon = location.longitude
                                                } else {
                                                    Log.w("GPS_FILTER", "РЕБ або збій! Фейкова локація проігнорована: ${location.latitude}, ${location.longitude}")
                                                }
                                            }
                                        }

                                        ordersList = RetrofitClient.apiService.getOpenOrders(
                                            currentCookie,
                                            lat = currentLat,
                                            lon = currentLon
                                        )
                                    } catch (e: retrofit2.HttpException) {
                                        if (e.code() == 401 || e.code() == 403) {
                                            forceLogout()
                                        } else if (!isSilent) {
                                            Toast.makeText(this@MainActivity, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        if (!isSilent) Toast.makeText(this@MainActivity, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        if (!isSilent) isLoading = false
                                    }
                                }
                            }

                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        isGpsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            locationManager.isLocationEnabled
                                        } else {
                                            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                                        }
                                        fetchData(isSilent = true)
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }

                            // При відкритті екрану завантажуємо реальний профіль і статус
                            LaunchedEffect(Unit) {
                                coroutineScope.launch {
                                    try {
                                        val profile = RetrofitClient.apiService.getProfile(currentCookie)
                                        isOnline = profile.isOnline // Це автоматично запустить/зупинить LocationTracker
                                    } catch (e: retrofit2.HttpException) {
                                        if (e.code() == 401 || e.code() == 403) forceLogout()
                                    } catch (e: Exception) {
                                        Log.e("SYNC", "Не вдалося отримати профіль для перевірки статусу")
                                    }
                                }
                                fetchData(isSilent = false)
                            }

                            // Фонове тихе оновлення
                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(30000)
                                    fetchData(isSilent = true)
                                }
                            }

                            // Слухаємо WebSocket події для миттєвого оновлення списку або логауту
                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")

                                        if (type == "auth_error") {
                                            forceLogout() // <--- Обробка помилки 401/403 від WebSocket
                                        } else if (type == "new_order" || type == "job_update") {
                                            fetchData(isSilent = true) // Оновлюємо список тихо
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            OrdersListScreen(
                                orders = ordersList,
                                announcements = announcementsList,
                                isLoading = isLoading,
                                isOnline = isOnline,
                                isGpsEnabled = isGpsEnabled, // ПЕРЕДАЄМО СТАТУС GPS НА ЕКРАН
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile") // Перехід на екран профілю
                                },
                                onDismissAnnouncement = { annId ->
                                    announcementsList = announcementsList.filter { it.id != annId }
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.dismissAnnouncement(currentCookie, annId)
                                        } catch (e: Exception) {
                                            Log.e("Announcements", "Failed to dismiss: ${e.message}")
                                        }
                                    }
                                },
                                onToggleStatus = { _ ->
                                    coroutineScope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.toggleStatus(currentCookie, EmptyRequest())
                                            isOnline = response.isOnline
                                        } catch (e: retrofit2.HttpException) {
                                            if (e.code() == 401 || e.code() == 403) {
                                                forceLogout()
                                            } else {
                                                Toast.makeText(this@MainActivity, "Помилка зв'язку з сервером", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Помилка зв'язку з сервером", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onRefresh = { fetchData(isSilent = false) },
                                onAcceptOrder = { jobId, onComplete ->
                                    coroutineScope.launch {
                                        try {
                                            val res = RetrofitClient.apiService.acceptOrder(currentCookie, jobId)
                                            if (res.isSuccessful) {
                                                fetchData(isSilent = false)
                                            } else {
                                                Toast.makeText(this@MainActivity, "Замовлення вже забрали", Toast.LENGTH_LONG).show()
                                                fetchData(isSilent = false)
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Помилка зв'язку з сервером", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            onComplete()
                                        }
                                    }
                                }
                            )
                        }

                        // РОУТ 3: АКТИВНЕ ЗАМОВЛЕННЯ
                        composable("active_order") {
                            var activeJob by remember { mutableStateOf<ActiveJobDetail?>(null) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchActiveJob() {
                                coroutineScope.launch {
                                    try {
                                        val res = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (res.active && res.job != null) activeJob = res.job
                                        else navController.navigate("orders") { popUpTo("active_order") { inclusive = true } }
                                    } catch (e: retrofit2.HttpException) {
                                        if (e.code() == 401 || e.code() == 403) forceLogout()
                                    } catch (e: Exception) {}
                                }
                            }

                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        fetchActiveJob()
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }

                            LaunchedEffect(Unit) { fetchActiveJob() }

                            // Слухаємо WebSocket події для миттєвого оновлення замовлення або логауту
                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")

                                        if (type == "auth_error") {
                                            forceLogout() // <--- Обробка помилки 401/403 від WebSocket
                                        } else if (type == "job_update" || type == "job_ready") {
                                            fetchActiveJob()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            activeJob?.let { job ->
                                ActiveOrderScreen(
                                    job = job,
                                    cookie = currentCookie,
                                    onRefresh = { fetchActiveJob() },
                                    onArrivedPickup = { jobId ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.arrivedAtPickup(currentCookie, jobId); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    },
                                    onUpdateStatus = { jobId, status ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.updateJobStatus(currentCookie, jobId, status); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    }
                                )
                            }
                        }

                        // РОУТ 4: ІСТОРІЯ ЗАМОВЛЕНЬ
                        composable("history") {
                            var historyList by remember { mutableStateOf<List<HistoryOrder>>(emptyList()) }
                            var isLoading by remember { mutableStateOf(true) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchHistory() {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        historyList = RetrofitClient.apiService.getHistory(currentCookie)
                                    } catch (e: retrofit2.HttpException) {
                                        if (e.code() == 401 || e.code() == 403) forceLogout()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Помилка завантаження історії", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }

                            LaunchedEffect(Unit) { fetchHistory() }

                            HistoryScreen(
                                history = historyList,
                                isLoading = isLoading,
                                onBack = { navController.popBackStack() },
                                onRefresh = { fetchHistory() }
                            )
                        }

                        // РОУТ 5: ПРОФІЛЬ КУР'ЄРА
                        composable("profile") {
                            var profileData by remember { mutableStateOf<CourierProfile?>(null) }
                            var isLoading by remember { mutableStateOf(true) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            LaunchedEffect(Unit) {
                                coroutineScope.launch {
                                    try {
                                        profileData = RetrofitClient.apiService.getProfile(currentCookie)
                                    } catch (e: retrofit2.HttpException) {
                                        if (e.code() == 401 || e.code() == 403) forceLogout()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Помилка завантаження профілю", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }

                            ProfileScreen(
                                profile = profileData,
                                isLoading = isLoading,
                                onBack = { navController.popBackStack() },
                                onLogout = { forceLogout(isExplicitLogout = true) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- ФУНКЦІЇ ДЛЯ КОНТРОЛЮ ПЕРШОГО ЗАПУСКУ (ОНБОРДИНГ) ---
    private fun isFirstLaunch(): Boolean {
        val sharedPreferences = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun setFirstLaunchCompleted() {
        val sharedPreferences = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
    }
}