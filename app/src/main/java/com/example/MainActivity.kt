package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.ui.theme.isAppInDarkTheme
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FeedPostItem
import com.example.model.HabitItem
import com.example.ui.AddHabitScreen
import com.example.ui.FeedScreen
import com.example.ui.SplashScreen
import com.example.ui.TrackerScreen
import com.example.ui.components.HabitImage
import com.example.ui.initialFeedPosts
import com.example.ui.initialHabitsList
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.BadgeOrange
import com.example.ui.theme.CardAccuracyDark
import com.example.ui.theme.CardAccuracyLight
import com.example.ui.theme.CardStreakDark
import com.example.ui.theme.CardStreakLight
import com.example.ui.theme.DarkIconBoxBg
import com.example.ui.theme.LightIconBoxBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NumberAccuracyDark
import com.example.ui.theme.NumberAccuracyLight
import com.example.ui.theme.NumberStreakDark
import com.example.ui.theme.NumberStreakLight
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SubtitleAccuracyDark
import com.example.ui.theme.SubtitleAccuracyLight
import com.example.ui.theme.SubtitleStreakDark
import com.example.ui.theme.SubtitleStreakLight
import com.example.ui.theme.TextGrayDark
import com.example.ui.theme.TextGrayLight

/**
 * MainActivity es el punto de entrada principal para esta aplicación Android.
 *
 * - **¿Para qué sirve?** Sirve como contenedor del ciclo de vida de la actividad e infla la interfaz Jetpack Compose.
 * - **¿Cómo funciona?** En [onCreate], habilita el modo Edge-to-Edge y carga la portada (SplashScreen) antes del dashboard.
 * - **¿Qué pasa si lo quitamos?** La aplicación fallaría al iniciar en el sistema operativo Android.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                HabitOctoMainApp(viewModel = viewModel)
            }
        }
    }
}

/**
 * HabitOctoMainApp es el contenedor principal de la aplicación.
 *
 * - **¿Para qué sirve?** Controla el Scaffold, la barra de navegación inferior (Tracker, Feed, Profile),
 *   y gestiona los estados compartidos de hábitos, micro hábitos, feed con imágenes y modo oscuro a través de [MainViewModel].
 * - **¿Cómo funciona?** Recibe [MainViewModel] y observa su `uiState` para coordinar el contenido de las pantallas y modales.
 * - **¿Qué pasa si lo quitamos?** Se perdería la estructura de navegación y la coordinación de pantallas.
 */
