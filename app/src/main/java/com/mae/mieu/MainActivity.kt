package com.mae.mieu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import org.json.JSONObject

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val loraFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Lora"), fontProvider = fontProvider)
)

private val pawPink = Color(0xFFE87090)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MieuScreen()
            }
        }
    }
}

@Composable
private fun MieuScreen() {
    val ctx = LocalContext.current
    val data = remember { loadAphorisms(ctx) }
    val vm: MieuViewModel = viewModel { MieuViewModel(data.aphorisms, data.total) }
    val current by vm.current.collectAsState()
    val navEnabled by vm.navigationEnabled.collectAsState()
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "paw")
    val pawAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pawAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.3f to Color.Black,
                        1.0f to Color(0xFF282828)
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(navEnabled) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = { totalDrag = 0f },
                                onDragCancel = { totalDrag = 0f }
                            ) { _, dragAmount ->
                                totalDrag += dragAmount
                                if (navEnabled && !vm.isTransitioning && abs(totalDrag) > 80f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    vm.advance(if (totalDrag < 0) +1 else -1)
                                    totalDrag = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                        },
                        label = "aphorism"
                    ) { text ->
                        Text(
                            text = text,
                            style = TextStyle(
                                fontFamily = loraFontFamily,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                lineHeight = 34.sp
                            ),
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(text) {
                                    detectTapGestures(onLongPress = {
                                        copyToClipboard(ctx, text)
                                    })
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_paw),
                    contentDescription = "Next",
                    tint = pawPink.copy(alpha = pawAlpha),
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (navEnabled && !vm.isTransitioning) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                vm.advance(+1)
                            }
                        }
                )
            }
        }
    }
}

private data class AphorismsData(val aphorisms: List<String>, val total: Int)

private fun loadAphorisms(ctx: Context): AphorismsData {
    return try {
        val json = ctx.assets.open("void_aphorisms.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("aphorisms")
        val total = obj.getInt("count")
        val list = List(arr.length()) { i ->
            arr.getString(i)
                .replace(Regex("^\\d+\\.\\s*"), "")
                .replace(Regex("^(Sarcastic|Tender|Perk|Benefit)[^:]*:\\s*"), "")
        }.filter { it.isNotEmpty() }
        AphorismsData(list, total)
    } catch (e: Exception) {
        Log.w("MieuApp", "Failed to load void_aphorisms.json: ${e.message}")
        AphorismsData(MieuViewModel.FALLBACK, MieuViewModel.FALLBACK.size)
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("aphorism", text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }
}
