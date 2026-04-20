package com.kaelvalen.glyphmatrixdraw.glyph

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Procedural animation generators. Each returns a list of 625-pixel frames
 * with values 0..255, already clamped to the circular mask.
 *
 * The effects are grouped into four display categories by [Kind.category]:
 *   AMBIENT, RADIAL, GEOMETRIC, PARTICLES
 * so the Effects UI can render them in neatly labelled sections.
 */
object EffectGenerator {

    enum class Category { AMBIENT, RADIAL, GEOMETRIC, PARTICLES }

    /**
     * All available effects. Keep the enum order deterministic — the UI relies
     * on it for stable button positions.
     */
    enum class Kind(val category: Category) {
        // Ambient
        BREATHING(Category.AMBIENT),
        PULSE(Category.AMBIENT),
        FADE(Category.AMBIENT),
        SHIMMER(Category.AMBIENT),
        PLASMA(Category.AMBIENT),

        // Radial
        WAVE(Category.RADIAL),
        RIPPLE(Category.RADIAL),
        CLOCK_PULSE(Category.RADIAL),     // displayed as "Heartbeat"
        ORBIT(Category.RADIAL),
        LOADING(Category.RADIAL),

        // Geometric
        SPIRAL(Category.GEOMETRIC),
        CHECKERBOARD(Category.GEOMETRIC),
        SCANLINE(Category.GEOMETRIC),
        BOUNCE(Category.GEOMETRIC),

        // Particles
        STARFIELD(Category.PARTICLES),
        RAIN(Category.PARTICLES),
        MATRIX_RAIN(Category.PARTICLES),
        FIRE(Category.PARTICLES),
        FIREWORK(Category.PARTICLES),
        LIGHTNING(Category.PARTICLES),
    }

