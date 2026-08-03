package com.example // Define el paquete del código de pruebas

import androidx.test.ext.junit.runners.AndroidJUnit4 // Ejecutor de JUnit para ejecutar pruebas instrumentadas en Android
import androidx.test.platform.app.InstrumentationRegistry // Registro para obtener el contexto del sistema Android en pruebas
import org.junit.Assert.* // Métodos de aserción para validar los resultados esperados (ej. assertEquals)
import org.junit.Test // Anotación para marcar funciones ejecutables como casos de prueba de JUnit
import org.junit.runner.RunWith // Anotación para indicarle a JUnit con qué clase corredora ejecutar la prueba

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class) // Le dice a JUnit que esta prueba debe ejecutarse sobre el entorno Android real
class ExampleInstrumentedTest { // Define la clase del caso de prueba instrumentado
  @Test // Marca la función como un caso de prueba individual ejecutable
  fun useAppContext() { // Función de prueba para validar que el contexto del paquete sea correcto
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext // Obtiene el contexto de la aplicación en ejecución
    assertEquals("com.example", appContext.packageName) // Verifica que el nombre del paquete del APK coincida exactamente con "com.example"
  }
}