@Composable
fun HabitOctoMainApp(
    viewModel: MainViewModel = viewModel(),
    isDarkMode: Boolean? = null,
    onToggleDarkMode: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeDarkMode = isDarkMode ?: uiState.isDarkMode
    val toggleDarkModeAction = onToggleDarkMode ?: { viewModel.toggleDarkMode() }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                val selectedColor = MaterialTheme.colorScheme.onBackground
                val unselectedColor = if (activeDarkMode) TextGrayDark else TextGrayLight
                val indicatorBg = MaterialTheme.colorScheme.secondary

                // PESTAÑA 1: Tracker
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Adjust,
                            contentDescription = "Tracker"
                        )
                    },
                    label = {
                        Text(
                            text = "Tracker",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = indicatorBg,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                // PESTAÑA 2: Feed
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = "Feed"
                        )
                    },
                    label = {
                        Text(
                            text = "Feed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = indicatorBg,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                // PESTAÑA 3: Profile
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = indicatorBg,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    TrackerScreen(
                        habits = uiState.habits,
                        onToggleHabit = { habitToToggle ->
                            viewModel.toggleHabit(habitToToggle)
                        },
                        onOpenAddHabit = {
                            viewModel.openAddHabitDialog()
                        },
                        onEditHabit = { habit ->
                            viewModel.openEditHabitDialog(habit)
                        },
                        onDeleteHabit = { habitToDelete ->
                            viewModel.deleteHabit(habitToDelete)
                            Toast.makeText(context, "Hábito '${habitToDelete.title}' eliminado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                1 -> {
                    FeedScreen(
                        posts = uiState.feedPosts,
                        onToggleLike = { postId ->
                            viewModel.toggleLike(postId)
                        },
                        onPublishPost = { content, category, imageUrl ->
                            viewModel.publishPost(content, category, imageUrl)
                            Toast.makeText(context, "¡Publicado en la comunidad!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { TopNavigationBarSection() }
                        item { ProfileHeaderSection() }
                        item {
                            DarkModeToggleSection(
                                isDarkMode = activeDarkMode,
                                onToggleDarkMode = toggleDarkModeAction
                            )
                        }
                        item { PersonalStatsSection() }
                        item { AccountDataSection() }
                    }
                }
            }

            // Diálogo unificado para Añadir y Modificar Hábitos y Micro Hábitos
            if (uiState.isAddHabitDialogOpen) {
                AddHabitScreen(
                    habitToEdit = uiState.habitEditingTarget,
                    onDismiss = {
                        viewModel.closeAddHabitDialog()
                    },
                    onSaveHabit = { savedHabit ->
                        viewModel.saveHabit(savedHabit)
                    },
                    onDeleteHabit = { habitToDelete ->
                        viewModel.deleteHabit(habitToDelete)
                    }
                )
            }
        }
    }
}

/**
 * DarkModeToggleSection permite al usuario alternar entre Modo Claro y Modo Noche en tiempo real.
 */
@Composable
fun DarkModeToggleSection(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isDarkMode) DarkIconBoxBg else LightIconBoxBg,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = "Modo Noche",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Modo Noche / Oscuro",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkMode) "Activado (tema negro OLED)" else "Desactivado (tema claro)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) TextGrayDark else TextGrayLight
                    )
                }
            }

            Switch(
                checked = isDarkMode,
                onCheckedChange = { onToggleDarkMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

/**
 * TopNavigationBarSection renderiza la cabecera superior con marca y campana.
 */
@Composable
fun TopNavigationBarSection() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HabitImage(
                model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif",
                contentDescription = "App Logo Avatar",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "HabitOcto",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = {
                Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * ProfileHeaderSection dibuja el avatar, insignia de nivel y nombre del usuario.
 */
@Composable
fun ProfileHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            HabitImage(
                model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .background(BadgeOrange, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LVL 12",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Alex Rivero",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Buscador de Hábitos • 28 Días Activo",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isAppInDarkTheme) TextGrayDark else TextGrayLight
        )
    }
}

/**
 * PersonalStatsSection renderiza el tablero de métricas y gráficos de rendimiento mensual.
 */
@Composable
fun PersonalStatsSection() {
    val isDark = isAppInDarkTheme
    val textGray = if (isDark) TextGrayDark else TextGrayLight
    val iconBg = if (isDark) DarkIconBoxBg else LightIconBoxBg
    val cardStreakBg = if (isDark) CardStreakDark else CardStreakLight
    val numberStreakColor = if (isDark) NumberStreakDark else NumberStreakLight
    val subtitleStreakColor = if (isDark) SubtitleStreakDark else SubtitleStreakLight
    val cardAccuracyBg = if (isDark) CardAccuracyDark else CardAccuracyLight
    val numberAccuracyColor = if (isDark) NumberAccuracyDark else NumberAccuracyLight
    val subtitleAccuracyColor = if (isDark) SubtitleAccuracyDark else SubtitleAccuracyLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Estadísticas Personales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ESTE MES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textGray
            )
        }

        // TARJETA 1: Consistencia
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Consistencia",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = textGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "87%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color = iconBg, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Adjust,
                            contentDescription = "Focus Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(35.dp, 55.dp, 40.dp, 75.dp, 60.dp, 70.dp, 80.dp)
                    heights.forEachIndexed { index, h ->
                        val isHighest = index == heights.size - 1
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(h)
                                .background(
                                    color = if (isHighest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }

        // Racha y Precisión
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardStreakBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RACHA ACTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = subtitleStreakColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "12 Días",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = numberStreakColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "¡Récord personal!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = subtitleStreakColor
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardAccuracyBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PRECISIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = subtitleAccuracyColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "94%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = numberAccuracyColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Integridad de Hábito",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = subtitleAccuracyColor
                    )
                }
            }
        }
    }
}

/**
 * AccountDataSection renderiza el panel de configuraciones y datos de la cuenta.
 */
@Composable
fun AccountDataSection() {
    val context = LocalContext.current
    var isTwoFactorEnabled by remember { mutableStateOf(true) }
    val isDark = isAppInDarkTheme
    val textGray = if (isDark) TextGrayDark else TextGrayLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Datos de la Cuenta",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Email
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(context, "Cambiar correo electrónico", Toast.LENGTH_SHORT).show()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Email",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DIRECCIÓN DE CORREO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "alex.r****@gmail.com",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Cambiar",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Password
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(context, "Actualizar contraseña", Toast.LENGTH_SHORT).show()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Password",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CONTRASEÑA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "••••••••••••",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Actualizar",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Two Factor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Two-Factor Auth",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VERIFICACIÓN EN DOS PASOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isTwoFactorEnabled) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isTwoFactorEnabled) MaterialTheme.colorScheme.primary else textGray
                    )
                }

                Switch(
                    checked = isTwoFactorEnabled,
                    onCheckedChange = { isTwoFactorEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.secondary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.background,
                        uncheckedBorderColor = textGray
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitOctoProfilePreview() {
    MyApplicationTheme(darkTheme = false) {
        HabitOctoMainApp(isDarkMode = false, onToggleDarkMode = {})
    }
}

@Preview(showBackground = true)
@Composable
fun HabitOctoProfileDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        HabitOctoMainApp(isDarkMode = true, onToggleDarkMode = {})
    }
}
