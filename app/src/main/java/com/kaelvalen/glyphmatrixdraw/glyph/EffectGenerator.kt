package com.kaelvalen.glyphmatrixdraw.glyph

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Procedural animation generators. Each returns a list of 625-pixel frames
 * with values 0..255, already clamped to the circular mask.
 */
object EffectGenerator {

    enum class Kind { BREATHING, PULSE, WAVE, SHIMMER, SPIRAL, STARFIELD, RAIN, CLOCK_PULSE }

    /**
     * Produce frames for the named effect.
     * [basePixels] is used as the shape for BREATHING/PULSE/SHIMMER — if empty,
     * a default filled circle is used.
     */
    fun generate(
        kind: Kind,
        frames: Int = 24,
        basePixels: IntArray? = null,
    ): List<IntArray> = when (kind) {
        Kind.BREATHING -> breathing(basePixels ?: solidCircle(), frames)
        Kind.PULSE     -> pulse(basePixels ?: solidCircle(), frames)
        Kind.WAVE      -> wave(frames)
        Kind.SHIMMER   -> shimmer(basePixels ?: solidCircle(), frames)
        Kind.SPIRAL    -> spiral(frames)
        Kind.STARFIELD -> starfield(frames)
        Kind.RAIN      -> rain(frames)
        Kind.CLOCK_PULSE -> clockPulse(frames)
    }

    private fun solidCircle(): IntArray {
        val out = IntArray(GlyphMask.COUNT)
        GlyphMask.forEachActive { _, _, i -> out[i] = 255 }
        return out
    }

    /** Sinusoidal brightness modulation of a base pattern. */
    private fun breathing(base: IntArray, count: Int): List<IntArray> {
        return (0 until count).map { i ->
            val t = i.toFloat() / count * 2f * Math.PI.toFloat()
            val k = (sin(t) + 1f) / 2f
            val scaled = 0.15f + k * 0.85f
            val out = IntArray(GlyphMask.COUNT)
            for (j in 0 until GlyphMask.COUNT) out[j] = (base[j] * scaled).toInt().coerceIn(0, 255)
            GlyphMask.clamp(out)
        }
    }

    /** Quick on/off pulse — 70% duty cycle, sharper than breathing. */
    private fun pulse(base: IntArray, count: Int): List<IntArray> {
        return (0 until count).map { i ->
            val phase = i.toFloat() / count
            val on = phase < 0.6f
            val k = if (on) 1f else 0.25f
            val out = IntArray(GlyphMask.COUNT)
            for (j in 0 until GlyphMask.COUNT) out[j] = (base[j] * k).toInt().coerceIn(0, 255)
            GlyphMask.clamp(out)
        }
    }

    /** Radial wave expanding from the centre then resetting. */
    private fun wave(count: Int): List<IntArray> {
        val cx = (GlyphMask.SIZE - 1) / 2f
        val cy = cx
        val maxR = hypot(cx, cy)
        return (0 until count).map { i ->
            val t = i.toFloat() / count
            val r = t * maxR
            val out = IntArray(GlyphMask.COUNT)
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot((col - cx), (row - cy))
                val band = 1.8f - kotlin.math.abs(d - r)
                val v = (max(0f, band) / 1.8f).let { s -> (s * s * 255f) }
                out[idx] = v.toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Random sparkle over the base pattern. */
    private fun shimmer(base: IntArray, count: Int): List<IntArray> {
        val rng = java.util.Random(42)
        return (0 until count).map {
            val out = IntArray(GlyphMask.COUNT)
            for (j in 0 until GlyphMask.COUNT) {
                if (base[j] == 0) { out[j] = 0; continue }
                val noise = 0.4f + rng.nextFloat() * 0.6f
                out[j] = (base[j] * noise).toInt().coerceIn(0, 255)
            }
            GlyphMask.clamp(out)
        }
    }

    /** Rotating spiral arm. */
    private fun spiral(count: Int): List<IntArray> {
        val cx = (GlyphMask.SIZE - 1) / 2f
        val cy = cx
        return (0 until count).map { i ->
            val phase = i.toFloat() / count * 2f * Math.PI.toFloat()
            val out = IntArray(GlyphMask.COUNT)
            GlyphMask.forEachActive { col, row, idx ->
                val dx = col - cx
                val dy = row - cy
                val d = hypot(dx, dy)
                val theta = kotlin.math.atan2(dy, dx)
                val arm = cos(theta * 2f + d * 0.6f - phase)
                val v = ((arm + 1f) / 2f) * 255f
                out[idx] = v.toInt().coerceIn(0, 255)
            }
            out
        }
    }

    /** Random persistent stars blinking. */
    private fun starfield(count: Int): List<IntArray> {
        val rng = java.util.Random(11)
        data class Star(val idx: Int, val phase: Float, val speed: Float)
        val activeIndices = mutableListOf<Int>()
        GlyphMask.forEachActive { _, _, i -> activeIndices += i }
        val stars = (0 until 40).map {
            val idx = activeIndices[rng.nextInt(activeIndices.size)]
            Star(idx, rng.nextFloat(), 0.5f + rng.nextFloat() * 1.5f)
        }
        return (0 until count).map { i ->
            val t = i.toFloat() / count
            val out = IntArray(GlyphMask.COUNT)
            for (s in stars) {
                val phase = (s.phase + t * s.speed) % 1f
                val k = (sin(phase * 2f * Math.PI.toFloat()) + 1f) / 2f
                out[s.idx] = max(out[s.idx], (k * 255f).toInt().coerceIn(0, 255))
            }
            out
        }
    }

    /** Falling drops on each column. */
    private fun rain(count: Int): List<IntArray> {
        val rng = java.util.Random(7)
        val drops = (0 until GlyphMask.SIZE).associateWith {
            (0 until 2).map { rng.nextFloat() }
        }
        return (0 until count).map { i ->
            val t = i.toFloat() / count
            val out = IntArray(GlyphMask.COUNT)
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

    /** Beating heart-like pulse on the centre of the matrix. */
    private fun clockPulse(count: Int): List<IntArray> {
        val cx = (GlyphMask.SIZE - 1) / 2f
        val cy = cx
        val maxR = 10f
        return (0 until count).map { i ->
            val t = i.toFloat() / count
            val beat = kotlin.math.abs(sin(t * Math.PI.toFloat() * 2f))
            val r = 2f + beat * (maxR - 2f)
            val thickness = 1.5f
            val out = IntArray(GlyphMask.COUNT)
            GlyphMask.forEachActive { col, row, idx ->
                val d = hypot(col - cx, row - cy)
                val edge = 1f - (kotlin.math.abs(d - r) / thickness).coerceIn(0f, 1f)
                out[idx] = (edge * 255f).toInt().coerceIn(0, 255)
            }
            out
        }
    }

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
