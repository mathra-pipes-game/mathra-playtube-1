/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import kotlin.math.sin
import kotlin.random.Random

/**
 * The 10 "Emoji Effect" styles the user can pick from in Settings.
 * Each style is just a themed set of emojis that float across the player
 * while a video is playing.
 */
data class EmojiEffectStyle(
    val id: String,
    val displayName: String,
    val emojis: List<String>
)

val EmojiEffectStyles = listOf(
    EmojiEffectStyle("fire", "Fire", listOf("🔥", "✨")),
    EmojiEffectStyle("hearts", "Hearts", listOf("❤️", "💕", "💖")),
    EmojiEffectStyle("stars", "Stars", listOf("⭐", "✨", "🌟")),
    EmojiEffectStyle("notes", "Music Notes", listOf("🎵", "🎶", "🎧")),
    EmojiEffectStyle("rain", "Rain", listOf("💧", "💦")),
    EmojiEffectStyle("confetti", "Confetti", listOf("🎉", "🎊", "🎈")),
    EmojiEffectStyle("bubbles", "Bubbles", listOf("🫧", "🔵")),
    EmojiEffectStyle("lightning", "Lightning", listOf("⚡", "💥")),
    EmojiEffectStyle("snow", "Snow", listOf("❄️", "🌨️")),
    EmojiEffectStyle("sparkle", "Sparkle", listOf("🌟", "💫", "✨")),
    EmojiEffectStyle("funny", "Funny", listOf("😂", "🤣", "😆", "😹", "😝", "🤪", "😜", "🥳", "😁", "😋"))
)

fun emojiStyleById(id: String): EmojiEffectStyle =
    EmojiEffectStyles.firstOrNull { it.id == id } ?: EmojiEffectStyles.first()

private data class EmojiParticle(
    var x: Float,
    var y: Float,
    val emoji: String,
    val baseSize: Float,
    val drift: Float,
    val seed: Float
)

/**
 * Floating/falling emoji particles overlaid on top of the player.
 * When [beatReactive] is true, particle speed pulses from soft to strong
 * (හෙමින් සිට හයියෙන් දක්වා) using a smooth, continuously varying rhythm
 * so the effect never feels static.
 *
 * This intentionally avoids hooking into raw audio FFT data (which would
 * need extra runtime permissions and real-time audio capture); the pulse
 * is a self-contained animation so it works reliably for every video.
 */
@Composable
fun EmojiEffectOverlay(
    enabled: Boolean,
    isPlaying: Boolean,
    style: EmojiEffectStyle,
    beatReactive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 14
) {
    if (!enabled) return

    val particles = remember(style.id) {
        List(particleCount) {
            EmojiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                emoji = style.emojis[Random.nextInt(style.emojis.size)],
                baseSize = Random.nextFloat() * 14f + 18f,
                drift = Random.nextFloat() * 0.6f - 0.3f,
                seed = Random.nextFloat() * 1000f
            )
        }
    }

    var tick by remember { mutableStateOf(0L) }
    var elapsedSeconds by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, style.id) {
        if (!isPlaying) return@LaunchedEffect
        val frameMs = 32L
        while (true) {
            elapsedSeconds += frameMs / 1000f
            tick++
            kotlinx.coroutines.delay(frameMs)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // tick keeps this block re-composing/re-drawing every frame.
            @Suppress("UNUSED_EXPRESSION") tick

            // Soft -> strong -> soft rhythm. Combines two sine waves so the
            // intensity swells unevenly, like a track building up and
            // dropping instead of a plain metronome.
            val rhythm = if (beatReactive) {
                val slow = (sin(elapsedSeconds * 1.1f) + 1f) / 2f
                val fast = (sin(elapsedSeconds * 3.7f) + 1f) / 2f
                0.35f + 0.65f * (0.6f * slow + 0.4f * fast)
            } else {
                0.6f
            }

            val speedPxPerFrame = (0.0015f + rhythm * 0.006f) * size.height
            val paint = Paint().apply {
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            particles.forEach { p ->
                // Advance position; wrap around top<->bottom.
                p.y += (speedPxPerFrame / size.height)
                if (p.y > 1.05f) {
                    p.y = -0.05f
                    p.x = Random.nextFloat()
                }
                val wobble = sin(elapsedSeconds * 1.4f + p.seed) * p.drift * 0.05f
                val drawX = ((p.x + wobble).coerceIn(0f, 1f)) * size.width
                val drawY = p.y * size.height
                val scale = 0.85f + rhythm * 0.35f
                val alpha = (0.55f + rhythm * 0.45f).coerceIn(0f, 1f)

                paint.textSize = p.baseSize * scale
                paint.alpha = (alpha * 255).toInt()
                drawContext.canvas.nativeCanvas.drawText(p.emoji, drawX, drawY, paint)
            }
        }
    }
}