    /**
     * Produce frames for the named effect.
     * [basePixels] is consumed by BREATHING / PULSE / SHIMMER / FADE as the
     * shape to animate. If the caller passes null (or an empty frame) a
     * filled circle is used instead.
     */
    fun generate(
        kind: Kind,
        frames: Int = 24,
        basePixels: IntArray? = null,
    ): List<IntArray> {
        val count = frames.coerceAtLeast(2)
        val base = basePixels?.takeIf { it.any { v -> v > 0 } } ?: solidCircle()
        return when (kind) {
            Kind.BREATHING    -> breathing(base, count)
            Kind.PULSE        -> pulse(base, count)
            Kind.FADE         -> fade(base, count)
            Kind.SHIMMER      -> shimmer(base, count)
            Kind.PLASMA       -> plasma(count)

            Kind.WAVE         -> wave(count)
            Kind.RIPPLE       -> ripple(count)
            Kind.CLOCK_PULSE  -> clockPulse(count)
            Kind.ORBIT        -> orbit(count)
            Kind.LOADING      -> loading(count)

            Kind.SPIRAL       -> spiral(count)
            Kind.CHECKERBOARD -> checkerboard(count)
            Kind.SCANLINE     -> scanline(count)
            Kind.BOUNCE       -> bounce(count)

            Kind.STARFIELD    -> starfield(count)
            Kind.RAIN         -> rain(count)
            Kind.MATRIX_RAIN  -> matrixRain(count)
            Kind.FIRE         -> fire(count)
            Kind.FIREWORK     -> firework(count)
            Kind.LIGHTNING    -> lightning(count)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private const val CENTER = (GlyphMask.SIZE - 1) / 2f

    private fun solidCircle(): IntArray {
        val out = IntArray(GlyphMask.COUNT)
        GlyphMask.forEachActive { _, _, i -> out[i] = 255 }
        return out
    }

    private fun newFrame() = IntArray(GlyphMask.COUNT)

    // ─── Ambient effects ──────────────────────────────────────────────────

    /** Sinusoidal brightness modulation of a base pattern. */
    private fun breathing(base: IntArray, count: Int): List<IntArray> = List(count) { i ->
        val t = i.toFloat() / count * 2f * PI.toFloat()
        val k = 0.15f + ((sin(t) + 1f) / 2f) * 0.85f
        val out = newFrame()
        for (j in 0 until GlyphMask.COUNT) out[j] = (base[j] * k).toInt().coerceIn(0, 255)
        GlyphMask.clamp(out)
    }

    /** Quick on/off pulse — 70% duty cycle, sharper than breathing. */
    private fun pulse(base: IntArray, count: Int): List<IntArray> = List(count) { i ->
        val phase = i.toFloat() / count
        val k = if (phase < 0.6f) 1f else 0.25f
        val out = newFrame()
        for (j in 0 until GlyphMask.COUNT) out[j] = (base[j] * k).toInt().coerceIn(0, 255)
        GlyphMask.clamp(out)
    }

    /** Smooth fade in/out with a linger at peak, easier on the eye than breathing. */
    private fun fade(base: IntArray, count: Int): List<IntArray> = List(count) { i ->
        val t = i.toFloat() / count
        // piecewise: 0→1 for first 40%, hold 20%, 1→0 for last 40%
        val k = when {
            t < 0.4f -> t / 0.4f
            t < 0.6f -> 1f
            else -> 1f - (t - 0.6f) / 0.4f
        }
        val out = newFrame()
        for (j in 0 until GlyphMask.COUNT) out[j] = (base[j] * k).toInt().coerceIn(0, 255)
        GlyphMask.clamp(out)
    }

    /** Random sparkle over the base pattern. */
    private fun shimmer(base: IntArray, count: Int): List<IntArray> {
        val rng = java.util.Random(42)
        return List(count) {
            val out = newFrame()
            for (j in 0 until GlyphMask.COUNT) {
                if (base[j] == 0) continue
                val noise = 0.4f + rng.nextFloat() * 0.6f
                out[j] = (base[j] * noise).toInt().coerceIn(0, 255)
            }
            GlyphMask.clamp(out)
        }
    }

    /** Interference of two sine waves — soft plasma colours (grayscale here). */
    private fun plasma(count: Int): List<IntArray> = List(count) { i ->
        val t = i.toFloat() / count * 2f * PI.toFloat()
        val out = newFrame()
        GlyphMask.forEachActive { col, row, idx ->
            val x = col / GlyphMask.SIZE.toFloat() * 2f - 1f
            val y = row / GlyphMask.SIZE.toFloat() * 2f - 1f
            val v = sin(x * 3f + t) + sin(y * 3f + t * 0.7f) +
                sin((x + y) * 2.5f + t * 0.9f) + sin(hypot(x, y) * 4f - t)
            // v is in [-4..4], normalise
            val k = (v + 4f) / 8f
            out[idx] = (k * 255f).toInt().coerceIn(0, 255)
        }
        out
    }

    // ─── Radial effects ───────────────────────────────────────────────────

    /** Radial wave expanding from the centre then resetting. */
    private fun wave(count: Int): List<IntArray> {
        val maxR = hypot(CENTER, CENTER)
        return List(count) { i ->
            val r = (i.toFloat() / count) * maxR
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot(col - CENTER, row - CENTER)
                val band = 1.8f - kotlin.math.abs(d - r)
                val v = (max(0f, band) / 1.8f).let { s -> s * s * 255f }
                out[idx] = v.toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Multiple concentric waves — like rain drops on water. */
    private fun ripple(count: Int): List<IntArray> {
        val maxR = hypot(CENTER, CENTER)
        return List(count) { i ->
            val phase = i.toFloat() / count
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot(col - CENTER, row - CENTER)
                val w = sin((d / maxR) * PI.toFloat() * 4f - phase * 2f * PI.toFloat())
                val k = ((w + 1f) / 2f)
                // fade outer rings to avoid clipping at the edge
                val attenuate = 1f - (d / maxR).coerceIn(0f, 1f) * 0.35f
                out[idx] = (k * attenuate * 255f).toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Beating heart-style pulse on the centre of the matrix. */
    private fun clockPulse(count: Int): List<IntArray> {
        val maxR = 10f
        val thickness = 1.5f
        return List(count) { i ->
            val t = i.toFloat() / count
            val beat = kotlin.math.abs(sin(t * PI.toFloat() * 2f))
            val r = 2f + beat * (maxR - 2f)
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot(col - CENTER, row - CENTER)
                val edge = 1f - (kotlin.math.abs(d - r) / thickness).coerceIn(0f, 1f)
                out[idx] = (edge * 255f).toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Single bright dot orbiting at a fixed radius. */
    private fun orbit(count: Int): List<IntArray> {
        val radius = 9f
        return List(count) { i ->
            val a = i.toFloat() / count * 2f * PI.toFloat()
            val x = CENTER + radius * cos(a)
            val y = CENTER + radius * sin(a)
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot(col - x, row - y)
                val k = max(0f, 1.6f - d) / 1.6f
                out[idx] = (k * 255f).toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Rotating arc — classic loading spinner. */
    private fun loading(count: Int): List<IntArray> {
        val radius = 10f
        val thickness = 1.5f
        val arc = PI.toFloat() * 0.8f // ~144°
        return List(count) { i ->
            val start = i.toFloat() / count * 2f * PI.toFloat()
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val dx = col - CENTER
                val dy = row - CENTER
                val d = hypot(dx, dy)
                if (kotlin.math.abs(d - radius) > thickness) return@forEachActive
                val theta = (atan2(dy, dx) - start + 4f * PI.toFloat()) % (2f * PI.toFloat())
                if (theta > arc) return@forEachActive
                val headFade = 1f - theta / arc           // head bright, tail dim
                val edgeFade = 1f - kotlin.math.abs(d - radius) / thickness
                out[idx] = (headFade * edgeFade * 255f).toInt().coerceIn(0, 255)
            }
            out
        }
    }

    // ─── Geometric effects ────────────────────────────────────────────────

    /** Rotating spiral arms. */
    private fun spiral(count: Int): List<IntArray> = List(count) { i ->
        val phase = i.toFloat() / count * 2f * PI.toFloat()
        val out = newFrame()
        GlyphMask.forEachActive { col, row, idx ->
            val dx = col - CENTER
            val dy = row - CENTER
            val d = hypot(dx, dy)
            val theta = atan2(dy, dx)
            val arm = cos(theta * 2f + d * 0.6f - phase)
            out[idx] = (((arm + 1f) / 2f) * 255f).toInt().coerceIn(0, 255)
        }
        out
    }

    /** 5x5 super-cell checkerboard that alternates each frame. */
    private fun checkerboard(count: Int): List<IntArray> {
        val cell = 5
        return List(count) { i ->
            val invert = (i / max(1, count / 2)) % 2 == 0
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val c = (col / cell + row / cell) % 2
                val on = if (invert) c == 0 else c == 1
                out[idx] = if (on) 255 else 0
            }
            out
        }
    }

    /** A vertical line sweeping left → right → left. */
    private fun scanline(count: Int): List<IntArray> = List(count) { i ->
        val t = (sin((i.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f) + 1f) / 2f
        val x = t * (GlyphMask.SIZE - 1)
        val out = newFrame()
        GlyphMask.forEachActive { col, row, idx ->
            val d = kotlin.math.abs(col - x)
            val k = max(0f, 1.8f - d) / 1.8f
            out[idx] = (k * 255f).toInt().coerceIn(0, 255)
        }
        out
    }

    /** A 3x3 ball bouncing between the edges of the mask. */
    private fun bounce(count: Int): List<IntArray> = List(count) { i ->
        val t = i.toFloat() / count
        val x = CENTER + 10f * cos(t * 2f * PI.toFloat())
        val y = CENTER + 7f  * sin(t * 4f * PI.toFloat())
        val out = newFrame()
        GlyphMask.forEachActive { col, row, idx ->
            val d = hypot(col - x, row - y)
            val k = max(0f, 1.8f - d) / 1.8f
            out[idx] = (k * 255f).toInt().coerceIn(0, 255)
        }
        out
    }

    // ─── Particle effects ─────────────────────────────────────────────────

    /** Random persistent stars blinking. */
    private fun starfield(count: Int): List<IntArray> {
        val rng = java.util.Random(11)
        val activeIndices = mutableListOf<Int>()
        GlyphMask.forEachActive { _, _, i -> activeIndices += i }
        data class Star(val idx: Int, val phase: Float, val speed: Float)
        val stars = List(40) {
            Star(
                idx = activeIndices[rng.nextInt(activeIndices.size)],
                phase = rng.nextFloat(),
                speed = 0.5f + rng.nextFloat() * 1.5f,
            )
        }
        return List(count) { i ->
            val t = i.toFloat() / count
            val out = newFrame()
            for (s in stars) {
                val phase = (s.phase + t * s.speed) % 1f
                val k = (sin(phase * 2f * PI.toFloat()) + 1f) / 2f
                val idx = s.idx
                out[idx] = max(out[idx], (k * 255f).toInt().coerceIn(0, 255))
            }
            out
        }
    }

    /** Falling drops on each column. */
    private fun rain(count: Int): List<IntArray> {
        val rng = java.util.Random(7)
        val drops = (0 until GlyphMask.SIZE).associateWith {
            List(2) { rng.nextFloat() }
        }
        return List(count) { i ->
            val t = i.toFloat() / count
            val out = newFrame()
            for (col in 0 until GlyphMask.SIZE) {
                for (p in drops[col]!!) {
                    val y = ((p + t * 2f) * GlyphMask.SIZE) % GlyphMask.SIZE.toFloat()
                    for (trail in 0 until 4) {
                        val yy = (y - trail).toInt()
                        if (yy in 0 until GlyphMask.SIZE && GlyphMask.isActive(col, yy)) {
                            val v = (255 - trail * 70).coerceIn(0, 255)
                            val idx = yy * GlyphMask.SIZE + col
                            if (out[idx] < v) out[idx] = v
                        }
                    }
                }
            }
            out
        }
    }

    /** Matrix-style digital rain — taller trails, individual column speeds. */
    private fun matrixRain(count: Int): List<IntArray> {
        val rng = java.util.Random(31)
        data class Col(val offset: Float, val speed: Float, val trail: Int)
        val cols = List(GlyphMask.SIZE) {
            Col(
                offset = rng.nextFloat(),
                speed = 0.6f + rng.nextFloat() * 1.2f,
                trail = 5 + rng.nextInt(5), // 5..9
            )
        }
        return List(count) { i ->
            val t = i.toFloat() / count
            val out = newFrame()
            for (col in 0 until GlyphMask.SIZE) {
                val c = cols[col]
                val head = ((c.offset + t * c.speed) * GlyphMask.SIZE) % GlyphMask.SIZE.toFloat()
                for (trail in 0 until c.trail) {
                    val yy = (head - trail).toInt()
                    if (yy in 0 until GlyphMask.SIZE && GlyphMask.isActive(col, yy)) {
                        val decay = (1f - trail.toFloat() / c.trail).let { it * it }
                        val v = (decay * 255f).toInt().coerceIn(0, 255)
                        val idx = yy * GlyphMask.SIZE + col
                        if (out[idx] < v) out[idx] = v
                    }
                }
            }
            out
        }
    }

    /** Flickering flame climbing from the bottom of the mask. */
    private fun fire(count: Int): List<IntArray> {
        val rng = java.util.Random(91)
        return List(count) { i ->
            val t = i.toFloat() / count * 2f * PI.toFloat()
            val out = newFrame()
            GlyphMask.forEachActive { col, row, idx ->
                val up = (GlyphMask.SIZE - 1 - row).toFloat() / (GlyphMask.SIZE - 1)
                val flick = 0.8f + 0.2f * sin(t + col * 0.6f + row * 0.3f)
                val jitter = 0.8f + rng.nextFloat() * 0.2f
                val v = (up * up * flick * jitter) * 255f
                out[idx] = v.toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Rocket launching from the bottom and exploding outward at the centre. */
    private fun firework(count: Int): List<IntArray> {
        val launch = (count * 0.45f).toInt().coerceAtLeast(1)
        val maxR = hypot(CENTER, CENTER)
        return List(count) { i ->
            val out = newFrame()
            if (i < launch) {
                // vertical line climbing up the centre column
                val y = (GlyphMask.SIZE - 1) - (i.toFloat() / launch) * (GlyphMask.SIZE - 1 - CENTER)
                GlyphMask.forEachActive { col, row, idx ->
                    if (col != CENTER.toInt()) return@forEachActive
                    val d = kotlin.math.abs(row - y)
                    if (d > 2f) return@forEachActive
                    val k = 1f - d / 2f
                    out[idx] = (k * 255f).toInt().coerceIn(0, 255)
                }
            } else {
                val p = (i - launch).toFloat() / (count - launch)
                val r = p * maxR
                val fade = 1f - p
                GlyphMask.forEachActive { col, row, idx ->
                    val d = hypot(col - CENTER, row - CENTER)
                    val band = 1.6f - kotlin.math.abs(d - r)
                    val k = (max(0f, band) / 1.6f) * fade
                    out[idx] = (k * 255f).toInt().coerceIn(0, 255)
                }
            }
            out
        }
    }

    /** Sudden bright bolt followed by a lingering glow. */
    private fun lightning(count: Int): List<IntArray> {
        val rng = java.util.Random(123)
        // Pre-generate one jagged bolt path down the matrix
        val path = IntArray(GlyphMask.SIZE)
        var col = CENTER.toInt()
        for (row in path.indices) {
            path[row] = col.coerceIn(0, GlyphMask.SIZE - 1)
            col += rng.nextInt(3) - 1
        }
        return List(count) { i ->
            val t = i.toFloat() / count
            val out = newFrame()
            val flash = when {
                t < 0.1f -> t / 0.1f                    // sharp attack
                t < 0.25f -> 1f - (t - 0.1f) / 0.15f    // quick decay
                t < 0.35f -> 0.6f * ((0.35f - t) / 0.1f) // secondary flash
                else -> 0f
            }
            if (flash > 0f) {
                for (row in 0 until GlyphMask.SIZE) {
                    val cCol = path[row]
                    for (dx in -1..1) {
                        val c = cCol + dx
                        if (c in 0 until GlyphMask.SIZE && GlyphMask.isActive(c, row)) {
                            val idx = row * GlyphMask.SIZE + c
                            val k = flash * if (dx == 0) 1f else 0.5f
                            val v = (k * 255f).toInt().coerceIn(0, 255)
                            if (out[idx] < v) out[idx] = v
                        }
                    }
                }
            }
            // ambient low glow so the matrix isn't completely black between strikes
            val ambient = (min(1f, (1f - t)) * 18f).toInt()
            if (ambient > 0) {
                GlyphMask.forEachActive { _, _, idx ->
                    if (out[idx] < ambient) out[idx] = ambient
                }
            }
            out
        }
    }

    // ─── Misc ─────────────────────────────────────────────────────────────

    /** Convenience: clock digits rendered with BitmapFont, one frame per minute tick. */
    fun clockFrame(hour: Int, minute: Int): IntArray {
        val out = IntArray(GlyphMask.COUNT)
        val h = hour.coerceIn(0, 23).toString().padStart(2, '0')
        val m = minute.coerceIn(0, 59).toString().padStart(2, '0')
        val topW = BitmapFont.measure(h); val botW = BitmapFont.measure(m)
        BitmapFont.draw(out, h, (GlyphMask.SIZE - topW) / 2, 7)
        BitmapFont.draw(out, m, (GlyphMask.SIZE - botW) / 2, 13)
        return GlyphMask.clamp(out)
    }

    @Suppress("UNUSED") private fun roundAndClamp(v: Float): Int = v.roundToInt().coerceIn(0, 255)
}
