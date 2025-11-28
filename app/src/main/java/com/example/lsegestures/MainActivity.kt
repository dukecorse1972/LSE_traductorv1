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
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable

// ------------------------------------------
// 📌 5. Compose – Foundation / Layout
// ------------------------------------------
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.delay

// Fuente personalizada
val LseFontFamily = FontFamily(
    Font(R.font.good_times_rg, weight = FontWeight.Normal)
)

// 🔹 Modelo de flashcards + datos
data class CardInfo(
    val title: String,
    val shortBody: String,
    val accent: Color,
    val background: List<Color>,
    val paragraphs: List<String>,
    val imageRes: Int         // 👈 imagen asociada a la card
)

// ⚠️ Ajusta los R.drawable.* a los nombres reales de tus imágenes
val infoCards = listOf(
    CardInfo(
        title = "Lengua de Signos Española",
        shortBody = "La LSE es una lengua viva con historia e identidad propias.",
        accent = Color(0xFFFB8CFF),
        background = listOf(Color(0xFFFFE6FF), Color(0xFFF3D9FF)),
        paragraphs = listOf(
            "La Lengua de Signos Española (LSE) es una lengua completa, con gramática y estructura propias. No se limita a traducir palabra por palabra el castellano, sino que organiza la información de una manera visual y espacial.",
            "Cada seña, cada expresión facial y cada movimiento del cuerpo aportan matices de significado. Por eso, la LSE no es un “apoyo” al habla, sino una forma legítima y plena de comunicación.",
            "Conocer y respetar la LSE significa reconocer la cultura, la identidad y la historia de la comunidad sorda que la utiliza cada día."
        ),
        imageRes = R.drawable.lse_card
    ),
    CardInfo(
        title = "Inclusión real",
        shortBody = "La accesibilidad comunicativa es un derecho, no un extra.",
        accent = Color(0xFF80DEEA),
        background = listOf(Color(0xFFE0FBFF), Color(0xFFCCF7FF)),
        paragraphs = listOf(
            "Cuando hablamos de inclusión real, no basta con que una persona “pueda estar” en un lugar. Es importante que también pueda participar, opinar y entender todo lo que ocurre a su alrededor.",
            "La accesibilidad comunicativa incluye intérpretes de LSE, subtítulos, materiales visuales y herramientas tecnológicas que reducen las barreras entre personas oyentes y sordas.",
            "Diseñar espacios accesibles no solo beneficia a la comunidad sorda: mejora la comunicación para todos y hace que los entornos sean más claros, respetuosos y humanos."
        ),
        imageRes = R.drawable.inclusion_card
    ),
    CardInfo(
        title = "Tecnología que acompaña",
        shortBody = "La tecnología puede ayudar a acercar mundos distintos.",
        accent = Color(0xFFFFF59D),
        background = listOf(Color(0xFFFFFDE7), Color(0xFFFFF9C4)),
        paragraphs = listOf(
            "La tecnología, bien usada, puede convertirse en una aliada de la accesibilidad. No sustituye a las personas ni a la Lengua de Signos, pero puede ayudar a visibilizar, enseñar y apoyar procesos de aprendizaje.",
            "Proyectos como MAS-CA GESTURES muestran que es posible combinar modelos de reconocimiento, diseño de interfaz y sensibilidad social para acercar la LSE a más gente.",
            "El reto está en que la tecnología no hable por la comunidad sorda, sino que camine a su lado, respetando sus tiempos, su cultura y sus necesidades reales."
        ),
        imageRes = R.drawable.tecnologia_card
    ),
    CardInfo(
        title = "Aprender a señar",
        shortBody = "Aprender LSE es un gesto de empatía y respeto.",
        accent = Color(0xFFB39DDB),
        background = listOf(Color(0xFFF2E7FE), Color(0xFFE9D7FF)),
        paragraphs = listOf(
            "Acercarse a la Lengua de Signos es abrir la puerta a una forma distinta de percibir y compartir el mundo. No se trata solo de memorizar señas, sino de aprender a mirar, a esperar y a comunicar con todo el cuerpo.",
            "Cada persona oyente que aprende LSE está tendiendo un puente hacia la comunidad sorda: facilita la convivencia en clase, en el trabajo y en la vida diaria.",
            "Aunque al principio cueste, cada seña aprendida es un pequeño paso hacia una sociedad donde comunicarse no dependa únicamente del oído, sino también de las manos, la mirada y la empatía."
        ),
        imageRes = R.drawable.aprender_card
    )
)

