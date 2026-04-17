package com.kaelvalen.glyphmatrixdraw.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityMainBinding
import com.kaelvalen.glyphmatrixdraw.glyph.ActiveState
import com.kaelvalen.glyphmatrixdraw.glyph.GlyphController
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var glyphController: GlyphController

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val pixels = result.data?.getIntArrayExtra(GalleryActivity.EXTRA_PIXELS) ?: return@registerForActivityResult
            val brightness = result.data?.getFloatExtra(GalleryActivity.EXTRA_BRIGHTNESS, 1.0f) ?: 1.0f
            loadPixelsIntoEditor(pixels, brightness)
        }
    }

    private val animationGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Animation gallery is for viewing/editing animations, no return data needed
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val pixels = result.data?.getIntArrayExtra(ImageImportActivity.EXTRA_PIXELS) ?: return@registerForActivityResult
            loadPixelsIntoEditor(pixels, viewModel.brightness.value ?: 1.0f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        glyphController = GlyphController(this)
        setupModeToggle()
        setupGridView()
        setupFreehandView()
        setupControls()
        setupNavButtons()
        observeViewModel()
    }

    override fun onResume() { super.onResume(); glyphController.init() }
    override fun onPause() { super.onPause(); glyphController.destroy() }

    private fun setupModeToggle() {
        binding.btnModeGrid.setOnClickListener { viewModel.switchMode(MainViewModel.DrawMode.GRID) }
        binding.btnModeFreehand.setOnClickListener { viewModel.switchMode(MainViewModel.DrawMode.FREEHAND) }
    }

    private fun setupGridView() {
        binding.pixelGridView.onPixelsChanged = { pixels ->
            viewModel.updatePixels(pixels)
            PixelStore.save(this, pixels, viewModel.brightness.value ?: 1.0f)
        }
    }

    private fun setupFreehandView() {
        binding.freehandCanvasView.onDrawingChanged = { pixels ->
            viewModel.updatePixels(pixels)
            PixelStore.save(this, pixels, viewModel.brightness.value ?: 1.0f)
            binding.pixelGridView.setPixels(pixels)
        }
    }

    private fun setupControls() {
        binding.seekBrightness.max = 100
        binding.seekBrightness.progress = 100
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = progress / 100f
                viewModel.updateBrightness(brightness)
                binding.pixelGridView.brightness = brightness
                binding.tvBrightness.text = getString(R.string.brightness_percent_format, progress)
                PixelStore.save(this@MainActivity, viewModel.currentPixels.value ?: IntArray(625), brightness)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnSend.setOnClickListener {
            val pixels = viewModel.currentPixels.value ?: return@setOnClickListener
            val brightness = viewModel.brightness.value ?: 1.0f
            PixelStore.save(this, pixels, brightness)
            ActiveState.setStatic(this, null)
            if (glyphController.isReady()) glyphController.sendFrame(pixels, brightness)
            Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show()
        }

        binding.btnClear.setOnClickListener {
            binding.pixelGridView.clearAll()
            binding.freehandCanvasView.clear()
            PixelStore.save(this, IntArray(625), 1.0f)
            glyphController.clearMatrix()
        }

        binding.btnErase.setOnClickListener {
            val isErase = !binding.pixelGridView.eraseMode
            binding.pixelGridView.eraseMode = isErase
            binding.btnErase.alpha = if (isErase) 1.0f else 0.5f
            binding.btnErase.text = if (isErase) getString(R.string.draw) else getString(R.string.erase)
        }

        binding.btnInvert.setOnClickListener { binding.pixelGridView.invertAll() }

        binding.btnFill.setOnClickListener { binding.pixelGridView.fillAll() }

        binding.btnApplyFreehand.setOnClickListener {
            val pixels = binding.freehandCanvasView.toPixelArray()
            binding.pixelGridView.setPixels(pixels)
            viewModel.updatePixels(pixels)
            PixelStore.save(this, pixels, viewModel.brightness.value ?: 1.0f)
            viewModel.switchMode(MainViewModel.DrawMode.GRID)
        }

        binding.btnClearFreehand.setOnClickListener {
            binding.freehandCanvasView.clear()
        }
    }

    private fun setupNavButtons() {
        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch(Intent(this, GalleryActivity::class.java))
        }
        binding.btnAnimation.setOnClickListener {
            animationGalleryLauncher.launch(Intent(this, AnimationGalleryActivity::class.java))
        }
        binding.btnImport.setOnClickListener {
            importLauncher.launch(Intent(this, ImageImportActivity::class.java))
        }
    }

    private fun loadPixelsIntoEditor(pixels: IntArray, brightness: Float) {
        binding.pixelGridView.setPixels(pixels)
        viewModel.updatePixels(pixels)
        viewModel.updateBrightness(brightness)
        binding.pixelGridView.brightness = brightness
        val progress = (brightness * 100).toInt()
        binding.seekBrightness.progress = progress
        binding.tvBrightness.text = getString(R.string.brightness_percent_format, progress)
        PixelStore.save(this, pixels, brightness)
        viewModel.switchMode(MainViewModel.DrawMode.GRID)
        Toast.makeText(this, R.string.loaded, Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewModel.currentMode.observe(this) { mode ->
            when (mode) {
                MainViewModel.DrawMode.GRID -> {
                    binding.pixelGridView.visibility = View.VISIBLE
                    binding.freehandCanvasView.visibility = View.GONE
                    binding.gridControls.visibility = View.VISIBLE
                    binding.freehandControls.visibility = View.GONE
                    binding.btnModeGrid.alpha = 1.0f
                    binding.btnModeFreehand.alpha = 0.5f
                }
                MainViewModel.DrawMode.FREEHAND -> {
                    binding.pixelGridView.visibility = View.GONE
                    binding.freehandCanvasView.visibility = View.VISIBLE
                    binding.gridControls.visibility = View.GONE
                    binding.freehandControls.visibility = View.VISIBLE
                    binding.btnModeGrid.alpha = 0.5f
                    binding.btnModeFreehand.alpha = 1.0f
                }
                null -> {}
            }
        }
    }
}
