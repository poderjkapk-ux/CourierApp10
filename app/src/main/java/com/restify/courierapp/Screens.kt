package com.restify.courierapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Locale

// ==========================================
// 0. ДИЗАЙН-СИСТЕМА (Кольори та Кастомні Компоненти)
// ==========================================

object AppColors {
    val Primary = Color(0xFF1E293B) // Глибокий преміальний темний (Dark Slate)
    val PrimaryDark = Color(0xFF0F172A)
    val Secondary = Color(0xFF10B981) // Соковитий смарагдовий
    val Background = Color(0xFFF8FAFC) // Чистий світло-сірий фон
    val Surface = Color.White
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6) // Приємний синій для інформаційних оголошень
    val ChatBubbleSelf = Color(0xFF1E293B)
    val ChatTextSelf = Color.White
    val ChatBubbleOther = Color(0xFFF1F5F9)
    val ChatTextOther = Color(0xFF0F172A)
    val Warning = Color(0xFFF59E0B)
    val Inactive = Color(0xFFCBD5E1)
}

// Кастомна кнопка з кольором
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (enabled) 6.dp else 0.dp, pressedElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// Кастомне поле вводу
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppColors.TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = AppColors.Primary,
            cursorColor = AppColors.Primary,
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Background.copy(alpha = 0.5f)
        ),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true
    )
}