// 🔹 Flashcards amigables, gorditas y pastel (versión compacta, clicable)
// 👉 AHORA el índice viene de fuera y se notifica hacia fuera
@Composable
fun InfoFlashcards(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onCardClick: (Int) -> Unit
) {
    // -1 = izquierda, 0 = centro, 1 = derecha
    val slideX = remember { Animatable(-1f) }

    // Pulso suave
    val pulseTransition = rememberInfiniteTransition(label = "card_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_pulse_scale"
    )

    // Carrusel con animación, empezando en currentIndex (del padre)
    LaunchedEffect(Unit) {
        var idx = currentIndex
        while (true) {
            onIndexChange(idx)   // avisamos al HomeScreen de qué índice se está mostrando

            slideX.snapTo(-1f)

            slideX.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 550,
                    easing = FastOutSlowInEasing
                )
            )

            delay(4000L)

            slideX.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 550,
                    easing = FastOutSlowInEasing
                )
            )

            delay(150L)
            idx = (idx + 1) % infoCards.size
        }
    }

    val card = infoCards[currentIndex]

    // Transparencia suave
    val centerFactor = (1f - kotlin.math.abs(slideX.value)).coerceIn(0f, 1f)
    val cardAlpha = 0.45f + 0.55f * centerFactor

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (slideX.value * 420f).dp)
                .padding(horizontal = 24.dp)
        ) {
            // Flashcard compacta
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = cardAlpha
                    }
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .background(
                        brush = Brush.linearGradient(card.background),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable { onCardClick(currentIndex) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // 🖼 Imagen pequeña redondeada
                    Image(
                        painter = painterResource(id = card.imageRes),
                        contentDescription = card.title,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(4.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                card.accent,
                                                card.accent.copy(alpha = 0.6f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = card.title,
                                    fontSize = 14.sp,
                                    fontFamily = LseFontFamily,
                                    color = Color(0xFF2B2840)
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .width(40.dp)
                                        .height(2.dp)
                                        .background(
                                            color = card.accent.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = card.shortBody,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF3B3555)
                        )
                    }
                }
            }
        }
    }
}

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
                    gestureConfidence = gestureConfidence,
                    onBack = { currentScreen = Screen.HOME }
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
    var expandedCardIndex by remember { mutableStateOf<Int?>(null) }

    // 👇 índice del carrusel que se mantiene aunque se oculte InfoFlashcards
    var carouselIndex by remember { mutableStateOf(0) }

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

            // 🔹 Flashcards más abajo (solo si NO hay una expandida)
            InfoFlashcards(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .alpha(if (expandedCardIndex == null) 1f else 0f),
                currentIndex = carouselIndex,
                onIndexChange = { newIndex ->
                    carouselIndex = newIndex
                },
                onCardClick = { index ->
                    expandedCardIndex = index
                }
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

                // Subtítulo tipo social (mejor contraste)
                Text(
                    text = "Reconocimiento de gestos en Lengua de Signos",
                    fontSize = 16.sp,
                    color = Color(0xFFCED4FF)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón circular morado con glow suave + pulse
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBB86FC),
                                    Color(0xFF7B4AF7)
                                )
                            ),
                            shape = CircleShape
                        )
                        .shadow(
                            elevation = 22.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x884A2AFF),
                            spotColor = Color(0x884A2AFF)
                        )
                        .clickable { onStartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Empezar",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontFamily = LseFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                // Hashtags con contraste mejorado
                Text(
                    text = "#MASCA   #LSE   #2Bach",
                    fontSize = 14.sp,
                    color = Color(0xFFCCE6FF)
                )
            }

            // Botón Créditos abajo a la derecha
            Button(
                onClick = { showCredits = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text(text = "Créditos")
            }

            // Overlay de créditos
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

            // Overlay de flashcard expandida
            if (expandedCardIndex != null) {
                val card = infoCards[expandedCardIndex!!]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xDD050010))
                        .clickable { /* consumir clics, no cerrar aquí */ },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .background(
                                brush = Brush.linearGradient(card.background),
                                shape = RoundedCornerShape(26.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .width(5.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    card.accent,
                                                    card.accent.copy(alpha = 0.6f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(50)
                                        )
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = card.title,
                                    fontSize = 18.sp,
                                    fontFamily = LseFontFamily,
                                    color = Color(0xFF2B2840)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 🖼 Imagen grande en la card expandida
                            Image(
                                painter = painterResource(id = card.imageRes),
                                contentDescription = card.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Párrafos ordenados
                            card.paragraphs.forEachIndexed { index, p ->
                                Text(
                                    text = p,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF3B3555)
                                )
                                if (index != card.paragraphs.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clickable { expandedCardIndex = null }
                                    .background(
                                        color = Color(0x33000000),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Cerrar",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2B2840)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Ondas de fondo holográficas
@Composable
fun HolographicWaves(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waves_transition")

    // Fases animadas
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

    // Movimiento vertical
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

            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }

        drawWave(
            baseYRatio = 0.78f,
            amplitudeRatio = 0.04f,
            color = Color(0x33BB86FC),
            phase = phase1,
            verticalOffset = verticalShift1
        )

        drawWave(
            baseYRatio = 0.82f,
            amplitudeRatio = 0.05f,
            color = Color(0x229C88FF),
            phase = phase2,
            verticalOffset = verticalShift2
        )

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
    gestureConfidence: State<Float?>,
    onBack: () -> Unit
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize()) {

                // 📷 Cámara a pantalla completa
                CameraPreview(
                    cameraExecutor = cameraExecutor,
                    handLandmarkerHelper = handLandmarkerHelper
                )

                // 🔮 Degradado superior para hacer legible el HUD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xAA0B021F),
                                    Color(0x550B021F),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // ✨ HUD de gesto (integrado, moderno)
                val label = gestureLabel.value
                val conf = gestureConfidence.value

                GestureHud(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp),
                    label = label,
                    confidence = conf
                )

                // 🔙 Botón volver al menú (abajo a la derecha)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    BackToMenuButton(onClick = onBack)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Permiso de cámara DENEGADO",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GestureHud(
    modifier: Modifier = Modifier,
    label: String?,
    confidence: Float?
) {
    // Fade-in suave del HUD
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "hud_alpha"
    )

    // Animación del valor de confianza
    val targetFraction = (confidence ?: 0f).coerceIn(0f, 1f)
    var animatedFraction by remember { mutableStateOf(targetFraction) }

    LaunchedEffect(targetFraction) {
        val start = animatedFraction
        val durationMs = 220
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

    val displayLabel = label ?: "Esperando gesto..."
    val isIdle = label == null

    // 🟣 HUD TRANSLÚCIDO estilo glass overlay
    Box(
        modifier = modifier.alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xAA1A0D2E),
                            Color(0x662B1E80),
                            Color(0x55291D72)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {

                // 🟣 Texto del gesto
                Text(
                    text = displayLabel,
                    fontSize = if (isIdle) 22.sp else 28.sp,
                    color = Color.White,
                    fontFamily = LseFontFamily
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🟩 Barra de confianza tipo "capsule"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = Color(0x50FFFFFF),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedFraction)
                            .height(12.dp)
                            .background(
                                color = barColor,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 🔵 Porcentaje tipo "pill"
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isIdle) "--%" else "$percent%",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun BackToMenuButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xAA1A0D2E),
                        Color(0x662B1E80),
                        Color(0x55291D72)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(
            text = "MENU",
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = LseFontFamily
        )
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