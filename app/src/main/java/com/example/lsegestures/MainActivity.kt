package com.example.lsegestures

// ------------------------------------------
// 📌 1. Android (Framework + Sistema)
// ------------------------------------------
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log


// ------------------------------------------
// 📌 2. Activity + Permissions (AndroidX)
// ------------------------------------------
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


// ------------------------------------------
// 📌 3. CameraX (Procesamiento de cámara)
// ------------------------------------------
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView


// ------------------------------------------
// 📌 4. Compose – Animations
// ------------------------------------------
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween


// ------------------------------------------
// 📌 5. Compose – Foundation / Layout
// ------------------------------------------
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape


// ------------------------------------------
// 📌 6. Compose – Material, Runtime y UI
// ------------------------------------------
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView


// ------------------------------------------
// 📌 7. Utilidades AndroidX y Kotlin
// ------------------------------------------
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay



val LseFontFamily = FontFamily(
    Font(R.font.good_times_rg, weight = FontWeight.Normal)
)
val LseFontFamily2 = FontFamily(
    Font(R.font.type_machine, weight = FontWeight.Normal)
)



// Pantallas simples: menú inicial / cámara
enum class Screen {
    HOME,
    CAMERA
}

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var gestureClassifier: TFLiteGestureClassifier? = null
    private var handLandmarkerHelper: HandLandmarkerHelper? = null

    private val seqLen = 60
    private val frameSequence = ArrayDeque<FloatArray>()

    private val gestureLabel = mutableStateOf<String?>(null)
    private val gestureConfidence = mutableStateOf<Float?>(null)

    // 🔊 Sonido
    private var mediaPlayer: MediaPlayer? = null
    private var lastPlayedGesture: String? = null
    private val minConfidenceForSound = 0.8f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👇 Mantener pantalla encendida mientras esta actividad está abierta
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraExecutor = Executors.newSingleThreadExecutor()

        gestureClassifier = TFLiteGestureClassifier(this)
        gestureClassifier?.runDummyInference()

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            maxNumHands = 2,
            listener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("HandLandmarker", "Error: $error (code=$errorCode)")
                }

                override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                    val result = resultBundle.results.firstOrNull()
                    val handsLandmarks = result?.landmarks() ?: emptyList()
                    val numHands = handsLandmarks.size

                    val frameFeatures = FloatArray(21 * 3 * 2)

                    if (numHands > 0) {
                        class HandInfo(val wristX: Float, val features63: FloatArray)

                        val handsInfo = mutableListOf<HandInfo>()

                        for (handIndex in 0 until numHands) {
                            val hand = handsLandmarks[handIndex]
                            if (hand.size < 21) continue

                            val coords = Array(21) { FloatArray(3) }
                            for (i in 0 until 21) {
                                val lm = hand[i]
                                coords[i][0] = lm.x()
                                coords[i][1] = lm.y()
                                coords[i][2] = lm.z()
                            }

                            val wristX = coords[0][0]
                            val wristY = coords[0][1]
                            val wristZ = coords[0][2]

                            var maxDist = 0f
                            for (i in 0 until 21) {
                                val dx = coords[i][0] - wristX
                                val dy = coords[i][1] - wristY
                                val dz = coords[i][2] - wristZ
                                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                                if (dist > maxDist) maxDist = dist
                            }

                            val vector63 = FloatArray(63)
                            for (i in 0 until 21) {
                                var dx = coords[i][0] - wristX
                                var dy = coords[i][1] - wristY
                                var dz = coords[i][2] - wristZ

                                if (maxDist > 0f) {
                                    dx /= maxDist
                                    dy /= maxDist
                                    dz /= maxDist
                                }

                                val base = i * 3
                                vector63[base] = dx
                                vector63[base + 1] = dy
                                vector63[base + 2] = dz
                            }

                            handsInfo.add(HandInfo(wristX, vector63))
                        }

                        handsInfo.sortBy { it.wristX }

                        if (handsInfo.isNotEmpty()) {
                            System.arraycopy(handsInfo[0].features63, 0, frameFeatures, 0, 63)
                            if (handsInfo.size >= 2) {
                                System.arraycopy(handsInfo[1].features63, 0, frameFeatures, 63, 63)
                            }
                        }
                    }

                    frameSequence.addLast(frameFeatures)
                    if (frameSequence.size > seqLen) frameSequence.removeFirst()

                    if (frameSequence.size == seqLen) {

                        val sequenceArray = Array(seqLen) { i ->
                            frameSequence.elementAt(i)
                        }

                        val classifier = gestureClassifier
                        val probs = classifier?.predict(sequenceArray)

                        if (probs != null) {
                            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
                            val confidence = probs[maxIdx]

                            val labels = arrayOf("Hola", "Adios", "Autonomia", "Igualdad")
                            val predicted = labels[maxIdx]

                            runOnUiThread {
                                gestureLabel.value = predicted
                                gestureConfidence.value = confidence

                                // 🔊 Intentar reproducir sonido según gesto + confianza
                                playGestureSoundIfNeeded(predicted, confidence)
                            }
                        }

                        frameSequence.clear()
                    }
                }
            }
        )

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.HOME) }

            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    onStartClick = { currentScreen = Screen.CAMERA }
                )
                Screen.CAMERA -> CameraScreen(
                    cameraExecutor = cameraExecutor,
                    handLandmarkerHelper = handLandmarkerHelper,
                    gestureLabel = gestureLabel,
                    gestureConfidence = gestureConfidence
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        gestureClassifier?.close()
        handLandmarkerHelper?.clear()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // 🔊 Reproduce el sonido asociado a un gesto, solo cuando CAMBIA y hay confianza suficiente
    private fun playGestureSoundIfNeeded(label: String, confidence: Float) {
        if (confidence < minConfidenceForSound) return
        if (label == lastPlayedGesture) return

        val resId = when (label) {
            "Hola" -> R.raw.hola
            "Adios" -> R.raw.adios
            "Autonomia" -> R.raw.autonomia
            "Igualdad" -> R.raw.igualdad
            else -> null
        } ?: return

        mediaPlayer?.release()
        mediaPlayer = null

        try {
            val mp = MediaPlayer.create(this, resId)
            mediaPlayer = mp
            mp.setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }

            mp.start()
            lastPlayedGesture = label

        } catch (e: Exception) {
            Log.e("GestureSound", "Error reproduciendo sonido para $label: ${e.message}", e)
        }
    }
}