// Елемент списку з іконкою для адрес
@Composable
fun AddressItem(icon: ImageVector, text: String, label: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            if (label != null) {
                Text(label, fontSize = 13.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
            }
            Text(text, fontSize = 16.sp, color = AppColors.TextPrimary, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ==========================================
// КОМПОНЕНТИ ТАЙМЕРІВ ТА ЧАСУ
// ==========================================

@Composable
fun CurrentTimeDisplay() {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = format.format(Date())
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .background(AppColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = currentTime,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.Primary
        )
    }
}

@Composable
fun StepTimer(
    startTimeIso: String?,
    endTimeIso: String?,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (startTimeIso.isNullOrEmpty()) return

    var durationText by remember { mutableStateOf("00:00") }

    LaunchedEffect(startTimeIso, endTimeIso, isActive) {
        while (true) {
            try {
                val start = Instant.parse(startTimeIso)
                val end = if (!endTimeIso.isNullOrEmpty()) {
                    Instant.parse(endTimeIso)
                } else if (isActive) {
                    Instant.now()
                } else {
                    null
                }

                if (end != null) {
                    val duration = Duration.between(start, end)
                    val s = duration.seconds
                    if (s >= 0) {
                        val m = s / 60
                        val h = m / 60
                        durationText = if (h > 0) {
                            String.format("%02d:%02d:%02d", h, m % 60, s % 60)
                        } else {
                            String.format("%02d:%02d", m, s % 60)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ігноруємо помилки парсингу дат
            }

            if (!endTimeIso.isNullOrEmpty() || !isActive) {
                break // Зупиняємо "тікання", якщо час зафіксований
            }
            delay(1000L)
        }
    }

    val backgroundColor = if (!endTimeIso.isNullOrEmpty()) {
        AppColors.Secondary.copy(alpha = 0.15f)
    } else if (isActive) {
        AppColors.Primary.copy(alpha = 0.15f)
    } else {
        Color.LightGray.copy(alpha = 0.2f)
    }

    val textColor = if (!endTimeIso.isNullOrEmpty()) AppColors.Secondary else if (isActive) AppColors.Primary else AppColors.TextSecondary

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⏳ $durationText",
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

// --- КОМПАКТНЫЙ ТАЙМЕР ДЛЯ СПИСКА ЗАКАЗОВ ---
@Composable
fun ReadinessBadge(readyAtIso: String?, isReady: Boolean, modifier: Modifier = Modifier) {
    if (isReady) {
        Box(modifier = modifier.background(AppColors.Secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("✨ Вже готово", color = AppColors.Secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        return
    }
    if (readyAtIso.isNullOrEmpty()) return

    var timeDiff by remember { mutableLongStateOf(0L) }

    LaunchedEffect(readyAtIso) {
        while (true) {
            try {
                val readyInstant = Instant.parse(readyAtIso)
                val now = Instant.now()
                timeDiff = Duration.between(now, readyInstant).seconds
            } catch (e: Exception) {}
            delay(1000L)
        }
    }

    val isLate = timeDiff < 0
    val absDiff = kotlin.math.abs(timeDiff)
    val m = absDiff / 60
    val s = absDiff % 60
    val timeString = String.format("%02d:%02d", m, s)

    val bgColor = if (isLate) AppColors.Error.copy(alpha = 0.15f) else if (m < 5) AppColors.Warning.copy(alpha = 0.15f) else AppColors.Primary.copy(alpha = 0.1f)
    val textColor = if (isLate) AppColors.Error else if (m < 5) Color(0xFFB45309) else AppColors.Primary
    val icon = if (isLate) "🚨" else if (m < 5) "🟠" else "🕰️"
    val textPrefix = if (isLate) "Запізнення" else if (m < 5) "Майже готово" else "Готується"

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$textPrefix $timeString",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// --- БОЛЬШОЙ ТАЙМЕР ДЛЯ АКТИВНОГО ЗАКАЗА ---
@Composable
fun LargeReadinessTimer(readyAtIso: String?) {
    if (readyAtIso.isNullOrEmpty()) return

    var timeDiff by remember { mutableLongStateOf(0L) }

    LaunchedEffect(readyAtIso) {
        while (true) {
            try {
                val readyInstant = Instant.parse(readyAtIso)
                val now = Instant.now()
                timeDiff = Duration.between(now, readyInstant).seconds
            } catch (e: Exception) {}
            delay(1000L)
        }
    }

    val isLate = timeDiff < 0
    val absDiff = kotlin.math.abs(timeDiff)
    val m = absDiff / 60
    val s = absDiff % 60
    val timeString = String.format("%02d:%02d", m, s)

    val bgColor = if (isLate) AppColors.Error else AppColors.Primary
    val titleText = if (isLate) "ЗАКЛАД ЗАТРИМУЄ ВИДАЧУ" else "ОЧІКУВАНИЙ ЧАС ВИДАЧІ"
    val subtitleText = if (isLate) "Замовлення вже мало бути готовим" else "Ресторан ще готує страви"

    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = bgColor.copy(alpha = 0.4f))
        .background(Brush.horizontalGradient(listOf(bgColor, bgColor.copy(alpha = 0.8f))), RoundedCornerShape(20.dp))
        .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isLate) Icons.Rounded.Warning else Icons.Rounded.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titleText, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitleText, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                text = "${if(isLate) "-" else ""}$timeString",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White
            )
        }
    }
}

// ==========================================
// КОМПОНЕНТ КАРТКИ ОГОЛОШЕННЯ (Announcement)
// ==========================================

@Composable
fun AnnouncementCard(
    announcement: Announcement,
    onDismiss: (Int) -> Unit
) {
    // Мапінг стилів з бекенду на кольори та іконки
    val (bgColor, contentColor, icon) = when (announcement.style) {
        "danger" -> Triple(AppColors.Error.copy(alpha = 0.15f), AppColors.Error, Icons.Rounded.Warning)
        "warning" -> Triple(AppColors.Warning.copy(alpha = 0.15f), Color(0xFFB45309), Icons.Rounded.Warning)
        "success" -> Triple(AppColors.Secondary.copy(alpha = 0.15f), AppColors.Secondary, Icons.Rounded.CheckCircle)
        else -> Triple(AppColors.Info.copy(alpha = 0.15f), AppColors.Info, Icons.Rounded.Info) // info за замовчуванням
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = contentColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = announcement.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = announcement.message,
                    fontSize = 14.sp,
                    color = AppColors.TextPrimary.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onDismiss(announcement.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Закрити",
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}


// ==========================================
// 1. ЕКРАН АВТОРИЗАЦІЇ (Login Screen)
// ==========================================
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    val coroutineScope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // --- СТАН ДЛЯ МОДАЛКИ СКИДАННЯ ПАРОЛЯ ---
    var showResetDialog by remember { mutableStateOf(false) }
    var resetPhone by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(colors = listOf(AppColors.Primary, AppColors.PrimaryDark)))
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(28.dp)
            ) {
                Text("Restify", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text("Delivery", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 220.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = AppColors.PrimaryDark.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Вхід для кур'єра", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Spacer(modifier = Modifier.height(28.dp))

                ModernTextField(value = phone, onValueChange = { phone = it }, label = "Номер тел. (починаючи 380)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(value = password, onValueChange = { password = it }, label = "Пароль", visualTransformation = PasswordVisualTransformation())

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMessage, color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                ModernButton(text = "Увійти", onClick = { onLoginClick(phone, password) }, modifier = Modifier.fillMaxWidth(), isLoading = isLoading, enabled = phone.isNotBlank() && password.isNotBlank(), icon = Icons.Rounded.ArrowForward)

                // --- КНОПКА ЗАБУЛИ ПАРОЛЬ ---
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Забули пароль?", color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text("Немає акаунту? Зареєструватися", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // --- МОДАЛЬНЕ ВІКНО СКИДАННЯ ПАРОЛЯ ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isResetLoading) showResetDialog = false },
            title = { Text("Відновлення пароля", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) },
            text = {
                Column {
                    Text("Введіть номер телефону. Новий пароль буде надіслано вам у Telegram.", fontSize = 14.sp, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernTextField(
                        value = resetPhone,
                        onValueChange = { resetPhone = it },
                        label = "Номер тел. (починаючи 380)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = resetMessage!!,
                            color = if (isResetSuccess) AppColors.Secondary else AppColors.Error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetPhone.isBlank()) {
                            resetMessage = "⚠️ Введіть номер телефону"
                            isResetSuccess = false
                            return@Button
                        }
                        coroutineScope.launch {
                            isResetLoading = true
                            resetMessage = null
                            try {
                                val res = RetrofitClient.apiService.resetCourierPassword(resetPhone)
                                if (res.isSuccessful) {
                                    isResetSuccess = true
                                    resetMessage = "✨ Новий пароль надіслано в Telegram!"
                                    delay(3000)
                                    showResetDialog = false
                                    resetMessage = null
                                    resetPhone = ""
                                } else {
                                    isResetSuccess = false
                                    val errorBody = res.errorBody()?.string()
                                    val detail = try { JSONObject(errorBody ?: "").getString("detail") } catch (e: Exception) { "Помилка скидання" }
                                    resetMessage = "⚠️ $detail"
                                }
                            } catch (e: Exception) {
                                isResetSuccess = false
                                resetMessage = "⚠️ Помилка мережі"
                            } finally {
                                isResetLoading = false
                            }
                        }
                    },
                    enabled = !isResetLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Надіслати")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }, enabled = !isResetLoading) {
                    Text("Скасувати", color = AppColors.TextSecondary)
                }
            },
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ==========================================
// 1.5. ЕКРАН РЕЄСТРАЦІЇ (ІЗ БЕЗПЕЧНИМ ВІДКРИТТЯМ TG З APP10)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationToken by remember { mutableStateOf<String?>(null) }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var phoneFromServer by remember { mutableStateOf("") }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> documentUri = uri }
    val selfiePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selfieUri = uri }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Стати кур'єром", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = AppColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isPhoneVerified) AppColors.Secondary else AppColors.Primary), contentAlignment = Alignment.Center) {
                    if (isPhoneVerified) Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    else Text("1", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (isPhoneVerified) AppColors.Secondary else Color.LightGray))
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isPhoneVerified) AppColors.Primary else Color.LightGray), contentAlignment = Alignment.Center) {
                    Text("2", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isPhoneVerified) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Call, null, modifier = Modifier.size(48.dp), tint = AppColors.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Підтвердження номеру", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Для безпеки нам потрібно підтвердити ваш номер через нашого Telegram бота.", textAlign = TextAlign.Center, color = AppColors.TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        ModernButton(
                            text = "Відкрити Telegram",
                            isLoading = isLoading,
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        // ВИПРАВЛЕНО: Використовуємо EmptyRequest(), як було в надійній версії App10
                                        val res = RetrofitClient.apiService.initVerification(EmptyRequest())
                                        if (res.isSuccessful && res.body() != null) {
                                            verificationToken = res.body()!!.token
                                            var telegramLink = res.body()!!.link

                                            Log.d("TG_LINK", "Отримано лінк від сервера: $telegramLink")

                                            // 1. ПРЕОБРАЗОВАНИЕ В ПРЯМОЙ DEEPLINK (tg://) ДЛЯ ОБХОДА БРАУЗЕРА
                                            if (telegramLink.contains("t.me/")) {
                                                val path = telegramLink.substringAfter("t.me/")
                                                val parts = path.split("?")
                                                val botUsername = parts[0].replace("/", "") // Имя бота
                                                val startParam = if (parts.size > 1) "&${parts[1]}" else "" // Токен (например &start=123)

                                                telegramLink = "tg://resolve?domain=$botUsername$startParam"
                                            } else if (!telegramLink.startsWith("http") && !telegramLink.startsWith("tg://")) {
                                                telegramLink = "tg://resolve?domain=$telegramLink" // Если пришло просто имя
                                            }

                                            Log.d("TG_LINK", "Конвертированный линк: $telegramLink")

                                            // 2. БЕЗОПАСНЫЙ И ПРЯМОЙ ЗАПУСК ТЕЛЕГРАМА
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramLink))
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                                                try {
                                                    // Жестко направляем в официальный клиент Telegram
                                                    intent.setPackage("org.telegram.messenger")
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // Если официального клиента нет (может быть установлен Telegram X или Bgram),
                                                    // убираем жесткую привязку к пакету и пытаемся открыть через общий обработчик tg://
                                                    intent.setPackage(null)
                                                    context.startActivity(intent)
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = "Не вдалося відкрити Telegram. Перевірте, чи встановлено додаток."
                                                isLoading = false
                                                return@launch
                                            }

                                            // 3. ПИНГУЕМ СЕРВЕР, ПОКА ПОЛЬЗОВАТЕЛЬ В ТЕЛЕГРАМЕ
                                            var attempts = 0
                                            val maxAttempts = 100 // Ограничиваем опрос ~5 минутами (100 попыток по 3 сек)

                                            while (!isPhoneVerified && attempts < maxAttempts) {
                                                delay(3000)
                                                attempts++

                                                try {
                                                    val check = RetrofitClient.apiService.checkVerification(verificationToken!!)
                                                    if (check.isSuccessful && check.body()?.status == "verified") {
                                                        isPhoneVerified = true
                                                        phoneFromServer = check.body()?.phone ?: ""
                                                        break
                                                    }
                                                } catch (e: Exception) {
                                                    // Игнорируем кратковременные ошибки сети при переключении приложений.
                                                    // Цикл просто пойдет на следующую итерацию.
                                                    Log.w("TG_VERIFICATION", "Помилка мережі під час перевірки, продовжуємо: ${e.message}")
                                                }
                                            }

                                            // Если после возврата из цикла статус всё еще не подтвержден
                                            if (!isPhoneVerified && attempts >= maxAttempts) {
                                                errorMessage = "Час очікування вийшов. Спробуйте ще раз."
                                            }
                                        } else {
                                            errorMessage = "Помилка сервера: ${res.code()}"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Сбой: ${e.localizedMessage ?: e.javaClass.simpleName}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    text = "Номер підтверджено: $phoneFromServer",
                    color = AppColors.Secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                ModernTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Ваше Ім'я"
                )
                Spacer(modifier = Modifier.height(12.dp))

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Придумайте пароль",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Документи", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text("Завантажте чіткі фото для перевірки", fontSize = 14.sp, color = AppColors.TextSecondary)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (documentUri != null) AppColors.Secondary.copy(alpha = 0.1f) else Color.Transparent)
                            .border(2.dp, if (documentUri != null) AppColors.Secondary else Color.LightGray, RoundedCornerShape(16.dp))
                            .clickable { docPicker.launch("image/*") },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(if (documentUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.Add, contentDescription = null, tint = if (documentUri != null) AppColors.Secondary else Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Фото ID/Паспорт", fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (selfieUri != null) AppColors.Secondary.copy(alpha = 0.1f) else Color.Transparent)
                            .border(2.dp, if (selfieUri != null) AppColors.Secondary else Color.LightGray, RoundedCornerShape(16.dp))
                            .clickable { selfiePicker.launch("image/*") },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(if (selfieUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.Person, contentDescription = null, tint = if (selfieUri != null) AppColors.Secondary else Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ваше Селфі", fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                ModernButton(
                    text = "Відправити заявку",
                    isLoading = isLoading,
                    onClick = {
                        if (name.isBlank() || password.isBlank() || documentUri == null || selfieUri == null) {
                            errorMessage = "Заповніть всі дані та додайте фотографії"
                        } else {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val textMediaType = MediaType.parse("text/plain")
                                    val nameReq = RequestBody.create(textMediaType, name)
                                    val passReq = RequestBody.create(textMediaType, password)
                                    val tokenReq = RequestBody.create(textMediaType, verificationToken!!)

                                    val docPart = prepareFilePart(context, "document_photo", documentUri!!)
                                    val selfiePart = prepareFilePart(context, "selfie_photo", selfieUri!!)

                                    if (docPart != null && selfiePart != null) {
                                        val res = RetrofitClient.apiService.registerCourier(
                                            nameReq, passReq, tokenReq, docPart, selfiePart
                                        )
                                        if (res.isSuccessful) onRegisterSuccess()
                                        else errorMessage = "Помилка відправки даних на сервер"
                                    } else {
                                        errorMessage = "Помилка обробки файлів"
                                    }
                                } catch (e: Exception) { errorMessage = "Помилка мережі при відправці" }
                                finally { isLoading = false }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(it, color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ВИПРАВЛЕНО: Додано .use { } для автоматичного закриття потоків і запобігання витоку файлових дескрипторів
fun prepareFilePart(context: Context, partName: String, fileUri: Uri): MultipartBody.Part? {
    return try {
        val tempFile = File(context.cacheDir, "${partName}_temp.jpg")
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return null
        val requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile)
        MultipartBody.Part.createFormData(partName, tempFile.name, requestFile)
    } catch (e: Exception) { null }
}

// ==========================================
// 2. ЕКРАН СПИСКУ ЗАМОВЛЕНЬ (Orders List)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OpenOrder>,
    announcements: List<Announcement>, // ДОДАНО
    isOnline: Boolean,
    isGpsEnabled: Boolean, // Додано параметр статусу GPS
    onToggleStatus: (Boolean) -> Unit,
    onAcceptOrder: (Int, () -> Unit) -> Unit,
    onDismissAnnouncement: (Int) -> Unit, // ДОДАНО
    onRefresh: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current // Додано для Intent до налаштувань

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Доступні", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background, titleContentColor = AppColors.TextPrimary),
                actions = {
                    CurrentTimeDisplay()
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Rounded.DateRange, contentDescription = "Історія", tint = AppColors.Primary)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Rounded.Person, contentDescription = "Профіль", tint = AppColors.Primary)
                    }
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = if (isOnline) AppColors.Secondary.copy(alpha = 0.15f) else AppColors.TextSecondary.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(end = 20.dp).clip(RoundedCornerShape(50)).clickable { onToggleStatus(!isOnline) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(if (isOnline) AppColors.Secondary else AppColors.TextSecondary, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isOnline) "Онлайн" else "Офлайн", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isOnline) AppColors.Secondary else AppColors.TextSecondary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // --- КРАСИВИЙ БАНЕР: ПРОХАННЯ УВІМКНУТИ GPS ---
            if (!isGpsEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Warning.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, AppColors.Warning),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Геолокацію вимкнено", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary)
                            Text("Увімкніть GPS, щоб бачити реальну відстань до замовлень, а не 5000 км 🙌", fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Warning),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Увімкнути", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                if (orders.isEmpty() && announcements.isEmpty() && !isLoading) { // ОНОВЛЕНО: Враховуємо оголошення
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Зараз немає замовлень", color = AppColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- ДОДАНО БЛОК ОГОЛОШЕНЬ ---
                        if (announcements.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    announcements.forEach { ann ->
                                        AnimatedVisibility(
                                            visible = true,
                                            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                                        ) {
                                            AnnouncementCard(
                                                announcement = ann,
                                                onDismiss = onDismissAnnouncement
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        // ------------------------------------

                        items(orders) { order -> OrderCard(order = order, onAcceptClick = onAcceptOrder) }
                    }
                }
            }
        }
    }
}

// ОНОВЛЕНА КАРТКА ЗАМОВЛЕННЯ З ПЛАВНИМ РОЗГОРТАННЯМ
@Composable
fun OrderCard(order: OpenOrder, onAcceptClick: (Int, () -> Unit) -> Unit) {
    var isAccepting by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val paymentInfo = when (order.paymentType) {
        "prepaid" -> Pair("✨ Оплачено", AppColors.Secondary)
        "buyout_paid" -> Pair("✨ Оплачено", AppColors.Secondary)
        "cash" -> Pair("💸 Готівка", AppColors.Warning)
        "buyout" -> Pair("💳 Викуп", AppColors.Error)
        else -> Pair(order.paymentType, AppColors.Primary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // КОМПАКТНА ЧАСТИНА
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.restaurantName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!expanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.restaurantAddress, fontSize = 14.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(modifier = Modifier.background(AppColors.Secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("${order.fee.toInt()} ₴", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.Secondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.background(paymentInfo.second.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(paymentInfo.first, color = paymentInfo.second, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (order.distToRest != null) {
                    Box(modifier = Modifier.background(AppColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("🛵 ~${order.distToRest} км", color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    tint = AppColors.TextSecondary
                )
            }

            // --- ДОДАНО ТАЙМЕР ---
            if (order.readyAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ReadinessBadge(readyAtIso = order.readyAt, isReady = false)
            }
            // ---------------------

            // ДЕТАЛЬНА ЧАСТИНА (ВІДЧИНЯЄТЬСЯ)
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // ВИПРАВЛЕНО ТЕКСТ ПРО ВИКУП
                if (order.paymentType == "buyout") {
                    Box(modifier = Modifier.fillMaxWidth().background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(
                            text = "Увага: Заберіть ${order.price} ₴ у клієнта. Повертатися в заклад не потрібно (викуп за власні кошти).",
                            color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AddressItem(icon = Icons.Rounded.LocationOn, text = order.restaurantAddress, label = "Забрати")

                Box(modifier = Modifier.padding(start = 19.dp, top = 6.dp, bottom = 6.dp).height(20.dp).width(2.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                AddressItem(icon = Icons.Rounded.Home, text = order.dropoffAddress, label = "Доставити")

                if (!order.comment.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text("📝 Коментар: ${order.comment}", fontSize = 14.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (order.distTrip != null) {
                        Text("🧭 Маршрут: ~${order.distTrip} км", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    ModernButton(
                        text = "Прийняти",
                        onClick = {
                            isAccepting = true
                            onAcceptClick(order.id) {
                                isAccepting = false
                            }
                        },
                        modifier = Modifier.height(48.dp).width(140.dp),
                        backgroundColor = AppColors.Primary,
                        isLoading = isAccepting
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. ЕКРАН АКТИВНОГО ЗАМОВЛЕННЯ (Active Order)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrderScreen(
    job: ActiveJobDetail,
    cookie: String,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Деталі", "Чат")

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(AppColors.Surface, AppColors.Background)))
                    .shadow(elevation = 8.dp, spotColor = AppColors.Primary.copy(alpha = 0.1f))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Замовлення #${job.id}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                            Text(
                                text = getStatusText(job.serverStatus, job.isReady),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = getStatusColor(job.serverStatus, job.isReady)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        CurrentTimeDisplay()
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.padding(end = 8.dp).background(AppColors.Primary.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Refresh, "Оновити", tint = AppColors.Primary)
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 4.dp,
                            color = AppColors.Primary
                        )
                    },
                    divider = { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold, fontSize = 16.sp) },
                            selectedContentColor = AppColors.Primary,
                            unselectedContentColor = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            when (selectedTabIndex) {
                0 -> OrderDetailsView(job, onArrivedPickup, onUpdateStatus, onRefresh)
                1 -> ChatView(job.id, cookie)
            }
        }
    }
}

fun getStatusText(status: String, isReady: Boolean): String {
    if (isReady) return "Замовлення готове! Можна забирати."
    return when(status) {
        "assigned" -> "Прямуйте до закладу"
        "arrived_pickup" -> "Очікуйте видачі"
        "ready" -> "Замовлення готове! Можна забирати."
        "picked_up" -> "Прямуйте до клієнта"
        "returning" -> "Повернення коштів у заклад"
        else -> status
    }
}

fun getStatusColor(status: String, isReady: Boolean): Color {
    if (isReady) return AppColors.Secondary
    return when(status) {
        "assigned", "picked_up" -> AppColors.Primary
        "ready" -> AppColors.Secondary
        "returning" -> AppColors.Warning
        else -> AppColors.TextSecondary
    }
}

// --- ПІД-ЕКРАН: ДЕТАЛІ ЗАМОВЛЕННЯ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsView(
    job: ActiveJobDetail,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var isActionLoading by remember(job.serverStatus) { mutableStateOf(false) }

    val handleRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            onRefresh()
            delay(500)
            isRefreshing = false
        }
    }

    val openGoogleMaps = { address: String, lat: Double?, lon: Double? ->
        try {
            val uri = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                Uri.parse("google.navigation:q=$lat,$lon")
            } else {
                Uri.parse("google.navigation:q=${Uri.encode(address)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // ВИПРАВЛЕНО: Правильне веб-посилання на Google Карти
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Помилка відкриття карт", Toast.LENGTH_SHORT).show()
        }
    }

    val isStep1Active = job.serverStatus in listOf("assigned", "arrived_pickup", "ready")
    val isStep1Done = job.serverStatus in listOf("picked_up", "delivered", "returning")
    val isStep2Active = job.serverStatus == "picked_up"
    val isStep2Done = job.serverStatus in listOf("delivered", "returning")

    val isOrderReady = job.isReady || job.serverStatus == "ready"

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = handleRefresh,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {

                if (isOrderReady) {
                    item {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = AppColors.Secondary.copy(alpha = 0.4f))
                            .background(Brush.horizontalGradient(listOf(AppColors.Secondary, Color(0xFF059669))), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("ЗАМОВЛЕННЯ ГОТОВЕ!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Можете забирати пакунок у закладі.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                } else if (isStep1Active && !job.readyAt.isNullOrEmpty()) {
                    // --- ДОДАНО ВЕЛИКИЙ ТАЙМЕР ---
                    item {
                        LargeReadinessTimer(job.readyAt)
                    }
                    // -----------------------------
                }

                // --- ФІНАНСИ ТА ОПЛАТА ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("ВАШ ДОХІД", color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${job.deliveryFee} ₴", fontWeight = FontWeight.Black, color = AppColors.Secondary, fontSize = 28.sp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("СУМА ЗАМОВЛЕННЯ", color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${job.orderPrice} ₴", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AppColors.TextPrimary)
                                }
                            }

                            // --- ВИПРАВЛЕНО: ТЕКСТИ ДЛЯ ВИКУПУ ---
                            val paymentInfo = when (job.paymentType) {
                                "prepaid" -> Pair("✨ ОПЛАЧЕНО (Гроші не беремо)", AppColors.Secondary)
                                "buyout_paid" -> Pair("✨ ОПЛАЧЕНО В ЗАКЛАДі (Свої гроші: ${job.orderPrice} ₴)", AppColors.Secondary)
                                "cash" -> Pair("💸 ГОТІВКА (Взяти ${job.orderPrice} ₴)", AppColors.Warning)
                                "buyout" -> Pair("💳 ЗАБЕРІТЬ У КЛІЄНТА: ${job.orderPrice} ₴ (Свої гроші)", AppColors.Error)
                                else -> Pair("Оплата: ${job.paymentType}", AppColors.Primary)
                            }

                            Box(modifier = Modifier.fillMaxWidth().background(paymentInfo.second.copy(alpha = 0.12f)).padding(16.dp)) {
                                Text(paymentInfo.first, fontWeight = FontWeight.ExtraBold, color = paymentInfo.second, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }

                            if (!job.comment.isNullOrEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED)).padding(16.dp)) {
                                    Text("📝 Коментар: ${job.comment}", fontSize = 15.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // --- КАРТКА ЗАКЛАДУ ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isStep1Active || isStep1Done) 1f else 0.5f)
                            .border(BorderStroke(if (isStep1Active) 2.dp else 1.dp, if (isStep1Active) AppColors.Primary else Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
                            .shadow(if (isStep1Active) 12.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = if (isStep1Active) AppColors.Primary.copy(alpha = 0.2f) else Color.Black.copy(0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.background(if (isStep1Active) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("КРОК 1: ЗАКЛАД", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isStep1Active) AppColors.Primary else AppColors.TextSecondary, letterSpacing = 1.sp)
                                        }
                                        StepTimer(
                                            startTimeIso = job.assignedAt,
                                            endTimeIso = job.pickedUpAt,
                                            isActive = isStep1Active
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(job.partnerName, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            AddressItem(icon = Icons.Rounded.LocationOn, text = job.partnerAddress)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.partnerAddress, null, null) },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                if (!job.partnerPhone.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Card(
                                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f)),
                                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                            val phone = job.partnerPhone.replace("+", "")
                                            val formattedPhone = if (phone.startsWith("380")) phone.substring(2) else phone
                                            try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))) } catch (e: Exception) {}
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Rounded.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- КАРТКА КЛІЄНТА ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isStep2Active || isStep2Done) 1f else 0.5f)
                            .border(BorderStroke(if (isStep2Active) 2.dp else 1.dp, if (isStep2Active) AppColors.Primary else Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
                            .shadow(if (isStep2Active) 12.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = if (isStep2Active) AppColors.Primary.copy(alpha = 0.2f) else Color.Black.copy(0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.background(if (isStep2Active) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("КРОК 2: КЛІЄНТ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isStep2Active) AppColors.Primary else AppColors.TextSecondary, letterSpacing = 1.sp)
                                        }
                                        StepTimer(
                                            startTimeIso = job.pickedUpAt,
                                            endTimeIso = job.deliveredAt,
                                            isActive = isStep2Active
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(job.customerName ?: "Ім'я не вказано", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            AddressItem(icon = Icons.Rounded.Home, text = job.customerAddress)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.customerAddress, job.customerLat, job.customerLon) },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f)),
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                        val phone = job.customerPhone.toString().replace("+", "")
                                        val formattedPhone = if (phone.startsWith("380")) phone.substring(2) else phone
                                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))) } catch (e: Exception) {}
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Rounded.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // --- ВИПРАВЛЕНО: КАРТКА ПОВЕРНЕННЯ КОШТІВ (ТИЛЬКИ ЯКЩО return_required) ---
                if (job.isReturnRequired && isStep2Done) {
                    val isStep3Active = job.serverStatus == "returning"
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(BorderStroke(2.dp, AppColors.Warning), RoundedCornerShape(24.dp)).shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AppColors.Warning.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(AppColors.Warning.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("КРОК 3: ПОВЕРНЕННЯ КОШТІВ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AppColors.Warning, letterSpacing = 1.sp)
                                    }
                                    StepTimer(
                                        startTimeIso = job.deliveredAt,
                                        endTimeIso = job.completedAt,
                                        isActive = isStep3Active
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Поверніть гроші в заклад", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                Spacer(modifier = Modifier.height(20.dp))
                                AddressItem(icon = Icons.Rounded.ArrowForward, text = job.partnerAddress)

                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.partnerAddress, null, null) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Warning),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Warning)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут назад", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ВИПРАВЛЕНО: КНОПКА ДІЇ ЗНИЗУ ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .shadow(24.dp, spotColor = Color.Black.copy(alpha = 0.15f))
                .padding(20.dp)
        ) {
            when (job.serverStatus) {
                "assigned" -> ModernButton("Я в закладі", { isActionLoading = true; onArrivedPickup(job.id) }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Primary, isLoading = isActionLoading)
                "arrived_pickup", "ready" -> ModernButton("Забрав замовлення", { isActionLoading = true; onUpdateStatus(job.id, "picked_up") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary, isLoading = isActionLoading)
                "picked_up" -> {
                    val btnText = if (job.isReturnRequired) "Забрав гроші (Везу в заклад)" else "Успішно доставлено"
                    ModernButton(btnText, { isActionLoading = true; onUpdateStatus(job.id, "delivered") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary, isLoading = isActionLoading)
                }
                "returning" -> ModernButton("Гроші віддав", { Toast.makeText(context, "Чекайте підтвердження від закладу. Заклад має натиснути кнопку у себе в кабінеті.", Toast.LENGTH_LONG).show() }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Warning, isLoading = false)
            }
        }
    }
}

// --- ПІД-ЕКРАН: ЧАТ ---
@Composable
fun ChatView(jobId: Int, cookie: String) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            messages = RetrofitClient.apiService.getChatMessages(cookie, jobId)
        } catch (e: Exception) {}
    }

    LaunchedEffect(Unit) {
        RetrofitClient.webSocketManager.messages.collect { messageJson ->
            try {
                val json = JSONObject(messageJson)
                if (json.optString("type") == "chat_message" && json.optInt("job_id") == jobId) {
                    val newMessage = ChatMessage(
                        role = json.optString("role"),
                        text = json.optString("text"),
                        time = json.optString("time")
                    )
                    messages = messages + newMessage
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isMe = msg.role == "courier"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .background(
                                if (isMe) AppColors.ChatBubbleSelf else AppColors.Surface,
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isMe) 20.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 20.dp
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(msg.text, fontSize = 16.sp, color = if (isMe) AppColors.ChatTextSelf else AppColors.TextPrimary, lineHeight = 22.sp)
                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = if (isMe) AppColors.ChatTextSelf.copy(alpha = 0.7f) else AppColors.TextSecondary,
                                modifier = Modifier.align(Alignment.End).padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = AppColors.Surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Повідомлення...", color = AppColors.TextSecondary) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        cursorColor = AppColors.Primary,
                        focusedContainerColor = AppColors.Background,
                        unfocusedContainerColor = AppColors.Background
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""

                            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val newMsg = ChatMessage(role = "courier", text = textToSend, time = currentTime)
                            messages = messages + newMsg

                            isSending = true
                            scope.launch {
                                try {
                                    listState.animateScrollToItem(0)
                                    RetrofitClient.apiService.sendChatMessage(cookie, jobId, textToSend, "courier")
                                    messages = RetrofitClient.apiService.getChatMessages(cookie, jobId)
                                } catch (e: Exception) {
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (inputText.isNotBlank()) AppColors.Primary else AppColors.Inactive, CircleShape)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = "Надіслати", tint = Color.White, modifier = Modifier.size(22.dp).padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ЕКРАН ІСТОРІЇ ЗАМОВЛЕНЬ (History)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryOrder>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Історія замовлень", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = AppColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (history.isEmpty() && !isLoading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Історія порожня", color = AppColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history) { order -> HistoryOrderCard(order) }
                }
            }
        }
    }
}

// ==========================================
// 5. ЕКРАН ПРОФІЛЮ (Profile Screen)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: CourierProfile?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Мій профіль", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = AppColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (profile != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Аватарка
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.linearGradient(listOf(AppColors.Primary, AppColors.Secondary)),
                                CircleShape
                            )
                            .shadow(8.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(profile.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
                    Text(profile.phone, fontSize = 16.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Карточка со статистикой (Комиссия, Рейтинг, Баланс)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // КОМИССИЯ
                            ProfileStatItem("Комісія", "${profile.commissionRate ?: 0.0}%", AppColors.Primary)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                            // РЕЙТИНГ
                            ProfileStatItem("Рейтинг", profile.rating?.toString() ?: "5.0", AppColors.Warning)
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                            // <--- ИЗМЕНЕНО: БАЛАНС
                            val balanceInt = profile.balance?.toInt() ?: 0
                            ProfileStatItem("Баланс", "$balanceInt ₴", AppColors.Secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Карточка с дополнительной информацией (Отзывы)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            AddressItem(icon = Icons.Outlined.Info, text = "Отримано відгуків: ${profile.ratingCount ?: 0}", label = "Статистика")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопка выхода
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, AppColors.Error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error)
                    ) {
                        Text("Вийти з акаунта", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                Text("Помилка завантаження профілю", modifier = Modifier.align(Alignment.Center), color = AppColors.Error)
            }
        }
    }
}

// Компонент для отображения элемента статистики (цифра + подпись)
@Composable
fun ProfileStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
    }
}

// Карточка истории заказов
@Composable
fun HistoryOrderCard(order: HistoryOrder) {
    val isDelivered = order.status == "delivered"

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Замовлення #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text(order.date, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))
            AddressItem(icon = Icons.Rounded.LocationOn, text = order.address)

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isDelivered) AppColors.Secondary.copy(alpha = 0.1f) else AppColors.Error.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isDelivered) "Виконано" else "Скасовано",
                        color = if (isDelivered) AppColors.Secondary else AppColors.Error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text("+${order.price} ₴", fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (isDelivered) AppColors.Secondary else AppColors.TextSecondary)
            }
        }
    }
}