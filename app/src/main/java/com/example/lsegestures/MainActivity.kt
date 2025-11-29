package com.example.lsegestures

// ------------------------------------------
// 📌 1. IMPORTS
// ------------------------------------------
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.widget.VideoView

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

// Imports de ML Kit y Jsoup
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

// ------------------------------------------
// 📌 2. CONFIGURACIÓN Y DATOS
// ------------------------------------------

val LseFontFamily = FontFamily(
    Font(R.font.good_times_rg, weight = FontWeight.Normal)
)

data class CardInfo(
    val title: String,
    val shortBody: String,
    val accent: Color,
    val background: List<Color>,
    val paragraphs: List<String>,
    val imageRes: Int
)

val infoCards = listOf(
    CardInfo(
        title = "Lengua de Signos Española",
        shortBody = "La LSE es una lengua viva con historia e identidad propias.",
        accent = Color(0xFFFB8CFF),
        background = listOf(Color(0xFFFFE6FF), Color(0xFFF3D9FF)),
        paragraphs = listOf(
            "La Lengua de Signos Española (LSE) es una lengua completa, con gramática y estructura propias.",
            "Cada seña, cada expresión facial y cada movimiento del cuerpo aportan matices de significado.",
            "Conocer y respetar la LSE significa reconocer la cultura, la identidad y la historia de la comunidad sorda."
        ),
        imageRes = R.drawable.lse_card
    ),
    CardInfo(
        title = "Inclusión real",
        shortBody = "La accesibilidad comunicativa es un derecho, no un extra.",
        accent = Color(0xFF80DEEA),
        background = listOf(Color(0xFFE0FBFF), Color(0xFFCCF7FF)),
        paragraphs = listOf(
            "Cuando hablamos de inclusión real, no basta con que una persona “pueda estar” en un lugar.",
            "La accesibilidad comunicativa incluye intérpretes de LSE, subtítulos y herramientas tecnológicas.",
            "Diseñar espacios accesibles mejora la comunicación para todos."
        ),
        imageRes = R.drawable.inclusion_card
    ),
    CardInfo(
        title = "Tecnología que acompaña",
        shortBody = "La tecnología puede ayudar a acercar mundos distintos.",
        accent = Color(0xFFFFF59D),
        background = listOf(Color(0xFFFFFDE7), Color(0xFFFFF9C4)),
        paragraphs = listOf(
            "La tecnología, bien usada, puede convertirse en una aliada de la accesibilidad.",
            "Proyectos como MAS-CA GESTURES muestran que es posible combinar modelos de reconocimiento y diseño social.",
            "El reto está en que la tecnología no hable por la comunidad sorda, sino que camine a su lado."
        ),
        imageRes = R.drawable.tecnologia_card
    ),
    CardInfo(
        title = "Aprender a señar",
        shortBody = "Aprender LSE es un gesto de empatía y respeto.",
        accent = Color(0xFFB39DDB),
        background = listOf(Color(0xFFF2E7FE), Color(0xFFE9D7FF)),
        paragraphs = listOf(
            "Acercarse a la Lengua de Signos es abrir la puerta a una forma distinta de percibir el mundo.",
            "Cada persona oyente que aprende LSE está tendiendo un puente hacia la comunidad sorda.",
            "Cada seña aprendida es un pequeño paso hacia una sociedad más inclusiva."
        ),
        imageRes = R.drawable.aprender_card
    )
)

// 🌍 CONFIGURACIÓN DE IDIOMAS (ML KIT + SPREADTHESIGN)
data class SignLanguage(
    val name: String,
    val flag: String,
    val urlCode: String,      // Para SpreadTheSign (ej: en.us)
    val mlKitCode: String     // Para Google Translate (ej: "en")
)

