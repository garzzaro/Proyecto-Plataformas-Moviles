package com.example



import android.os.Bundle

import android.widget.Toast

import androidx.activity.ComponentActivity

import androidx.activity.compose.BackHandler

import androidx.activity.compose.setContent

import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.CameraAlt

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.Pets

import androidx.compose.material.icons.outlined.Adjust

import androidx.compose.material.icons.outlined.DarkMode

import androidx.compose.material.icons.outlined.Email

import androidx.compose.material.icons.outlined.Group

import androidx.compose.material.icons.outlined.Language

import androidx.compose.material.icons.outlined.LightMode

import androidx.compose.material.icons.outlined.Lock

import androidx.compose.material.icons.outlined.Notifications

import androidx.compose.material.icons.outlined.Shield

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.NavigationBarItemDefaults

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Surface

import androidx.compose.material3.Switch

import androidx.compose.material3.SwitchDefaults

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.DisposableEffect

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.compositionLocalOf

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.saveable.rememberSaveableStateHolder

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.Lifecycle

import androidx.lifecycle.LifecycleEventObserver

import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.model.AchievementCatalog

import com.example.model.AppLanguage

import com.example.model.AppStrings

import com.example.model.AvatarCatalog

import com.example.model.CloudSyncManager

import com.example.model.HabitDates

import com.example.model.HabitItem

import com.example.model.LocalAppLanguage

import com.example.model.PetAccessory

import com.example.model.PetCustomization

import com.example.model.PetType

import com.example.model.RankingUser

import com.example.model.TrackingSummary

import com.example.model.UserEntity

import com.example.ui.AchievementsShowcaseCard

import com.example.ui.AddHabitScreen

import com.example.ui.AuthModalDialog

import com.example.ui.FeedScreen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat

import androidx.compose.material.icons.filled.NotificationsActive

import com.example.model.HabitNotificationScheduler

import com.example.ui.NotificationGenerator

import com.example.ui.NotificationsCenterDialog

import com.example.ui.PetAndRankingScreen

import com.example.ui.ProfileCloudTagCard

import com.example.ui.ProfileEditDialog

import com.example.ui.TrackerScreen

import com.example.ui.components.HabitImage

import com.example.ui.components.UserAvatarHeaderChip

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

import com.example.ui.theme.isAppInDarkTheme

import com.example.viewmodel.MainViewModel



/**

 * Estado compartido para que el icono de campana de `TopNavigationBarSection` abra siempre

 * el Centro de Notificaciones conectado al estado global desde cualquier pestaña.

 */

data class NotificationsHeaderController(

    val unreadCount: Int = 0,

    val onOpenNotifications: (() -> Unit)? = null

)



val LocalNotificationsHeaderController = compositionLocalOf { NotificationsHeaderController() }



/**

 * MainActivity es el punto de entrada principal para esta aplicación Android.

 *

 * - **¿Para qué sirve?** Sirve como contenedor del ciclo de vida de la actividad e infla la interfaz Jetpack Compose.

 * - **¿Cómo funciona?** En [onCreate], habilita el modo Edge-to-Edge y conecta [MainViewModel] con [HabitOctoMainApp].

 * - **¿Qué pasa si lo quitamos?** La aplicación fallaría al iniciar en el sistema operativo Android.

 */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(this@MainActivity))

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

 * - **¿Para qué sirve?** Controla el Scaffold, la barra de navegación inferior (Tracker, Feed, Mascota, Profile),

 *   y gestiona los estados compartidos de hábitos, micro hábitos, feed con imágenes, mascota, ranking social,

 *   centro de notificaciones, idioma bilingüe (ES/EN) y sesión persistente a través de [MainViewModel] y [CloudSyncManager].

 * - **¿Cómo funciona?** Recibe [MainViewModel] y observa su `uiState` para coordinar el contenido de las 4 pestañas y modales.

 * - **¿Qué pasa si lo quitamos?** Se perdería la estructura de navegación y la coordinación de pantallas.

 */

@Composable

