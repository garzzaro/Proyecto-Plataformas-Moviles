package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HabitImage
import com.example.ui.theme.DarkTealBackground
import com.example.ui.theme.DarkTealText
import com.example.ui.theme.DeepDarkTeal
import com.example.ui.theme.LightTealAccent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import kotlinx.coroutines.delay

/**
 * SplashScreen renderiza la portada de bienvenida de la aplicación.
 *
 * - **¿Para qué sirve?** Ofrece una experiencia de marca inmersiva antes de acceder al contenido principal.
 * - **¿Cómo funciona?** Despliega el logotipo del pulpo HabitOcto con degradados de color temáticos y un temporizador que habilita el botón de inicio.
 * - **¿Qué pasa si lo quitamos?** La aplicación entraría directamente a la interfaz de hábitos sin una presentación visual previa.
 */
@Composable
fun SplashScreen(
    onContinue: () -> Unit
) {
    // Estado reactivo que indica si la animación inicial ha concluido para permitir continuar
    var isReady by remember { mutableStateOf(false) }

    // Efecto lanzado que simula la carga inicial de recursos de la aplicación
    LaunchedEffect(Unit) {
        delay(1800)
        isReady = true
    }

    // Contenedor de pantalla completa con degradado corporativo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryTeal,
                        DarkTealBackground,
                        DeepDarkTeal
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Columna centrada con el logotipo, nombre y lema de la aplicación
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Marco circular con brillo y avatar del pulpo
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                HabitImage(
                    model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif",
                    contentDescription = "Mascota Pulpo HabitOcto",
                    modifier = Modifier
                        .size(144.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre de la aplicación
            Text(
                text = "HabitOcto",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lema motivacional
            Text(
                text = "Transforma tu vida, un micro hábito a la vez",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LightTealAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Muestra indicador circular mientras se inicializa o botón de inicio al finalizar
            if (!isReady) {
                CircularProgressIndicator(
                    color = SecondaryTeal,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Empezar",
                        color = DarkTealText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pie de página informativo con versión
        Text(
            text = "Versión 1.0 • HabitOcto Team",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}
