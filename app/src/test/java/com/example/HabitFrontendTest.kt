package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.TrackerScreen
import com.example.ui.components.HabitItemCard
import com.example.ui.components.HabitProgressDialog
import com.example.ui.components.DataStateContent
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [36])
class HabitFrontendTest {
    @get:Rule val compose = createComposeRule()
    private val habit = HabitItem("h", "Leer una página de un libro cada mañana", "", "Aprendizaje", isMicroHabit = true,
        tags = listOf("Micro", "Diario", "Aprendizaje"))

    @Test fun `load error exposes retry and replaces the loading indicator`() {
        var state by mutableStateOf<UiState<Unit>>(UiState.Loading)
        var retries = 0
        compose.setContent {
            MyApplicationTheme {
                DataStateContent(state, onRetry = { retries++; state = UiState.Empty }) {
                    androidx.compose.material3.Text("Sin hábitos guardados")
                }
            }
        }
        compose.onNodeWithText("Cargando tus datos…").assertExists()
        compose.runOnIdle { state = UiState.Error("No se pudo abrir el almacenamiento") }
        compose.onNodeWithText("Cargando tus datos…").assertDoesNotExist()
        compose.onNodeWithText("No se pudieron cargar tus datos").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/habitocto/load-error.png")
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText("Sin hábitos guardados").assertExists()
        assertEquals(1, retries)
    }

    @Test fun `completion control changes status and keeps the card editable`() {
        var state by mutableStateOf(habit)
        compose.setContent {
            MyApplicationTheme {
                HabitItemCard(state, onToggleComplete = { state = state.copy(isCompleted = !state.isCompleted) }, onClickDetail = {})
            }
        }
        compose.onNodeWithContentDescription("Registrar avance de ${habit.title}").performClick()
        compose.onNodeWithText("Completado", substring = true).assertExists()
        compose.onNodeWithContentDescription("Completado. Cambiar cumplimiento de ${habit.title}").performClick()
        compose.onNodeWithText("Pendiente", substring = true).assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/habitocto/card-light.png")
    }

    // Robolectric #8460: tamaño forzado + TextField en Dialog puede oscilar en medición.
    // Las demás pruebas conservan la pantalla estrecha; esta comprueba la entrada numérica.
    @Config(qualifiers = "xhdpi", sdk = [36])
    @Test fun `progress supports large quantities and rejects invalid values`() {
        var saved = -1
        compose.setContent {
            MyApplicationTheme {
                HabitProgressDialog(habit.copy(isProgressive = true, maxProgress = 10_000), onDismiss = {}, onSave = { saved = it })
            }
        }
        compose.onNodeWithText("Avance acumulado").performTextReplacement("10001")
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        compose.onNodeWithText("Avance acumulado").performTextReplacement("8200")
        compose.onNodeWithText("Guardar avance").performClick()
        assertEquals(8200, saved)
    }

    @Test fun `tracker empty state is shown in dark mode`() {
        val summary = HabitTracking.summary(emptyList(), emptySet(), HabitDates.today())
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                TrackerScreen(emptyList(), {}, {}, {}, {}, summary = summary)
            }
        }
        compose.onNodeWithText("Aún no tienes hábitos.", substring = true).assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/habitocto/tracker-dark.png")
    }

    @Test fun `profile renders actual stats on a narrow screen`() {
        val today = HabitDates.today()
        val rows = (today - 6..today).map { day -> HabitRecordEntity("h", day, day, "Leer", true, false, "", 1, 1, day) }
        val summary = HabitTracking.summary(rows, setOf("h"), today)
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                Column(modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState()).padding(8.dp)) {
                    ProfileHeaderSection(UserEntity("u", "Mi nombre", createdDay = today), summary)
                    PersonalStatsSection(summary)
                }
            }
        }
        compose.onAllNodesWithText("100%", useUnmergedTree = true).assertCountEquals(2)
        compose.onRoot().captureRoboImage(filePath = "build/reports/habitocto/profile-light.png")
    }
}
