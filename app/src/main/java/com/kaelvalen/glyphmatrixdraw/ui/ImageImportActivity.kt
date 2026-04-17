package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadUri(it) }
    }

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

        val reditherListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLabels()
                sourceBitmap?.let { redraw(it) }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        binding.seekThreshold.setOnSeekBarChangeListener(reditherListener)
        binding.seekBrightnessImport.setOnSeekBarChangeListener(reditherListener)
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
        redraw(sourceBitmap!!)
        binding.tvImportHint.visibility = View.GONE
        binding.btnImportConfirm.isEnabled = true
    }

    private fun redraw(bmp: Bitmap) {
        val brightness = binding.seekBrightnessImport.progress / 100f
        val scaled = Bitmap.createScaledBitmap(bmp, 25, 25, true)

        // Grayscale mode: each LED gets brightness from source pixel intensity
        val pixels = FloydSteinberg.grayscale(scaled, brightness)
        scaled.recycle()
        binding.importPixelGrid.setPixels(pixels)
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
    }

    private fun cropToSquare(bmp: Bitmap): Bitmap {
        val size = minOf(bmp.width, bmp.height)
        val x = (bmp.width - size) / 2
        val y = (bmp.height - size) / 2
        return Bitmap.createBitmap(bmp, x, y, size, size)
    }
}