val availableLanguages = listOf(
    SignLanguage("España (LSE)", "🇪🇸", "es.es", TranslateLanguage.SPANISH),
    SignLanguage("EE.UU. (ASL)", "🇺🇸", "en.us", TranslateLanguage.ENGLISH),
    SignLanguage("Reino Unido (BSL)", "🇬🇧", "en.gb", TranslateLanguage.ENGLISH),
    SignLanguage("Francia (LSF)", "🇫🇷", "fr.fr", TranslateLanguage.FRENCH),
    SignLanguage("Alemania (DGS)", "🇩🇪", "de.de", TranslateLanguage.GERMAN),
    SignLanguage("Italia (LIS)", "🇮🇹", "it.it", TranslateLanguage.ITALIAN),
    SignLanguage("Portugal (LGP)", "🇵🇹", "pt.pt", TranslateLanguage.PORTUGUESE)
)

// FUNCIÓN SUSPENDIDA PARA TRADUCIR CON ML KIT
suspend fun translateText(text: String, targetLang: String): String {
    if (targetLang == TranslateLanguage.SPANISH) return text // No traducir si es español

    return suspendCancellableCoroutine { continuation ->
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        continuation.resume(translatedText)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
}

enum class Screen { HOME, CAMERA }

// ------------------------------------------
// 📌 3. MAIN ACTIVITY
// ------------------------------------------

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var gestureClassifier: TFLiteGestureClassifier? = null
    private var handLandmarkerHelper: HandLandmarkerHelper? = null

    private val seqLen = 60
    private val frameSequence = ArrayDeque<FloatArray>()
    private var predictionCounter = 0

    private val gestureLabel = mutableStateOf<String?>(null)
    private val gestureConfidence = mutableStateOf<Float?>(null)

    private lateinit var soundPool: SoundPool
    private val soundMap = SparseIntArray()
    private var lastPlayedGesture: String? = null
    private val minConfidenceForSound = 0.8f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // ⚠️ Asegúrate de tener estos archivos en res/raw o la app crasheará al iniciar
        // Si no los tienes, comenta estas 4 líneas
        try {
            soundMap.put(1, soundPool.load(this, R.raw.gracias, 1))
            soundMap.put(2, soundPool.load(this, R.raw.buenos_dias, 1))
            soundMap.put(3, soundPool.load(this, R.raw.triste, 1))
            soundMap.put(4, soundPool.load(this, R.raw.alegre, 1))
            soundMap.put(5, soundPool.load(this, R.raw.amigo, 1))
            soundMap.put(6, soundPool.load(this, R.raw.ayuda, 1))
        } catch (e: Exception) {
            Log.e("SoundPool", "Error cargando sonidos: ${e.message}")
        }

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

                    predictionCounter++
                    val shouldPredict = (frameSequence.size == seqLen) && (predictionCounter % 4 == 0)

                    if (shouldPredict) {
                        val sequenceArray = Array(seqLen) { i -> frameSequence.elementAt(i) }
                        val classifier = gestureClassifier
                        val probs = classifier?.predict(sequenceArray)

                        if (probs != null) {
                            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
                            val confidence = probs[maxIdx]
                            val labels = arrayOf("Gracias", "Buenos dias", "Triste", "Alegre", "Amigo", "Ayuda")
                            val predicted = labels[maxIdx]

                            runOnUiThread {
                                gestureLabel.value = predicted
                                gestureConfidence.value = confidence
                                playGestureSoundIfNeeded(predicted, confidence)
                            }
                        }
                    }
                }
            }
        )

        setContent {
            var currentScreen by remember { mutableStateOf(Screen.HOME) }
            val context = LocalContext.current

            when (currentScreen) {
                Screen.HOME -> {
                    SwipeableMainScreen(
                        onStartCamera = { currentScreen = Screen.CAMERA },
                        context = context
                    )
                }
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
        soundPool.release()
    }

    private fun playGestureSoundIfNeeded(label: String, confidence: Float) {
        if (confidence < minConfidenceForSound) return
        if (label == lastPlayedGesture) return

        val soundId = when (label) {
            "Gracias" -> soundMap[1]
            "Buenos dias" -> soundMap[2]
            "Triste" -> soundMap[3]
            "Alegre" -> soundMap[4]
            "Amigo" -> soundMap[5]
            "Ayuda" -> soundMap[6]
            else -> 0
        }
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            lastPlayedGesture = label
        }
    }
}