fun HabitOctoMainApp(

    viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(LocalContext.current.applicationContext)),

    isDarkMode: Boolean? = null,

    onToggleDarkMode: (() -> Unit)? = null

) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activeDarkMode = isDarkMode ?: uiState.isDarkMode

    val toggleDarkModeAction = onToggleDarkMode ?: { viewModel.toggleDarkMode() }

    val appLanguage = AppLanguage.fromCode(uiState.languageCode)

    val isEnglish = appLanguage.isEnglish

    val strings = AppStrings.get(isEnglish)

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val syncManager = remember { CloudSyncManager.getInstance(context) }

    // Launcher de permisos de notificaciones para Android 13+ y programador periódico de recordatorios

    val notificationPermissionLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.RequestPermission()

    ) { isGranted ->

        if (isGranted) {

            HabitNotificationScheduler.startPeriodicReminders(context = context, isEnglish = isEnglish)

        }

    }



    LaunchedEffect(isEnglish) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            } else {

                HabitNotificationScheduler.startPeriodicReminders(context = context, isEnglish = isEnglish)

            }

        } else {

            HabitNotificationScheduler.startPeriodicReminders(context = context, isEnglish = isEnglish)

        }

    }



    val activeSession by syncManager.currentSession.collectAsStateWithLifecycle()



    // Conserva desplazamiento y borradores de cada pestaña durante la navegación.

    val tabState = rememberSaveableStateHolder()

    var deleteTarget by remember { mutableStateOf<HabitItem?>(null) }

    var editProfile by rememberSaveable { mutableStateOf(false) }

    var showAuthModal by rememberSaveable { mutableStateOf(false) }

    var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }

    var readNotificationIds by rememberSaveable { mutableStateOf(emptyList<String>()) }



    val generatedNotifications = remember(

        uiState.habits,

        uiState.summary,

        uiState.friends.size,

        uiState.feedPosts.size,

        isEnglish,

        readNotificationIds

    ) {

        NotificationGenerator.buildNotifications(

            habits = uiState.habits,

            summary = uiState.summary,

            friendsCount = uiState.friends.size,

            postsCount = uiState.feedPosts.size,

            isEnglish = isEnglish

        ).map { item ->

            if (item.id in readNotificationIds) item.copy(isUnread = false) else item

        }

    }

    val unreadNotificationsCount = generatedNotifications.count { it.isUnread }



    // Sincronización automática con CloudSyncManager cuando cambian uiState.user o uiState.summary

    LaunchedEffect(uiState.user, uiState.summary.totalCompleted, uiState.summary.microStreak) {

        val currentUser = uiState.user ?: return@LaunchedEffect

        val customization = PetCustomization.fromUser(currentUser)

        val sessionTag = syncManager.currentSession.value?.normalizedTag

        val userTag = currentUser.friendCode.takeIf { it.isNotBlank() && it != "000" }

        val resolvedTag = userTag

            ?: sessionTag

            ?: CloudSyncManager.generateDeterministicTag(currentUser.displayName, currentUser.email.orEmpty())



        val currentSyncSession = syncManager.currentSession.value

        if (currentSyncSession == null ||

            currentSyncSession.normalizedTag != resolvedTag ||

            currentSyncSession.displayName != currentUser.displayName

        ) {

            syncManager.loginOrRegister(

                name = currentUser.displayName,

                email = currentUser.email.orEmpty(),

                preferredTag = resolvedTag,

                completedHabits = uiState.summary.totalCompleted,

                currentStreak = uiState.summary.microStreak,

                petCustomization = customization

            )

        } else {

            syncManager.publishCurrentUserStats(

                completedHabits = uiState.summary.totalCompleted,

                currentStreak = uiState.summary.microStreak,

                petCustomization = customization,

                userNameOverride = currentUser.displayName

            )

        }

    }



    // Al volver a la app, recupera cambios persistidos y comprueba si cambió la fecha.

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()

        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }

    }

    // Volver desde Feed, Mascota o Perfil regresa a Tracker; los diálogos conservan su propio cierre.

    BackHandler(enabled = uiState.selectedTab != 0 && !uiState.isAddHabitDialogOpen) { viewModel.selectTab(0) }



    val userLevel = 1 + uiState.summary.totalCompleted / 10

    val notificationsController = remember(unreadNotificationsCount) {

        NotificationsHeaderController(

            unreadCount = unreadNotificationsCount,

            onOpenNotifications = { showNotificationsDialog = true }

        )

    }



    CompositionLocalProvider(

        LocalAppLanguage provides appLanguage,

        LocalNotificationsHeaderController provides notificationsController

    ) {

        Scaffold(

            containerColor = MaterialTheme.colorScheme.background,

            bottomBar = {

                NavigationBar(

                    containerColor = MaterialTheme.colorScheme.background,

                    contentColor = MaterialTheme.colorScheme.primary,

                    tonalElevation = 2.dp

                ) {

                    val selectedColor = MaterialTheme.colorScheme.onBackground

                    val unselectedColor = if (activeDarkMode) TextGrayDark else TextGrayLight

                    val indicatorBg = MaterialTheme.colorScheme.secondary



                    // PESTAÑA 1: Tracker (index 0)

                    NavigationBarItem(

                        selected = uiState.selectedTab == 0,

                        onClick = { viewModel.selectTab(0) },

                        icon = {

                            Icon(

                                imageVector = Icons.Outlined.Adjust,

                                contentDescription = strings.tabTracker

                            )

                        },

                        label = {

                            Text(

                                text = strings.tabTracker,

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



                    // PESTAÑA 2: Feed (index 1)

                    NavigationBarItem(

                        selected = uiState.selectedTab == 1,

                        onClick = { viewModel.selectTab(1) },

                        icon = {

                            Icon(

                                imageVector = Icons.Outlined.Group,

                                contentDescription = strings.tabFeed

                            )

                        },

                        label = {

                            Text(

                                text = strings.tabFeed,

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



                    // PESTAÑA 3: Mascota y Ranking (index 2)

                    NavigationBarItem(

                        selected = uiState.selectedTab == 2,

                        onClick = { viewModel.selectTab(2) },

                        icon = {

                            Icon(

                                imageVector = Icons.Default.Pets,

                                contentDescription = strings.tabPet

                            )

                        },

                        label = {

                            Text(

                                text = strings.tabPet,

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



                    // PESTAÑA 4: Profile (index 3)

                    NavigationBarItem(

                        selected = uiState.selectedTab == 3,

                        onClick = { viewModel.selectTab(3) },

                        icon = {

                            Icon(

                                imageVector = Icons.Default.Person,

                                contentDescription = strings.tabProfile

                            )

                        },

                        label = {

                            Text(

                                text = strings.tabProfile,

                                style = MaterialTheme.typography.labelSmall,

                                fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Medium

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

                if (uiState.isLoading) {

                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {

                        CircularProgressIndicator()

                        Text(strings.loadingData, modifier = Modifier.padding(16.dp))

                        TextButton(onClick = viewModel::refresh) { Text(strings.retryAction) }

                    }

                } else tabState.SaveableStateProvider(uiState.selectedTab) {

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

                                    deleteTarget = habitToDelete

                                },

                                history = uiState.history,

                                summary = uiState.summary,

                                onSetProgress = viewModel::setHabitProgress,

                                onNavigateToPet = { viewModel.selectTab(2) },

                                petCustomization = PetCustomization.fromUser(uiState.user),

                                onUpdatePetCustomization = { updated ->

                                    viewModel.updatePetCustomization(

                                        petType = updated.petType.id,

                                        petName = updated.petName,

                                        accessory = updated.accessory.id,

                                        habitat = updated.habitat.id

                                    )

                                },

                                user = uiState.user,

                                onProfileClick = { viewModel.selectTab(3) },

                                isEnglish = isEnglish,

                                requirePhotoForMicroHabits = true

                            )

                        }

                        1 -> {

                            FeedScreen(

                                posts = uiState.feedPosts,

                                onToggleLike = { postId ->

                                    viewModel.toggleLike(postId)

                                },

                                publicationVersion = uiState.publicationVersion,

                                isSaving = uiState.isSaving,

                                onPublishPost = { content, category, imageUrl ->

                                    viewModel.publishPost(content, category, imageUrl)

                                },

                                user = uiState.user,

                                level = userLevel,

                                onProfileClick = { viewModel.selectTab(3) },

                                onDeleteOwnPost = viewModel::deleteOwnPost,

                                onAddComment = viewModel::addComment,

                                isEnglish = isEnglish

                            )

                        }

                        2 -> {

                            val roomRankingFriends = remember(uiState.friends) {

                                uiState.friends.mapIndexed { index, friend ->

                                    val pet = PetType.fromId(friend.petType)

                                    val acc = PetAccessory.fromId(friend.petAccessory)

                                    RankingUser(

                                        id = "room_${friend.friendCode}",

                                        name = if (friend.displayName.contains("#${friend.friendCode}")) friend.displayName else "${friend.displayName} #${friend.friendCode}",

                                        avatarUrl = friend.avatarUrl,

                                        score = friend.score,

                                        completedHabits = friend.completedHabits,

                                        currentStreak = friend.currentStreak,

                                        rankPosition = index + 1,

                                        isCurrentUser = false,

                                        badge = buildString {

                                            append(pet.iconEmoji)

                                            if (acc != PetAccessory.NONE) append(acc.emoji)

                                        },

                                        isFriend = true,

                                        friendCode = friend.friendCode,

                                        petType = pet,

                                        petAccessory = acc

                                    )

                                }

                            }



                            Column(modifier = Modifier.fillMaxSize()) {

                                TopNavigationBarSection(

                                    user = uiState.user,

                                    level = userLevel,

                                    onProfileClick = { viewModel.selectTab(3) },

                                    onNotificationsClick = { showNotificationsDialog = true },

                                    unreadNotificationsCount = unreadNotificationsCount

                                )

                                PetAndRankingScreen(

                                    currentStreak = uiState.summary.microStreak,

                                    totalCompleted = uiState.summary.totalCompleted,

                                    userName = uiState.user?.displayName ?: "Suchini",

                                    userFriendCode = uiState.user?.friendCode?.takeIf { it.isNotBlank() && it != "000" }

                                        ?: activeSession?.normalizedTag

                                        ?: "777",

                                    completedToday = uiState.summary.completedToday,

                                    scheduledToday = maxOf(1, uiState.summary.scheduledToday),

                                    initialCustomization = PetCustomization.fromUser(uiState.user),

                                    externalFriends = roomRankingFriends,

                                    onUpdateCustomization = { updated ->

                                        viewModel.updatePetCustomization(

                                            petType = updated.petType.id,

                                            petName = updated.petName,

                                            accessory = updated.accessory.id,

                                            habitat = updated.habitat.id

                                        )

                                    },

                                    onAddFriendByCode = { code, name, streak, habits ->

                                        viewModel.addFriendByCode(

                                            code = code,

                                            name = name,

                                            habits = habits,

                                            streak = streak

                                        )

                                    }

                                )

                            }

                        }

                        3 -> {

                            val profileAchievements = remember(

                                uiState.summary.totalCompleted,

                                uiState.summary.microStreak,

                                uiState.friends.size

                            ) {

                                AchievementCatalog.evaluate(

                                    totalCompleted = uiState.summary.totalCompleted,

                                    currentStreak = uiState.summary.microStreak,

                                    friendsCount = maxOf(1, uiState.friends.size),

                                    petLevel = userLevel

                                )

                            }



                            LazyColumn(

                                modifier = Modifier

                                    .fillMaxSize()

                                    .background(MaterialTheme.colorScheme.background)

                                    .padding(bottom = 16.dp),

                                verticalArrangement = Arrangement.spacedBy(16.dp)

                            ) {

                                item {

                                    TopNavigationBarSection(

                                        user = uiState.user,

                                        level = userLevel,

                                        onProfileClick = { viewModel.selectTab(3) },

                                        onNotificationsClick = { showNotificationsDialog = true },

                                        unreadNotificationsCount = unreadNotificationsCount

                                    )

                                }

                                item {

                                    ProfileHeaderSection(

                                        user = uiState.user,

                                        summary = uiState.summary,

                                        onEdit = { editProfile = true },

                                        onOpenAuth = { showAuthModal = true }

                                    )

                                }

                                item {

                                    ProfileCloudTagCard(

                                        userName = uiState.user?.displayName ?: "Suchini",

                                        userEmail = uiState.user?.email,

                                        completedHabits = uiState.summary.totalCompleted,

                                        currentStreak = uiState.summary.microStreak,

                                        onOpenAuthModal = { showAuthModal = true }

                                    )

                                }

                                item {

                                    DarkModeToggleSection(

                                        isDarkMode = activeDarkMode,

                                        onToggleDarkMode = toggleDarkModeAction

                                    )

                                }

                                item {

                                    LanguageToggleSection(

                                        isEnglish = isEnglish,

                                        onToggleLanguage = { viewModel.toggleLanguage() }

                                    )

                                }

                                item {

                                    Card(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .padding(horizontal = 20.dp),

                                        shape = RoundedCornerShape(16.dp),

                                        colors = CardDefaults.cardColors(

                                            containerColor = if (activeDarkMode) Color(0xFF1E293B) else Color.White

                                        ),

                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

                                    ) {

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .clickable {

                                                    HabitNotificationScheduler.sendReminder(context = context, isEnglish = isEnglish)

                                                    Toast.makeText(

                                                        context,

                                                        if (isEnglish) "Reminder notification sent to tray!" else "¡Notificación de recordatorio enviada a la barra!",

                                                        Toast.LENGTH_SHORT

                                                    ).show()

                                                }

                                                .padding(16.dp),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                Icon(

                                                    imageVector = Icons.Default.NotificationsActive,

                                                    contentDescription = null,

                                                    tint = PrimaryTeal,

                                                    modifier = Modifier.size(24.dp)

                                                )

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {

                                                    Text(

                                                        text = if (isEnglish) "Notification Reminders" else "Recordatorios de Hábitos",

                                                        fontWeight = FontWeight.Bold,

                                                        fontSize = 14.sp,

                                                        color = if (activeDarkMode) Color.White else Color(0xFF0F172A)

                                                    )

                                                    Text(

                                                        text = if (isEnglish) "Active periodic alerts • Tap to test" else "Recordatorios periódicos activos • Toca para probar",

                                                        fontSize = 12.sp,

                                                        color = PrimaryTeal

                                                    )

                                                }

                                            }

                                            Text(text = "🔔", fontSize = 18.sp)

                                        }

                                    }

                                }

                                item { PersonalStatsSection(uiState.summary) }

                                item {

                                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {

                                        AchievementsShowcaseCard(

                                            achievements = profileAchievements,

                                            isDark = activeDarkMode

                                        )

                                    }

                                }

                                item {

                                    AccountDataSection(

                                        user = uiState.user,

                                        onEdit = { editProfile = true },

                                        onOpenAuth = { showAuthModal = true },

                                        onLogout = {

                                            syncManager.logout()

                                            viewModel.logoutUser()

                                            Toast.makeText(

                                                context,

                                                if (isEnglish) "Logged out. Your local data remains saved." else "Sesión cerrada. Tus datos locales siguen guardados.",

                                                Toast.LENGTH_SHORT

                                            ).show()

                                        }

                                    )

                                }

                            }

                        }

                    }

                }

                if (uiState.isSaving && !uiState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))



                // Diálogo unificado para Añadir y Modificar Hábitos y Micro Hábitos

                if (uiState.isAddHabitDialogOpen) {

                    AddHabitScreen(

                        habitToEdit = uiState.habitEditingTarget,

                        isSaving = uiState.isSaving,

                        onDismiss = {

                            viewModel.closeAddHabitDialog()

                        },

                        onSaveHabit = { savedHabit ->

                            viewModel.saveHabit(savedHabit)

                        },

                        onDeleteHabit = { habitToDelete ->

                            deleteTarget = habitToDelete

                        }

                    )

                }

            }

        }



        // Centro de Notificaciones Inteligentes

        if (showNotificationsDialog) {

            NotificationsCenterDialog(

                notifications = generatedNotifications,

                isEnglish = isEnglish,

                onDismiss = { showNotificationsDialog = false },

                onSelectNotification = { notif ->

                    if (notif.id !in readNotificationIds) {

                        readNotificationIds = readNotificationIds + notif.id

                    }

                    viewModel.selectTab(notif.targetTab)

                },

                onMarkAllRead = {

                    readNotificationIds = generatedNotifications.map { it.id }

                }

            )

        }



        // Se confirma el archivo del hábito sin borrar sus registros históricos.

        deleteTarget?.let { habit ->

            AlertDialog(

                onDismissRequest = { deleteTarget = null },

                title = { Text(if (isEnglish) "Delete ${habit.title}?" else "¿Eliminar ${habit.title}?") },

                text = {

                    Text(

                        if (isEnglish) {

                            "It will be removed from your active habits. Its completion history will be preserved."

                        } else {

                            "Se quitará de tus hábitos activos. Su historial de cumplimiento se conservará."

                        }

                    )

                },

                confirmButton = {

                    TextButton(onClick = { viewModel.deleteHabit(habit); deleteTarget = null }) {

                        Text(if (isEnglish) "Delete" else "Eliminar")

                    }

                },

                dismissButton = {

                    TextButton(onClick = { deleteTarget = null }) {

                        Text(strings.cancelAction)

                    }

                }

            )

        }



        if (editProfile) {

            ProfileEditDialog(

                user = uiState.user,

                isSaving = uiState.isSaving,

                isEnglish = isEnglish,

                onDismiss = { editProfile = false },

                onSave = { name, email ->

                    viewModel.updateProfile(name, email) { editProfile = false }

                },

                onSaveWithAvatar = { name, email, avatarUrl ->

                    viewModel.updateProfileWithAvatar(name, email, avatarUrl) { editProfile = false }

                }

            )

        }



        // Modal de Inicio de Sesión / Registro / Personalización de #Código de 3 dígitos

        if (showAuthModal) {

            AuthModalDialog(

                onDismiss = { showAuthModal = false },

                onAuthSuccess = { session ->

                    viewModel.loginOrRegisterUser(

                        name = session.displayName,

                        email = session.email,

                        friendCode = session.normalizedTag

                    ) {

                        showAuthModal = false

                    }

                },

                initialName = uiState.user?.displayName.orEmpty(),

                initialEmail = uiState.user?.email.orEmpty(),

                initialTag = uiState.user?.friendCode?.takeIf { it != "000" }.orEmpty(),

                completedHabits = uiState.summary.totalCompleted,

                currentStreak = uiState.summary.microStreak

            )

        }



        // Los errores se muestran sobre los formularios para no ocultar un guardado fallido.

        if (uiState.error != null) {

            AlertDialog(

                onDismissRequest = viewModel::clearError,

                title = { Text(if (isEnglish) "Operation could not be completed" else "No se pudo completar la operación") },

                text = { Text(uiState.error.orEmpty()) },

                confirmButton = {

                    TextButton(onClick = { viewModel.clearError(); viewModel.refresh() }) {

                        Text(strings.retryAction)

                    }

                },

                dismissButton = {

                    TextButton(onClick = viewModel::clearError) {

                        Text(strings.closeLabel)

                    }

                }

            )

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

    val strings = AppStrings.current()

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

                horizontalArrangement = Arrangement.spacedBy(14.dp),

                modifier = Modifier.weight(1f)

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

                        contentDescription = strings.darkModeTitle,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(22.dp)

                    )

                }



                Column {

                    Text(

                        text = strings.darkModeTitle,

                        style = MaterialTheme.typography.bodyLarge,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurface

                    )

                    Text(

                        text = if (isDarkMode) strings.darkModeOnSubtitle else strings.darkModeOffSubtitle,

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

 * LanguageToggleSection permite alternar el idioma de toda la aplicación entre Español (🇪🇸) e Inglés (🇺🇸) en vivo.

 */

@Composable

fun LanguageToggleSection(

    isEnglish: Boolean,

    onToggleLanguage: () -> Unit

) {

    val isDark = isAppInDarkTheme

    val strings = AppStrings.get(isEnglish)



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

                horizontalArrangement = Arrangement.spacedBy(14.dp),

                modifier = Modifier.weight(1f)

            ) {

                Box(

                    modifier = Modifier

                        .size(40.dp)

                        .background(

                            if (isDark) DarkIconBoxBg else LightIconBoxBg,

                            CircleShape

                        ),

                    contentAlignment = Alignment.Center

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Language,

                        contentDescription = strings.appLanguageTitle,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(22.dp)

                    )

                }



                Column {

                    Text(

                        text = strings.appLanguageTitle,

                        style = MaterialTheme.typography.bodyLarge,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurface

                    )

                    Text(

                        text = strings.appLanguageSubtitle,

                        style = MaterialTheme.typography.bodySmall,

                        color = if (isDark) TextGrayDark else TextGrayLight

                    )

                }

            }



            Row(

                horizontalArrangement = Arrangement.spacedBy(6.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Surface(

                    shape = RoundedCornerShape(12.dp),

                    color = if (!isEnglish) PrimaryTeal else MaterialTheme.colorScheme.background,

                    modifier = Modifier

                        .clip(RoundedCornerShape(12.dp))

                        .clickable { if (isEnglish) onToggleLanguage() }

                ) {

                    Text(

                        text = "🇪🇸 ES",

                        color = if (!isEnglish) Color.White else MaterialTheme.colorScheme.onSurface,

                        fontWeight = FontWeight.Bold,

                        fontSize = 12.sp,

                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                    )

                }



                Surface(

                    shape = RoundedCornerShape(12.dp),

                    color = if (isEnglish) PrimaryTeal else MaterialTheme.colorScheme.background,

                    modifier = Modifier

                        .clip(RoundedCornerShape(12.dp))

                        .clickable { if (!isEnglish) onToggleLanguage() }

                ) {

                    Text(

                        text = "🇺🇸 EN",

                        color = if (isEnglish) Color.White else MaterialTheme.colorScheme.onSurface,

                        fontWeight = FontWeight.Bold,

                        fontSize = 12.sp,

                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                    )

                }

            }

        }

    }

}



/**

 * TopNavigationBarSection renderiza la cabecera superior con el logo oficial de HabitOcto (`R.drawable.ic_pulpo`),

 * el chip de Avatar con Nivel + `#Código` de 3 dígitos (que al tocarlo lleva a `Profile`) y el botón interactivo

 * del Centro de Notificaciones con insignia de no leídas.

 */

@Composable

fun TopNavigationBarSection(

    user: UserEntity? = null,

    level: Int? = null,

    onProfileClick: (() -> Unit)? = null,

    onNotificationsClick: (() -> Unit)? = null,

    unreadNotificationsCount: Int? = null

) {

    val context = LocalContext.current

    val isEnglish = LocalAppLanguage.current.isEnglish

    val headerController = LocalNotificationsHeaderController.current

    var showFallbackDialog by rememberSaveable { mutableStateOf(false) }

    var fallbackReadIds by rememberSaveable { mutableStateOf(emptyList<String>()) }



    val fallbackNotifications = remember(isEnglish, fallbackReadIds) {

        NotificationGenerator.buildNotifications(

            habits = emptyList(),

            summary = TrackingSummary(),

            friendsCount = 4,

            postsCount = 4,

            isEnglish = isEnglish

        ).map { item ->

            if (item.id in fallbackReadIds) item.copy(isUnread = false) else item

        }

    }



    val effectiveUnreadCount = unreadNotificationsCount

        ?: if (headerController.onOpenNotifications != null) headerController.unreadCount

        else fallbackNotifications.count { it.isUnread }



    val syncSession = remember(context) {

        runCatching { CloudSyncManager.getInstance(context).getActiveOrLastSession() }.getOrNull()

    }

    val resolvedUser = remember(user, syncSession) {

        when {

            user != null && (user.friendCode.isBlank() || user.friendCode == "000") && syncSession != null -> {

                user.copy(friendCode = syncSession.normalizedTag)

            }

            user != null -> user

            syncSession != null -> UserEntity(

                id = syncSession.userId,

                displayName = syncSession.displayName,

                email = syncSession.email,

                avatarUrl = syncSession.avatarUrl,

                createdDay = 0L,

                friendCode = syncSession.normalizedTag,

                isLoggedIn = syncSession.isLoggedIn

            )

            else -> null

        }

    }

    val activeLevel = level ?: 1

    val navigateToProfile: () -> Unit = onProfileClick ?: {}



    if (showFallbackDialog) {

        NotificationsCenterDialog(

            notifications = fallbackNotifications,

            isEnglish = isEnglish,

            onDismiss = { showFallbackDialog = false },

            onSelectNotification = { notif ->

                if (notif.id !in fallbackReadIds) {

                    fallbackReadIds = fallbackReadIds + notif.id

                }

                if (notif.targetTab == 3) navigateToProfile()

            },

            onMarkAllRead = {

                fallbackReadIds = fallbackNotifications.map { it.id }

            }

        )

    }



    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(

                top = 8.dp,

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

                model = AvatarCatalog.appLogo(),

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



        Row(

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            UserAvatarHeaderChip(

                user = resolvedUser,

                level = activeLevel,

                onClick = navigateToProfile

            )



            Box(contentAlignment = Alignment.TopEnd) {

                IconButton(

                    onClick = {

                        when {

                            onNotificationsClick != null -> onNotificationsClick()

                            headerController.onOpenNotifications != null -> headerController.onOpenNotifications.invoke()

                            else -> showFallbackDialog = true

                        }

                    }

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Notifications,

                        contentDescription = "Notifications",

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(26.dp)

                    )

                }



                if (effectiveUnreadCount > 0) {

                    Box(

                        modifier = Modifier

                            .offset(x = (-6).dp, y = 6.dp)

                            .size(18.dp)

                            .background(BadgeOrange, CircleShape)

                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),

                        contentAlignment = Alignment.Center

                    ) {

                        Text(

                            text = effectiveUnreadCount.coerceAtMost(9).toString(),

                            color = Color.White,

                            fontSize = 9.sp,

                            fontWeight = FontWeight.ExtraBold

                        )

                    }

                }

            }

        }

    }

}



/**

 * ProfileHeaderSection dibuja el avatar clicable, insignia de nivel, código `#XXX` y botones de edición de foto/perfil.

 */

@Composable

fun ProfileHeaderSection(

    user: UserEntity? = null,

    summary: TrackingSummary = TrackingSummary(),

    onEdit: () -> Unit = {},

    onOpenAuth: (() -> Unit)? = null

) {

    val strings = AppStrings.current()

    val friendTag = user?.friendCode?.takeIf { it.isNotBlank() && it != "000" } ?: "777"

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(top = 12.dp, bottom = 8.dp),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Box(

            contentAlignment = Alignment.BottomCenter,

            modifier = Modifier

                .padding(bottom = 6.dp)

                .clickable(onClick = onEdit)

        ) {

            HabitImage(

                model = user?.avatarUrl ?: R.drawable.ic_pulpo,

                contentDescription = "Profile Picture",

                modifier = Modifier

                    .size(100.dp)

                    .clip(CircleShape)

                    .border(2.5.dp, PrimaryTeal, CircleShape),

                contentScale = ContentScale.Crop

            )



            Box(

                modifier = Modifier

                    .background(BadgeOrange, RoundedCornerShape(12.dp))

                    .padding(horizontal = 8.dp, vertical = 2.dp)

            ) {

                Text(

                    text = "LVL ${1 + summary.totalCompleted / 10}",

                    color = Color.White,

                    style = MaterialTheme.typography.labelSmall,

                    fontWeight = FontWeight.Bold

                )

            }



            Box(

                modifier = Modifier

                    .align(Alignment.BottomEnd)

                    .offset(x = 4.dp, y = (-4).dp)

                    .size(30.dp)

                    .background(SecondaryTeal, CircleShape)

                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    imageVector = Icons.Default.CameraAlt,

                    contentDescription = strings.changeProfilePhoto,

                    tint = Color(0xFF00332C),

                    modifier = Modifier.size(15.dp)

                )

            }

        }



        Spacer(modifier = Modifier.height(8.dp))



        Text(

            text = "${user?.displayName ?: "Mi perfil"} #$friendTag",

            style = MaterialTheme.typography.titleLarge,

            fontWeight = FontWeight.Bold,

            color = MaterialTheme.colorScheme.onBackground

        )



        Spacer(modifier = Modifier.height(2.dp))



        Text(

            text = strings.formatActiveDays(summary.activeDays),

            style = MaterialTheme.typography.bodyMedium,

            color = if (isAppInDarkTheme) TextGrayDark else TextGrayLight

        )



        Spacer(modifier = Modifier.height(8.dp))



        Row(

            horizontalArrangement = Arrangement.spacedBy(8.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            OutlinedButton(

                onClick = onEdit,

                shape = RoundedCornerShape(14.dp)

            ) {

                Icon(

                    imageVector = Icons.Default.CameraAlt,

                    contentDescription = null,

                    modifier = Modifier.size(16.dp),

                    tint = MaterialTheme.colorScheme.primary

                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(

                    text = strings.editProfileAndPhoto,

                    fontWeight = FontWeight.Bold,

                    fontSize = 12.sp

                )

            }

            if (onOpenAuth != null) {

                TextButton(onClick = onOpenAuth) {

                    Text(

                        text = strings.accountAndCodeButton,

                        fontWeight = FontWeight.Bold,

                        fontSize = 12.sp

                    )

                }

            }

        }

    }

}



/**

 * PersonalStatsSection renderiza el tablero compacto y limpio de métricas semanales y racha actual.

 */

@Composable

fun PersonalStatsSection(summary: TrackingSummary = TrackingSummary()) {

    val isDark = isAppInDarkTheme

    val strings = AppStrings.current()

    val textGray = if (isDark) TextGrayDark else TextGrayLight

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

                text = strings.personalStatsTitle,

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                color = MaterialTheme.colorScheme.primary

            )

            Text(

                text = strings.last7DaysLabel,

                style = MaterialTheme.typography.labelSmall,

                fontWeight = FontWeight.Bold,

                color = textGray

            )

        }



        // TARJETA 1: Consistencia Semanal compacta (sin cuadro verde decorativo sin función)

        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),

            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

        ) {

            Column(modifier = Modifier.padding(16.dp)) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Column {

                        Text(

                            text = strings.consistencyLabel,

                            style = MaterialTheme.typography.bodySmall,

                            fontWeight = FontWeight.SemiBold,

                            color = textGray

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(

                            text = "${summary.weekPercent}%",

                            style = MaterialTheme.typography.headlineSmall,

                            fontWeight = FontWeight.ExtraBold,

                            color = MaterialTheme.colorScheme.primary

                        )

                    }



                    Surface(

                        shape = RoundedCornerShape(12.dp),

                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

                    ) {

                        Text(

                            text = "${summary.completedWeek}/${summary.scheduledWeek}",

                            style = MaterialTheme.typography.labelMedium,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)

                        )

                    }

                }



                Spacer(modifier = Modifier.height(12.dp))



                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(64.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.Bottom

                ) {

                    summary.days.forEachIndexed { index, dayItem ->

                        val barHeight = (dayItem.percent * 0.44f).coerceAtLeast(4f).dp

                        val isToday = index == summary.days.size - 1

                        Column(

                            horizontalAlignment = Alignment.CenterHorizontally,

                            verticalArrangement = Arrangement.Bottom

                        ) {

                            Box(

                                modifier = Modifier

                                    .width(22.dp)

                                    .height(barHeight)

                                    .background(

                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),

                                        shape = RoundedCornerShape(6.dp)

                                    )

                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(

                                text = HabitDates.label(dayItem.day, "EEE").take(2),

                                fontSize = 10.sp,

                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,

                                color = if (isToday) MaterialTheme.colorScheme.primary else textGray

                            )

                        }

                    }

                }

            }

        }



        // Racha y Progreso Actual

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

                        text = strings.currentStreakLabel.uppercase(),

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.Bold,

                        color = subtitleStreakColor

                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(

                        text = strings.formatDaysCount(summary.microStreak),

                        style = MaterialTheme.typography.headlineMedium,

                        fontWeight = FontWeight.Bold,

                        color = numberStreakColor

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = strings.formatBestStreak(summary.bestMicroStreak),

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

                        text = strings.currentProgressLabel.uppercase(),

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.Bold,

                        color = subtitleAccuracyColor

                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(

                        text = "${summary.todayPercent}%",

                        style = MaterialTheme.typography.headlineMedium,

                        fontWeight = FontWeight.Bold,

                        color = numberAccuracyColor

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = strings.formatTodayGoals(summary.completedToday, summary.scheduledToday),

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

 * AccountDataSection renderiza el panel de configuraciones, sesión guardada y datos de la cuenta.

 */

@Composable

fun AccountDataSection(

    user: UserEntity? = null,

    onEdit: () -> Unit = {},

    onOpenAuth: (() -> Unit)? = null,

    onLogout: (() -> Unit)? = null

) {

    val context = LocalContext.current

    val strings = AppStrings.current()

    var isTwoFactorEnabled by remember { mutableStateOf(true) }

    val isDark = isAppInDarkTheme

    val textGray = if (isDark) TextGrayDark else TextGrayLight

    val friendTag = user?.friendCode?.takeIf { it.isNotBlank() && it != "000" } ?: "777"



    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 20.dp),

        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {

        Text(

            text = strings.accountDetailsTitle,

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

                    onEdit()

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

                        text = strings.emailAddressLabel,

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.Bold,

                        color = textGray

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = user?.email ?: strings.noEmailConfigured,

                        style = MaterialTheme.typography.bodyLarge,

                        fontWeight = FontWeight.Medium,

                        color = MaterialTheme.colorScheme.onSurface

                    )

                }

                Text(

                    text = strings.changeAction,

                    style = MaterialTheme.typography.bodyLarge,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.primary

                )

            }

        }



        // Sesión Cloud / Código #XXX

        Card(

            modifier = Modifier

                .fillMaxWidth()

                .clickable {

                    if (onOpenAuth != null) {

                        onOpenAuth()

                    } else {

                        Toast.makeText(context, "Sesión activa como #$friendTag", Toast.LENGTH_SHORT).show()

                    }

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

                    contentDescription = "Sesión y Tag",

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier.size(22.dp)

                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(

                        text = strings.deviceSessionLabel,

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.Bold,

                        color = textGray

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = strings.formatSavedSessionTag(friendTag, user?.isLoggedIn == true),

                        style = MaterialTheme.typography.bodyLarge,

                        fontWeight = FontWeight.Medium,

                        color = MaterialTheme.colorScheme.onSurface

                    )

                }

                Text(

                    text = strings.configureAction,

                    style = MaterialTheme.typography.bodyLarge,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.primary

                )

            }

        }



        // Two Factor / Sincronización

        Card(

            modifier = Modifier

                .fillMaxWidth()

                .clickable {

                    onLogout?.invoke()

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

                    imageVector = Icons.Outlined.Shield,

                    contentDescription = "Two-Factor Auth",

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier.size(22.dp)

                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(

                        text = strings.multiDeviceSyncLabel,

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.Bold,

                        color = textGray

                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = strings.formatSyncActiveWithCode(friendTag, isTwoFactorEnabled),

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