// 🏠 Menú principal morado con botón circular + créditos
@Composable
fun HomeScreen(
    onStartClick: () -> Unit
) {
    var showCredits by remember { mutableStateOf(false) }

    // 🔹 Fade-in del bloque central
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "home_fade_in"
    )

    // 🔹 Pulso del botón "Empezar"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "start_button_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo degradado morado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B021F),
                            Color(0xFF3F2B96)
                        )
                    )
                )
        ) {
            // 🔮 Ondas holográficas animadas, detrás de todo
            HolographicWaves(
                modifier = Modifier.fillMaxSize()
            )

            // ✨ Palabras flotando y escribiéndose SOLO por la zona superior
            FloatingTypewriterWordsAboveTitle(
                modifier = Modifier.fillMaxSize()
            )

            // Contenido principal centrado con fade-in
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .alpha(alpha)
            ) {
                // Título
                Text(
                    text = "MAS-CA GESTURES",
                    fontSize = 27.sp,
                    fontFamily = LseFontFamily,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtítulo tipo social
                Text(
                    text = "Reconocimiento de gestos en Lengua de Signos",
                    fontSize = 15.sp,
                    color = Color(0xFFB3B3FF)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón circular morado con efecto "pulso"
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scale)   // 👈 pulso
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBB86FC),
                                    Color(0xFF7B4AF7)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { onStartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Empezar",
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Hashtags / etiqueta social
                Text(
                    text = "#MASCA   #LSE   #2Bach",
                    fontSize = 13.sp,
                    color = Color(0xFFB3E5FC)
                )
            }

            // Botón Créditos abajo a la derecha (igual funcional)
            Button(
                onClick = { showCredits = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text(text = "Créditos")
            }

            // Overlay de créditos (igual que antes)
            if (showCredits) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .background(
                                color = Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "✨ Créditos",
                            fontSize = 22.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Equipo MAS-CA",
                            fontSize = 18.sp,
                            color = Color(0xFFBB86FC)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Alumnos:",
                            fontSize = 14.sp,
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = "• Dario\n• Manuel\n• Raul",
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Driver:",
                            fontSize = 14.sp,
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = "Wladimir López de Zamora",
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Instituto:",
                            fontSize = 14.sp,
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = "I.E.S Hermanos Amoros\n2º Bachillerato",
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "#LSE  #MASCA  #2Bach",
                            fontSize = 12.sp,
                            color = Color(0xFF90CAF9)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Button(onClick = { showCredits = false }) {
                                Text(text = "Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✨ Palabras flotantes con efecto máquina de escribir + ligera rotación
// ✨ Palabras flotantes dobles con efecto máquina de escribir + ligera rotación
// ✨ Palabras flotantes dobles con efecto máquina de escribir + ligera rotación
// ✨ Palabras flotantes dobles con efecto máquina de escribir + rotación + glow
@Composable
fun FloatingTypewriterWordsAboveTitle(
    modifier: Modifier = Modifier
) {
    // Palabras / frases relacionadas con LSE e inclusión
    val words = listOf(
        "Inclusión",
        "Accesibilidad",
        "Igualdad",
        "Empatía",
        "Comunicación",
        "Lengua de signos",
        "Diversidad",
        "Respeto",
        "Integración social",
        "Conexión",
        "Libertad",
        "Identidad",
        "Convivencia",
        "Señas que unen",
        "Iguales en derechos",
        "Las señas hablan",
        "Sin barreras",
        "Somos comunidad",
        "Puentes, no muros"
    )

    // Paleta suave morado/azul/turquesa
    val palette = listOf(
        Color(0xFFB3E5FC),
        Color(0xFF80DEEA),
        Color(0xFFCE93D8),
        Color(0xFF81D4FA),
        Color(0xFFB39DDB)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "double_words")

    // Cursor compartido
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Rotación suave (-5° a +5°)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Flotación vertical ligera (-6dp a +6dp)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(modifier = modifier.fillMaxSize()) {

        // ZONAS BIEN SEPARADAS

        // Palabra 1 → zona superior izquierda (un poco más baja que antes)
        val leftX = listOf((-110).dp, (-85).dp, (-60).dp, (-40).dp)
        val upperY = listOf(
            70.dp,   // antes 60.dp
            95.dp,   // antes 85.dp
            120.dp   // antes 110.dp
        )

        // Palabra 2 → zona algo más baja y a la derecha (también bajada un poco)
        val rightX = listOf(40.dp, 70.dp, 100.dp, 130.dp)
        val lowerY = listOf(
            140.dp,  // antes 130.dp
            165.dp,  // antes 155.dp
            190.dp   // antes 180.dp
        )

        // 🔥 Sub-composable reutilizable
        @Composable
        fun AnimatedFloatingWord(
            seed: Int,
            xPositions: List<Dp>,
            yPositions: List<Dp>
        ) {
            var wordIndex by remember(seed) { mutableIntStateOf(seed % words.size) }
            var currentText by remember(seed) { mutableStateOf("") }
            var deleting by remember(seed) { mutableStateOf(false) }
            var targetX by remember(seed) { mutableStateOf(xPositions.random()) }
            var targetY by remember(seed) { mutableStateOf(yPositions.random()) }
            var currentFullLength by remember(seed) { mutableIntStateOf(words[wordIndex].length) }
            var color by remember(seed) { mutableStateOf(palette.random()) }

            // Animación suave hacia la nueva posición
            val animatedX by animateDpAsState(
                targetValue = targetX,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "word_x_$seed"
            )
            val animatedYBase by animateDpAsState(
                targetValue = targetY,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "word_y_$seed"
            )

            val animatedY = animatedYBase + floatOffset.dp

            // Progreso de escritura para un pequeño fade in/out
            val typeProgress =
                if (currentFullLength > 0) currentText.length.toFloat() / currentFullLength.toFloat()
                else 0f
            val localAlpha = 0.3f + 0.7f * typeProgress.coerceIn(0f, 1f)

            // Máquina de escribir con velocidades variables
            LaunchedEffect(Unit) {
                while (true) {
                    val full = words[wordIndex]
                    currentFullLength = full.length

                    if (!deleting) {
                        if (currentText.length < full.length) {
                            currentText = full.substring(0, currentText.length + 1)

                            // velocidad aleatoria 65–120 ms
                            val delayMs = Random.nextLong(65L, 120L)
                            delay(delayMs)
                        } else {
                            // pausa aleatoria cuando la palabra está completa
                            delay(Random.nextLong(700L, 1300L))
                            deleting = true
                        }
                    } else {
                        if (currentText.isNotEmpty()) {
                            currentText = currentText.dropLast(1)
                            delay(Random.nextLong(45L, 90L))
                        } else {
                            // nueva palabra + nueva posición + nuevo color
                            deleting = false
                            wordIndex = (wordIndex + 1) % words.size
                            targetX = xPositions.random()
                            targetY = yPositions.random()
                            color = palette.random()
                            delay(200L)
                        }
                    }
                }
            }

            val display = if (cursorAlpha > 0.5f) "$currentText|" else "$currentText "

            // Glow / pseudo-neón: texto duplicado detrás, más grande y transparente
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(animatedX, animatedY)
                    .rotate(rotation)
            ) {
                // Capa "glow"
                // Capa principal
                Text(
                    text = display,
                    fontSize = 20.sp,
                    fontFamily = LseFontFamily2,
                    color = color.copy(alpha = localAlpha),
                )
            }
        }

        // ✨ 2 palabras siempre alejadas y vivas
        AnimatedFloatingWord(
            seed = 1,
            xPositions = leftX,
            yPositions = upperY
        )

        AnimatedFloatingWord(
            seed = 99,
            xPositions = rightX,
            yPositions = lowerY
        )
    }
}

@Composable
fun HolographicWaves(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waves_transition")

    // Fases animadas para tres ondas con distinta velocidad (movimiento horizontal)
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_3"
    )

    // 🌊 Movimiento vertical suave de cada onda (sube/baja lentamente)
    val verticalShift1 by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertical_shift_1"
    )

    val verticalShift2 by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertical_shift_2"
    )

    val verticalShift3 by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertical_shift_3"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun drawWave(
            baseYRatio: Float,
            amplitudeRatio: Float,
            color: Color,
            phase: Float,
            verticalOffset: Float
        ) {
            val path = Path()
            // baseY en píxeles + pequeño desplazamiento vertical animado
            val baseY = h * baseYRatio + verticalOffset
            val amplitude = h * amplitudeRatio

            val points = 40
            val step = w / points

            path.moveTo(0f, baseY)

            var x = 0f
            while (x <= w) {
                val t = x / w
                val y = baseY + amplitude * sin(2f * PI.toFloat() * t * 2f + phase)
                path.lineTo(x, y)
                x += step
            }

            // Cerrar hacia abajo para poder rellenar
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }

        // Onda 1: morado claro, muy sutil
        drawWave(
            baseYRatio = 0.78f,          // más cerca de la parte baja
            amplitudeRatio = 0.04f,      // poca altura
            color = Color(0x33BB86FC),   // alpha muy bajo
            phase = phase1,
            verticalOffset = verticalShift1
        )

        // Onda 2: lila brillante, todavía más suave
        drawWave(
            baseYRatio = 0.82f,
            amplitudeRatio = 0.05f,
            color = Color(0x229C88FF),
            phase = phase2,
            verticalOffset = verticalShift2
        )

        // Onda 3: azul violáceo, casi solo “bruma”
        drawWave(
            baseYRatio = 0.86f,
            amplitudeRatio = 0.06f,
            color = Color(0x1A4A90E2),
            phase = phase3,
            verticalOffset = verticalShift3
        )
    }
}