// ----------------------------------------------------------------------
// 🚀 UI COMPOSABLES
// ----------------------------------------------------------------------

@Composable
fun SwipeableMainScreen(
    onStartCamera: () -> Unit,
    context: Context
) {
    // 2 PÁGINAS SOLAMENTE: 0=Home, 1=Voz (Hemos quitado la 3)
    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B021F), Color(0xFF3F2B96))
                )
            )
    ) {
        HolographicWaves(modifier = Modifier.fillMaxSize())

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreenContent(onStartClick = onStartCamera)
                1 -> VoiceToSignScreen(context = context)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            repeat(2) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceToSignScreen(context: Context) {
    var recognizedText by remember { mutableStateOf("Pulsa el micro y di: Hola") }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var isVideoExpanded by remember { mutableStateOf(false) }

    // Idioma
    var selectedLang by remember { mutableStateOf(availableLanguages[0]) }
    var showMenu by remember { mutableStateOf(false) }
    var translatedTerm by remember { mutableStateOf("") }

    LaunchedEffect(recognizedText, selectedLang) {
        if (recognizedText.contains("Pulsa el micro") || recognizedText.contains("Error") || recognizedText.isBlank()) return@LaunchedEffect

        isLoading = true
        hasSearched = true
        currentVideoUrl = null
        isVideoExpanded = false

        // 1. TRADUCCIÓN CON ML KIT
        val originalWord = recognizedText.trim().lowercase()
        var wordToSearch = originalWord

        if (selectedLang.mlKitCode != TranslateLanguage.SPANISH) {
            loadingMessage = "Traduciendo a ${selectedLang.name}..."
            try {
                val translation = withContext(Dispatchers.IO) {
                    translateText(originalWord, selectedLang.mlKitCode)
                }
                wordToSearch = translation.lowercase()
                translatedTerm = wordToSearch
                Log.d("TRANSLATOR", "Traducción exitosa: $originalWord -> $wordToSearch")
            } catch (e: Exception) {
                Log.e("TRANSLATOR", "Fallo al traducir: ${e.message}")
                wordToSearch = originalWord
            }
        } else {
            translatedTerm = ""
        }

        // 2. SCRAPING BLINDADO
        loadingMessage = "Buscando vídeo en la nube..."

        val videoFound = withContext(Dispatchers.IO) {
            try {
                val cleanText = wordToSearch.replace(" ", "+")
                val urlBusqueda = "https://www.spreadthesign.com/${selectedLang.urlCode}/search/?q=$cleanText"

                val connection = Jsoup.connect(urlBusqueda)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .header("Accept-Language", "es-ES,es;q=0.9")
                    .referrer("https://www.google.com/")
                    .ignoreHttpErrors(true)
                    .timeout(15000)

                val docLista = connection.get()

                if (connection.response().statusCode() == 403) return@withContext null

                var urlFicha: String? = null
                if (docLista.location().contains("/word/")) {
                    urlFicha = docLista.location()
                } else {
                    val fichaLink = docLista.select("a[href*=/word/]").first()
                    if (fichaLink != null) urlFicha = fichaLink.attr("abs:href")
                }

                if (urlFicha == null) return@withContext null

                val docFicha = if (urlFicha == docLista.location()) docLista else Jsoup.connect(urlFicha)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    .referrer(urlBusqueda)
                    .timeout(10000)
                    .get()

                var src: String? = null
                val videoSource = docFicha.select("video source").first()
                if (videoSource != null) src = videoSource.attr("src")

                if (src.isNullOrEmpty()) {
                    val videoTag = docFicha.select("video").first()
                    src = videoTag?.attr("src")
                }

                if (src.isNullOrEmpty()) {
                    val htmlBruto = docFicha.html()
                    val regex = """https://media\.spreadthesign\.com/video/mp4/[^"]+\.mp4""".toRegex()
                    val match = regex.find(htmlBruto)
                    src = match?.value
                }

                if (src != null) {
                    if (!src.startsWith("http")) src = "https://www.spreadthesign.com$src"
                    src
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        currentVideoUrl = videoFound
        isLoading = false
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.get(0)?.lowercase() ?: ""
            recognizedText = text
        }
    }

    // INTERFAZ
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp, start = 30.dp, end = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. TÍTULO CENTRADO
            Text("MODO OYENTE", fontSize = 26.sp, fontFamily = LseFontFamily, color = Color(0xFF80DEEA))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Traductor Global AI", fontSize = 15.sp, color = Color(0xFFB2EBF2))

            Spacer(modifier = Modifier.height(30.dp))

            // 2. CAJA DE VÍDEO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .shadow(10.dp, RoundedCornerShape(30.dp))
                    .clickable(enabled = currentVideoUrl != null) {
                        isVideoExpanded = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF80DEEA))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(loadingMessage, color = Color(0xFF80DEEA), fontSize = 12.sp)
                    }
                }
                else if (currentVideoUrl != null && !isVideoExpanded) {
                    LseVideoPlayer(
                        videoUrl = currentVideoUrl!!,
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(Icons.Default.PlayCircleOutline, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(50.dp).align(Alignment.Center))
                    Text("Toca para ampliar", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
                } else if (hasSearched && currentVideoUrl == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Videocam, null, tint = Color(0xFFFF5555), modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No encontrado en ${selectedLang.name}", color = Color(0xFFFF5555), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Videocam, null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                        Text("El vídeo aparecerá aquí", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. DESPLEGABLE DE IDIOMA (DEBAJO DEL VIDEO)
            Box {
                Button(
                    onClick = { showMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Idioma: ${selectedLang.flag} ${selectedLang.name}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1C1C1E))
                ) {
                    availableLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(lang.name, color = Color.White)
                                }
                            },
                            onClick = {
                                selectedLang = lang
                                showMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. TEXTO RECONOCIDO
            Text("\"$recognizedText\"", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            if (selectedLang.urlCode != "es.es" && hasSearched) {
                Text("Traducción: $translatedTerm", fontSize = 14.sp, color = Color(0xFF80DEEA))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. BOTÓN MICRÓFONO
            val infiniteTransition = rememberInfiniteTransition(label = "mic")
            val scale by infiniteTransition.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale")

            Box(
                modifier = Modifier
                    .size(100.dp).scale(scale)
                    .shadow(20.dp, CircleShape, spotColor = Color(0xFF00E5FF))
                    .background(Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF008299))), CircleShape)
                    .clickable {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                        try { speechLauncher.launch(intent) } catch (_: Exception) { recognizedText = "Error micro" }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(50.dp))
            }
            Spacer(modifier = Modifier.height(50.dp))
        }

        // CAPA DE PANTALLA COMPLETA
        if (isVideoExpanded && currentVideoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(enabled = false) {}
            ) {
                LseVideoPlayer(videoUrl = currentVideoUrl!!, modifier = Modifier.fillMaxSize().align(Alignment.Center))
                IconButton(
                    onClick = { isVideoExpanded = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Cerrar", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 📹 COMPONENTE REUTILIZABLE: REPRODUCTOR DE VÍDEO LSE
// ---------------------------------------------------------
@Composable
fun LseVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }

    AndroidView(
        factory = {
            videoView.apply {
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        update = { view ->
            if (view.tag != videoUrl) {
                view.setVideoURI(Uri.parse(videoUrl))
                view.start()
                view.tag = videoUrl
            }
        },
        modifier = modifier
    )
}

// ----------------------------------------------------------------------
// 🏠 PANTALLA HOME
// ----------------------------------------------------------------------

@Composable
fun HomeScreenContent(onStartClick: () -> Unit) {
    var showCredits by remember { mutableStateOf(false) }
    var expandedCardIndex by remember { mutableStateOf<Int?>(null) }
    var carouselIndex by remember { mutableStateOf(0) }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    val alpha by animateFloatAsState(if (startAnimation) 1f else 0f, tween(800), label = "home_fade_in")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "start_button_scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        InfoFlashcards(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .alpha(if (expandedCardIndex == null) 1f else 0f),
            currentIndex = carouselIndex,
            onIndexChange = { carouselIndex = it },
            onCardClick = { expandedCardIndex = it }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .alpha(alpha)
        ) {
            Text("MAS-CA TRADUCTOR", fontSize = 26.sp, fontFamily = LseFontFamily, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Reconocimiento de gestos en tiempo real", fontSize = 15.sp, color = Color(0xFFCED4FF))
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFBB86FC), Color(0xFF7B4AF7))),
                        CircleShape
                    )
                    .shadow(22.dp, CircleShape, ambientColor = Color(0x884A2AFF), spotColor = Color(0x884A2AFF))
                    .clickable { onStartClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("Empezar", fontSize = 20.sp, color = Color.White, fontFamily = LseFontFamily)
            }
            Spacer(modifier = Modifier.height(34.dp))
            Text("#MASCA   #LSE   #2Bach", fontSize = 14.sp, color = Color(0xFFCCE6FF))
        }

        Button(
            onClick = { showCredits = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Créditos")
        }

        if (showCredits) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("✨ Créditos", fontSize = 22.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Equipo MAS-CA", fontSize = 18.sp, color = Color(0xFFBB86FC))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Alumnos:\n• Dario\n• Manuel\n• Raul", fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Driver:\nWladimir López de Zamora", fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Instituto:\nI.E.S Hermanos Amoros\n2º Bachillerato", fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { showCredits = false }) { Text("Cerrar") }
                }
            }
        }

        if (expandedCardIndex != null) {
            val card = infoCards[expandedCardIndex!!]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD050010))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .background(Brush.linearGradient(card.background), RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(card.title, fontSize = 18.sp, fontFamily = LseFontFamily, color = Color(0xFF2B2840))
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = card.imageRes),
                            contentDescription = card.title,
                            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        card.paragraphs.forEach { p ->
                            Text(p, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF3B3555))
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.align(Alignment.End).clickable { expandedCardIndex = null }
                                .background(Color(0x33000000), RoundedCornerShape(50)).padding(14.dp, 8.dp)
                        ) {
                            Text("Cerrar", fontSize = 13.sp, color = Color(0xFF2B2840))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 🔧 RESTO DE COMPONENTES
// ----------------------------------------------------

@Composable
fun InfoFlashcards(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onCardClick: (Int) -> Unit
) {
    val slideX = remember { Animatable(-1f) }

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

    LaunchedEffect(Unit) {
        var idx = currentIndex
        while (true) {
            onIndexChange(idx)

            slideX.snapTo(-1f)
            slideX.animateTo(
                targetValue = 0f,
                animationSpec = tween(550, easing = FastOutSlowInEasing)
            )
            delay(4000L)
            slideX.animateTo(
                targetValue = 1f,
                animationSpec = tween(550, easing = FastOutSlowInEasing)
            )
            delay(150L)
            idx = (idx + 1) % infoCards.size
        }
    }

    val card = infoCards[currentIndex]
    val centerFactor = (1f - kotlin.math.abs(slideX.value)).coerceIn(0f, 1f)
    val cardAlpha = 0.45f + 0.55f * centerFactor

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (slideX.value * 420f).dp)
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = cardAlpha
                    }
                    .shadow(14.dp, RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(card.background), RoundedCornerShape(22.dp))
                    .clickable { onCardClick(currentIndex) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        Brush.verticalGradient(
                                            listOf(card.accent, card.accent.copy(alpha = 0.6f))
                                        ),
                                        RoundedCornerShape(50)
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
                                        .background(card.accent.copy(alpha = 0.6f), RoundedCornerShape(50))
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

@Composable
fun HolographicWaves(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waves_transition")
    val phase1 by infiniteTransition.animateFloat(0f, (2f * PI).toFloat(), infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Restart), "p1")
    val phase2 by infiniteTransition.animateFloat(0f, (2f * PI).toFloat(), infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Restart), "p2")
    val phase3 by infiniteTransition.animateFloat(0f, (2f * PI).toFloat(), infiniteRepeatable(tween(15000, easing = FastOutSlowInEasing), RepeatMode.Restart), "p3")

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun drawWave(baseYRatio: Float, amplitudeRatio: Float, color: Color, phase: Float) {
            val path = Path()
            val baseY = h * baseYRatio
            val amplitude = h * amplitudeRatio
            val step = w / 40
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, baseY + amplitude * sin(2f * PI.toFloat() * (x / w) * 2f + phase))
                x += step
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            drawPath(path = path, color = color, style = Fill)
        }
        drawWave(0.78f, 0.04f, Color(0x33BB86FC), phase1)
        drawWave(0.82f, 0.05f, Color(0x229C88FF), phase2)
        drawWave(0.86f, 0.06f, Color(0x1A4A90E2), phase3)
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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(cameraExecutor, handLandmarkerHelper)
                Box(
                    modifier = Modifier.fillMaxWidth().height(210.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xAA0B021F), Color(0x550B021F), Color.Transparent)))
                        .align(Alignment.TopCenter)
                )
                GestureHud(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 24.dp, end = 24.dp),
                    label = gestureLabel.value,
                    confidence = gestureConfidence.value
                )
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                    BackToMenuButton(onClick = onBack)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permiso de cámara DENEGADO", color = Color.White)
            }
        }
    }
}

