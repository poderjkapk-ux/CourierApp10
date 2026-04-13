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
import androidx.compose.foundation.layout.Box
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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.osmdroid.config.Configuration
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTracker::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
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

        Configuration.getInstance().userAgentValue = packageName
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

                    var isOnline by rememberSaveable { mutableStateOf(false) }

                    // ГЛОБАЛЬНИЙ СТАН: Персональне замовлення (Direct Offer)
                    var directOffer by remember { mutableStateOf<OpenOrder?>(null) }
                    var isDirectOfferLoading by remember { mutableStateOf(false) }

                    // ---> НОВА ФУНКЦІЯ: Надійна перевірка персональних замовлень через REST API
                    fun checkDirectOffers() {
                        val currentCookie = sharedPref.getString("cookie", null) ?: return
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val offers = RetrofitClient.apiService.getDirectOffers(currentCookie)
                                launch(Dispatchers.Main) {
                                    directOffer = offers.firstOrNull()
                                }
                            } catch (e: Exception) {
                                Log.e("DirectOffers", "Error fetching direct offers", e)
                            }
                        }
                    }

                    // ---> СПОСТЕРІГАЧ ЖИТТЄВОГО ЦИКЛУ: Перевіряємо замовлення щоразу, коли додаток відкривається
                    val globalLifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(globalLifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                checkDirectOffers()
                            }
                        }
                        globalLifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { globalLifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    // ---> ФОНОВИЙ ОПРОС: щоб точно нічого не пропустити
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(15000) // Раз на 15 секунд перевіряємо
                            checkDirectOffers()
                        }
                    }

                    fun forceLogout(isExplicitLogout: Boolean = false) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val currentCookie = sharedPref.getString("cookie", null)

                            if (isExplicitLogout && currentCookie != null && isOnline) {
                                try {
                                    RetrofitClient.apiService.toggleStatus(currentCookie, EmptyRequest())
                                } catch (e: Exception) {
                                    Log.e("Logout", "Не вдалося переключити статус на бекенді")
                                }
                            }

                            launch(Dispatchers.Main) {
                                isOnline = false
                                sharedPref.edit().remove("cookie").apply()
                                RetrofitClient.webSocketManager.disconnect()
                                stopService(Intent(this@MainActivity, LocationTracker::class.java))

                                if (!isExplicitLogout) {
                                    Toast.makeText(this@MainActivity, "Сесія закінчилась, увійдіть знову", Toast.LENGTH_LONG).show()
                                }

                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (savedCookie != null) {
                            RetrofitClient.webSocketManager.connect(savedCookie)

                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.sendFcmToken(savedCookie, token)
                                        } catch (e: Exception) {
                                            Log.e("FCM_TOKEN", "Помилка відправки токена при старті: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            RetrofitClient.webSocketManager.disconnect()
                        }
                    }

                    // --- ГЛОБАЛЬНЕ ПРОСЛУХОВУВАННЯ WEBSOCKET ---
                    LaunchedEffect(Unit) {
                        RetrofitClient.webSocketManager.messages.collect { messageJson ->
                            try {
                                val json = JSONObject(messageJson)
                                val type = json.getString("type")

                                if (type == "auth_error") {
                                    forceLogout()
                                } else if (type == "direct_offer") {
                                    try {
                                        // Намагаємось одразу розпарсити через WebSocket
                                        val orderObj = json.optJSONObject("data") ?: json.optJSONObject("order") ?: json.optJSONObject("job") ?: json
                                        directOffer = Gson().fromJson(orderObj.toString(), OpenOrder::class.java)
                                    } catch (e: Exception) {
                                        Log.e("WS", "Помилка парсингу direct_offer. Запит через API...", e)
                                        // Якщо парсинг впав — страхуємось і робимо запит до сервера
                                        checkDirectOffers()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

                    val hasLocationPermission = permissionsState.permissions.any {
                        (it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                                it.permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                                it.status.isGranted
                    }

                    var showLocationDisclosure by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(permissionsState.allPermissionsGranted, isOnline, hasLocationPermission) {
                        if (!permissionsState.allPermissionsGranted) {
                            val hasSeenDisclosure = sharedPref.getBoolean("has_seen_location_disclosure", false)
                            if (!hasSeenDisclosure) {
                                showLocationDisclosure = true
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        }

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

                    if (showLocationDisclosure) {
                        AlertDialog(
                            onDismissRequest = { },
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

                    val startDestination = if (isFirstLaunch()) {
                        "onboarding"
                    } else if (savedCookie != null) {
                        "orders"
                    } else {
                        "login"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDestination) {

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

                                                        RetrofitClient.webSocketManager.connect(cookieValue)

                                                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                val token = task.result
                                                                coroutineScope.launch {
                                                                    try {
                                                                        RetrofitClient.apiService.sendFcmToken(cookieValue, token)
                                                                    } catch (e: Exception) {}
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

                            composable("orders") {
                                var ordersList by remember { mutableStateOf<List<OpenOrder>>(emptyList()) }
                                var announcementsList by remember { mutableStateOf<List<Announcement>>(emptyList()) }
                                var isLoading by remember { mutableStateOf(true) }

                                val currentCookie = sharedPref.getString("cookie", "") ?: ""

                                val context = LocalContext.current
                                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                var isGpsEnabled by remember { mutableStateOf(true) }

                                fun fetchData(isSilent: Boolean = false) {
                                    if (!isSilent) isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val activeJobRes = RetrofitClient.apiService.getActiveJobs(currentCookie)
                                            if (activeJobRes.active && activeJobRes.jobs.isNotEmpty()) {
                                                navController.navigate("active_order") {
                                                    popUpTo("orders") { inclusive = true }
                                                }
                                                return@launch
                                            }

                                            try {
                                                announcementsList = RetrofitClient.apiService.getAnnouncements(currentCookie)
                                            } catch (e: Exception) { }

                                            var currentLat = 46.4825
                                            var currentLon = 30.7233

                                            if (hasLocationPermission) {
                                                val location = getLastKnownLocation()
                                                if (location != null) {
                                                    if (location.latitude > 45.0 && location.latitude < 48.0 && location.longitude > 29.0 && location.longitude < 32.0) {
                                                        currentLat = location.latitude
                                                        currentLon = location.longitude
                                                    }
                                                }
                                            }

                                            ordersList = RetrofitClient.apiService.getOpenOrders(
                                                currentCookie,
                                                lat = currentLat,
                                                lon = currentLon
                                            )
                                        } catch (e: retrofit2.HttpException) {
                                            if (e.code() == 401 || e.code() == 403) forceLogout()
                                        } catch (e: Exception) {
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

                                LaunchedEffect(Unit) {
                                    coroutineScope.launch {
                                        try {
                                            val profile = RetrofitClient.apiService.getProfile(currentCookie)
                                            isOnline = profile.isOnline
                                        } catch (e: retrofit2.HttpException) {
                                            if (e.code() == 401 || e.code() == 403) forceLogout()
                                        } catch (e: Exception) { }
                                    }
                                    fetchData(isSilent = false)
                                }

                                LaunchedEffect(Unit) {
                                    while (true) {
                                        delay(30000)
                                        fetchData(isSilent = true)
                                    }
                                }

                                LaunchedEffect(Unit) {
                                    RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                        try {
                                            val json = JSONObject(messageJson)
                                            val type = json.getString("type")

                                            if (type == "auth_error") {
                                                forceLogout()
                                            } else if (type == "new_order" || type == "job_update" || type == "job_ready") {
                                                fetchData(isSilent = true)
                                            }
                                        } catch (e: Exception) { }
                                    }
                                }

                                OrdersListScreen(
                                    orders = ordersList,
                                    announcements = announcementsList,
                                    isLoading = isLoading,
                                    isOnline = isOnline,
                                    isGpsEnabled = isGpsEnabled,
                                    onNavigateToHistory = {
                                        navController.navigate("history")
                                    },
                                    onNavigateToProfile = {
                                        navController.navigate("profile")
                                    },
                                    onDismissAnnouncement = { annId ->
                                        announcementsList = announcementsList.filter { it.id != annId }
                                        coroutineScope.launch {
                                            try {
                                                RetrofitClient.apiService.dismissAnnouncement(currentCookie, annId)
                                            } catch (e: Exception) { }
                                        }
                                    },
                                    onToggleStatus = { _ ->
                                        coroutineScope.launch {
                                            try {
                                                val response = RetrofitClient.apiService.toggleStatus(currentCookie, EmptyRequest())
                                                isOnline = response.isOnline
                                            } catch (e: retrofit2.HttpException) {
                                                if (e.code() == 401 || e.code() == 403) forceLogout()
                                                else Toast.makeText(this@MainActivity, "Помилка зв'язку з сервером", Toast.LENGTH_SHORT).show()
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

                            composable("active_order") {
                                var activeJob by remember { mutableStateOf<ActiveJobDetail?>(null) }
                                var activeJobsList by remember { mutableStateOf<List<ActiveJobSummary>>(emptyList()) }
                                var selectedJobId by remember { mutableStateOf<Int?>(null) }
                                val currentCookie = sharedPref.getString("cookie", "") ?: ""

                                fun fetchActiveData() {
                                    coroutineScope.launch {
                                        try {
                                            val resList = RetrofitClient.apiService.getActiveJobs(currentCookie)
                                            if (resList.active && resList.jobs.isNotEmpty()) {
                                                activeJobsList = resList.jobs
                                                val targetId = if (activeJobsList.any { it.id == selectedJobId }) selectedJobId else activeJobsList.first().id
                                                selectedJobId = targetId

                                                val resJob = RetrofitClient.apiService.getActiveJob(currentCookie, targetId)
                                                if (resJob.active && resJob.job != null) {
                                                    activeJob = resJob.job
                                                }
                                            } else {
                                                navController.navigate("orders") { popUpTo("active_order") { inclusive = true } }
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            if (e.code() == 401 || e.code() == 403) forceLogout()
                                        } catch (e: Exception) {}
                                    }
                                }

                                val lifecycleOwner = LocalLifecycleOwner.current
                                DisposableEffect(lifecycleOwner) {
                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) {
                                            fetchActiveData()
                                        }
                                    }
                                    lifecycleOwner.lifecycle.addObserver(observer)
                                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                }

                                LaunchedEffect(Unit) { fetchActiveData() }

                                LaunchedEffect(Unit) {
                                    RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                        try {
                                            val json = JSONObject(messageJson)
                                            val type = json.getString("type")

                                            if (type == "auth_error") {
                                                forceLogout()
                                            } else if (type == "job_update" || type == "job_ready" || type == "new_order") {
                                                fetchActiveData()
                                            }
                                        } catch (e: Exception) { }
                                    }
                                }

                                activeJob?.let { job ->
                                    ActiveOrderScreen(
                                        job = job,
                                        activeJobsList = activeJobsList,
                                        onJobSelected = { id ->
                                            selectedJobId = id
                                            fetchActiveData()
                                        },
                                        cookie = currentCookie,
                                        onRefresh = { fetchActiveData() },
                                        onArrivedPickup = { jobId ->
                                            coroutineScope.launch {
                                                try { RetrofitClient.apiService.arrivedAtPickup(currentCookie, jobId); fetchActiveData() } catch (e: Exception) {}
                                            }
                                        },
                                        onUpdateStatus = { jobId, status ->
                                            coroutineScope.launch {
                                                try { RetrofitClient.apiService.updateJobStatus(currentCookie, jobId, status); fetchActiveData() } catch (e: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }

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

                            composable("profile") {
                                var profileData by remember { mutableStateOf<CourierProfile?>(null) }
                                var motivatorsList by remember { mutableStateOf<List<Motivator>>(emptyList()) }
                                var isLoading by remember { mutableStateOf(true) }
                                val currentCookie = sharedPref.getString("cookie", "") ?: ""

                                LaunchedEffect(Unit) {
                                    coroutineScope.launch {
                                        try {
                                            val profileTask = launch { profileData = RetrofitClient.apiService.getProfile(currentCookie) }
                                            val motivatorsTask = launch {
                                                try {
                                                    motivatorsList = RetrofitClient.apiService.getMotivators(currentCookie)
                                                } catch (e: Exception) { }
                                            }

                                            profileTask.join()
                                            motivatorsTask.join()

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
                                    motivators = motivatorsList,
                                    isLoading = isLoading,
                                    onBack = { navController.popBackStack() },
                                    onLogout = { forceLogout(isExplicitLogout = true) }
                                )
                            }
                        }

                        // --- ГЛОБАЛЬНЕ ВСПЛИВАЮЧЕ ВІКНО ПЕРСОНАЛЬНОГО ЗАМОВЛЕННЯ ---
                        directOffer?.let { offer ->
                            DirectOfferDialog(
                                offer = offer,
                                isLoading = isDirectOfferLoading,
                                onAccept = {
                                    isDirectOfferLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val cookie = sharedPref.getString("cookie", "") ?: ""
                                            val response = RetrofitClient.apiService.acceptOrder(cookie, offer.id)
                                            if (response.isSuccessful) {
                                                directOffer = null
                                                navController.navigate("active_order") { popUpTo("orders") { inclusive = true } }
                                            } else {
                                                Toast.makeText(this@MainActivity, "Замовлення вже недоступне", Toast.LENGTH_SHORT).show()
                                                directOffer = null
                                                checkDirectOffers() // Одразу перевіряємо чи немає іншого
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
                                            directOffer = null
                                            checkDirectOffers()
                                        } finally {
                                            isDirectOfferLoading = false
                                        }
                                    }
                                },
                                onDecline = {
                                    isDirectOfferLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val cookie = sharedPref.getString("cookie", "") ?: ""
                                            RetrofitClient.apiService.declineDirectOrder(cookie, offer.id)
                                        } catch (e: Exception) {}
                                        finally {
                                            directOffer = null
                                            isDirectOfferLoading = false
                                            checkDirectOffers() // Якщо відмовились - можливо є інше
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isFirstLaunch(): Boolean {
        val sharedPreferences = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun setFirstLaunchCompleted() {
        val sharedPreferences = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
    }
}