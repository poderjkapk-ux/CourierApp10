package com.restify.courierapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Ваш робочий інструмент",
        description = "Вітаємо в Restify Courier! Тут ви отримуєте нові замовлення, бачите свій заробіток та керуєте всіма етапами доставки в єдиному зручному вікні.",
        icon = Icons.Rounded.LocationOn,
        iconTint = Color(0xFF8A2BE2) // Фіолетовий
    ),
    OnboardingPage(
        title = "Деталі та Оплата",
        description = "Уважно перевіряйте інформацію перед прийняттям заявки. Звертайте особливу увагу на тип розрахунку: чи це звичайна передплата, чи вам потрібно викупити замовлення в закладі власними коштами.",
        icon = Icons.Rounded.List,
        iconTint = Color(0xFF00C853) // Зелений
    ),
    OnboardingPage(
        title = "Керування статусами",
        description = "Ваша ефективність залежить від своєчасного оновлення статусів. Відзначайте своє прибуття до закладу, момент отримання страв та факт передачі замовлення клієнту в один дотик.",
        icon = Icons.Rounded.CheckCircle,
        iconTint = Color(0xFF2979FF) // Синій
    ),
    OnboardingPage(
        title = "Зв'язок із закладом",
        description = "Виникли питання щодо замовлення чи затримуєтесь в дорозі? Використовуйте зручний внутрішній чат для швидкого зв'язку з адміністратором ресторану без переходу в інші додатки.",
        icon = Icons.Rounded.Email,
        iconTint = Color(0xFFFF8F00) // Помаранчевий
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomSection(
                size = onboardingPages.size,
                index = pagerState.currentPage,
                onNextClicked = {
                    if (pagerState.currentPage < onboardingPages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { position ->
                PagerScreen(onboardingPage = onboardingPages[position])
            }
        }
    }
}

@Composable
fun PagerScreen(onboardingPage: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(onboardingPage.iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(onboardingPage.iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = onboardingPage.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = onboardingPage.iconTint
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = onboardingPage.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = onboardingPage.description,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BottomSection(
    size: Int,
    index: Int,
    onNextClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(size) { indicatorIndex ->
                val isSelected = index == indicatorIndex
                val width by animateDpAsState(
                    targetValue = if (isSelected) 28.dp else 10.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "indicator_anim"
                )
                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(10.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNextClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            AnimatedContent(
                targetState = if (index == size - 1) "Розпочати зміну" else "Далі",
                transitionSpec = {
                    if (targetState == "Розпочати зміну") {
                        slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() with
                                slideOutVertically { height -> height } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "button_text_anim"
            ) { targetText ->
                Text(
                    text = targetText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}