@Composable
fun GestureHud(modifier: Modifier = Modifier, label: String?, confidence: Float?) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(600), label = "hud_alpha")

    val targetFraction = (confidence ?: 0f).coerceIn(0f, 1f)
    val clampedFraction = targetFraction
    val percent = (clampedFraction * 100).toInt()

    val barColor = when {
        clampedFraction < 0.4f -> Color(0xFFFF5555)
        clampedFraction < 0.7f -> Color(0xFFFFEE58)
        else -> Color(0xFF66BB6A)
    }
    val displayLabel = label ?: "Esperando gesto..."

    Box(modifier = modifier.alpha(alpha)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xAA1A0D2E), Color(0x662B1E80), Color(0x55291D72))),
                RoundedCornerShape(26.dp)
            ).padding(22.dp, 18.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(displayLabel, fontSize = if (label == null) 22.sp else 28.sp, color = Color.White, fontFamily = LseFontFamily)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp).background(Color(0x50FFFFFF), RoundedCornerShape(50))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(clampedFraction).height(12.dp).background(barColor, RoundedCornerShape(50)))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier.background(Color(0x33000000), RoundedCornerShape(50)).padding(12.dp, 6.dp)
                ) {
                    Text(if (label == null) "--%" else "$percent%", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BackToMenuButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.background(
            Brush.linearGradient(listOf(Color(0xAA1A0D2E), Color(0x662B1E80), Color(0x55291D72))),
            RoundedCornerShape(50)
        ).clickable { onClick() }.padding(22.dp, 12.dp)
    ) {
        Text("MENU", color = Color.White, fontSize = 16.sp, fontFamily = LseFontFamily)
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

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                    val helper = handLandmarkerHelper
                    if (helper != null) {
                        helper.detectLiveStream(imageProxy, false)
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Preview")
    }
}