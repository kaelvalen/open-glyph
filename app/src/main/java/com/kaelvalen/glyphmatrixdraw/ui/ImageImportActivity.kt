package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityImageImportBinding
import java.io.File

class ImageImportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIXELS = "pixels"
        const val FILE_PROVIDER_AUTHORITY = "com.kaelvalen.glyphmatrixdraw.fileprovider"
    }

    private lateinit var binding: ActivityImageImportBinding
    private var sourceBitmap: Bitmap? = null
    private var cameraUri: Uri? = null
    private var dither: ImageProcessing.Dither = ImageProcessing.Dither.GRAYSCALE
    private val ditherButtons = mutableMapOf<ImageProcessing.Dither, TextView>()
    private var invert = false

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { loadUri(it) } }
    private val cameraPicker = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { loadUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnImportBack.setOnClickListener { finish() }
        binding.btnPickGallery.setOnClickListener { galleryPicker.launch("image/*") }
        binding.btnPickCamera.setOnClickListener { launchCamera() }
        binding.btnImportConfirm.setOnClickListener { confirm() }
        binding.btnRotate.setOnClickListener {
            sourceBitmap?.let { sourceBitmap = ImageProcessing.rotate90(it); redraw() }
        }
        binding.btnInvert.setOnClickListener {
            invert = !invert
            binding.btnInvert.background = ContextCompat.getDrawable(this, if (invert) R.drawable.chip_bg_selected else R.drawable.chip_bg)
            binding.btnInvert.setTextColor(if (invert) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
            redraw()
        }

        buildDitherRow()
        selectDither(ImageProcessing.Dither.GRAYSCALE)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) { updateLabels(); redraw() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        binding.seekThreshold.setOnSeekBarChangeListener(listener)
        binding.seekBrightnessImport.setOnSeekBarChangeListener(listener)
        binding.seekContrast.setOnSeekBarChangeListener(listener)
        updateLabels()
    }

    private fun buildDitherRow() {
        val entries = listOf(
            ImageProcessing.Dither.GRAYSCALE to R.string.dither_grayscale,
            ImageProcessing.Dither.THRESHOLD to R.string.dither_threshold,
            ImageProcessing.Dither.FLOYD_STEINBERG to R.string.dither_floyd,
            ImageProcessing.Dither.ATKINSON to R.string.dither_atkinson,
            ImageProcessing.Dither.BAYER to R.string.dither_bayer,
        )
        val density = resources.displayMetrics.density
        for ((d, labelRes) in entries) {
            val tv = TextView(this).apply {
                text = getString(labelRes)
                textSize = 10f
                setTextColor(0xFFCCCCCC.toInt())
                gravity = Gravity.CENTER
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.12f
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                background = ContextCompat.getDrawable(this@ImageImportActivity, R.drawable.tool_bg)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (32 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
            binding.ditherRow.addView(tv, lp)
            tv.setOnClickListener { selectDither(d) }
            ditherButtons[d] = tv
        }
    }

    private fun selectDither(d: ImageProcessing.Dither) {
        dither = d
        for ((key, view) in ditherButtons) {
            val sel = key == d
            view.background = ContextCompat.getDrawable(this, if (sel) R.drawable.tool_bg_selected else R.drawable.tool_bg)
            view.setTextColor(if (sel) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        }
        redraw()
    }

    private fun launchCamera() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "import_tmp.jpg")
        cameraUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file)
        cameraPicker.launch(cameraUri!!)
    }

    private fun loadUri(uri: Uri) {
        val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        sourceBitmap = cropToSquare(bmp)
        binding.tvImportHint.visibility = View.GONE
        binding.btnImportConfirm.isEnabled = true
        binding.btnImportConfirm.alpha = 1f
        redraw()
    }

    private fun redraw() {
        val bmp = sourceBitmap ?: return
        val scaled = Bitmap.createScaledBitmap(bmp, 25, 25, true)
        val threshold = binding.seekThreshold.progress / 100f
        val brightness = binding.seekBrightnessImport.progress / 100f
        val contrast = (binding.seekContrast.progress / 100f).coerceAtLeast(0.1f)
        val pixels = ImageProcessing.process(scaled, dither, threshold, brightness, contrast, invert)
        com.kaelvalen.glyphmatrixdraw.glyph.GlyphMask.clamp(pixels)
        scaled.recycle()
        binding.importPixelGrid.setPixels(pixels, pushToUndo = false)
    }

    private fun confirm() {
        val pixels = binding.importPixelGrid.pixels.clone()
        val intent = Intent().putExtra(EXTRA_PIXELS, pixels)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun updateLabels() {
        val b = binding.seekBrightnessImport.progress / 100f
        binding.tvBrightnessImportVal.text = getString(R.string.brightness_value_format, b)
        binding.tvThresholdVal.text = getString(R.string.threshold_value_format, binding.seekThreshold.progress)
        val c = (binding.seekContrast.progress / 100f).coerceAtLeast(0.1f)
        binding.tvContrastVal.text = getString(R.string.contrast_value_format, c)
    }

    private fun cropToSquare(bmp: Bitmap): Bitmap {
        val size = minOf(bmp.width, bmp.height)
        val x = (bmp.width - size) / 2
        val y = (bmp.height - size) / 2
        return Bitmap.createBitmap(bmp, x, y, size, size)
    }
}