@Composable
fun CameraScreen(
    cameraExecutor: ExecutorService,
    handLandmarkerHelper: HandLandmarkerHelper?,
    gestureLabel: State<String?>,
    gestureConfidence: State<Float?>
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize()) {

                CameraPreview(
                    cameraExecutor = cameraExecutor,
                    handLandmarkerHelper = handLandmarkerHelper
                )

                val label = gestureLabel.value
                val conf = gestureConfidence.value

                if (label != null && conf != null) {
                    val targetFraction = conf.coerceIn(0f, 1f)

                    var animatedFraction by remember { mutableFloatStateOf(targetFraction) }

                    LaunchedEffect(targetFraction) {
                        val start = animatedFraction
                        val durationMs = 200
                        val frameMs = 16
                        val steps = durationMs / frameMs
                        for (i in 1..steps) {
                            val t = i / steps.toFloat()
                            animatedFraction = start + (targetFraction - start) * t
                            delay(frameMs.toLong())
                        }
                        animatedFraction = targetFraction
                    }

                    val clampedFraction = animatedFraction.coerceIn(0f, 1f)
                    val percent = (clampedFraction * 100).toInt()

                    val barColor = when {
                        clampedFraction < 0.4f -> Color(0xFFFF5555)
                        clampedFraction < 0.7f -> Color(0xFFFFEE58)
                        else -> Color(0xFF66BB6A)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xAA000000),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(8.dp)
                                .background(
                                    color = Color(0x55FFFFFF),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(clampedFraction)
                                    .height(8.dp)
                                    .background(
                                        color = barColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$percent%",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Permiso de cámara DENEGADO")
            }
        }
    }
}

@Composable
fun CameraPreview(
    cameraExecutor: ExecutorService,
    handLandmarkerHelper: HandLandmarkerHelper?
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx: Context ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = CameraXPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                    val helper = handLandmarkerHelper
                    if (helper != null) {
                        helper.detectLiveStream(
                            imageProxy = imageProxy,
                            isFrontCamera = false
                        )
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CameraPreviewPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Preview (no cámara real en modo preview)")
    }
}
