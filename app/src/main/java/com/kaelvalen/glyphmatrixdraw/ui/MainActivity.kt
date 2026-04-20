package com.kaelvalen.glyphmatrixdraw.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kaelvalen.glyphmatrixdraw.R
import com.kaelvalen.glyphmatrixdraw.databinding.ActivityMainBinding
import com.kaelvalen.glyphmatrixdraw.glyph.ActiveState
import com.kaelvalen.glyphmatrixdraw.glyph.GlyphController
import com.kaelvalen.glyphmatrixdraw.glyph.Pattern
import com.kaelvalen.glyphmatrixdraw.glyph.PatternStore
import com.kaelvalen.glyphmatrixdraw.glyph.PixelStore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var glyphController: GlyphController

    private val toolButtons = mutableMapOf<DrawTool, TextView>()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val pixels = result.data?.getIntArrayExtra(GalleryActivity.EXTRA_PIXELS) ?: return@registerForActivityResult
            val brightness = result.data?.getFloatExtra(GalleryActivity.EXTRA_BRIGHTNESS, 1.0f) ?: 1.0f
            loadPixelsIntoEditor(pixels, brightness)
        }
    }

    private val animationGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val textLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val pixels = result.data?.getIntArrayExtra(TextActivity.EXTRA_PIXELS) ?: return@registerForActivityResult
            loadPixelsIntoEditor(pixels, viewModel.brightness.value ?: 1.0f)
        }
    }
    private val effectsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

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

        setupToolPalette()
        setupModeToggle()
        setupGridView()
        setupFreehandView()
        setupControls()
        setupNavButtons()
        setupUndoRedo()
        setupTransforms()
        observeViewModel()

        // Restore last drawing so the session continues where the user left off.
        val saved = PixelStore.loadPixels(this)
        val savedBrightness = PixelStore.loadBrightness(this)
        if (saved.any { it > 0 }) {
            binding.pixelGridView.setPixels(saved, pushToUndo = false)
            viewModel.updatePixels(saved)
            viewModel.updateBrightness(savedBrightness)
            binding.pixelGridView.brightness = savedBrightness
            binding.seekBrightness.progress = (savedBrightness * 100).toInt()
        }
    }

    override fun onResume() { super.onResume(); glyphController.init() }
    override fun onPause() { super.onPause(); glyphController.destroy() }

    private fun setupToolPalette() {
        val tools = listOf(
            DrawTool.PEN to R.string.tool_pen,
            DrawTool.ERASER to R.string.tool_eraser,
            DrawTool.FILL to R.string.tool_fill,
            DrawTool.LINE to R.string.tool_line,
            DrawTool.RECT to R.string.tool_rect,
            DrawTool.RECT_FILL to R.string.tool_rect_fill,
            DrawTool.CIRCLE to R.string.tool_circle,
            DrawTool.CIRCLE_FILL to R.string.tool_circle_fill,
            DrawTool.DROPPER to R.string.tool_dropper,
        )
        val density = resources.displayMetrics.density
        for ((tool, labelRes) in tools) {
            val tv = TextView(this).apply {
                text = getString(labelRes)
                textSize = 10f
                setTextColor(0xFFCCCCCC.toInt())
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
                letterSpacing = 0.12f
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.tool_bg)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (34 * density).toInt()).apply {
                marginEnd = (6 * density).toInt()
            }
            binding.toolRow.addView(tv, lp)
            tv.setOnClickListener { selectTool(tool) }
            toolButtons[tool] = tv
        }
        selectTool(DrawTool.PEN)
    }

    private fun selectTool(tool: DrawTool) {
        binding.pixelGridView.tool = tool
        for ((t, view) in toolButtons) {
            val selected = t == tool
            view.background = ContextCompat.getDrawable(this, if (selected) R.drawable.tool_bg_selected else R.drawable.tool_bg)
            view.setTextColor(if (selected) 0xFF080808.toInt() else 0xFFCCCCCC.toInt())
        }
    }

    private fun setupModeToggle() {
        binding.btnModeGrid.setOnClickListener { viewModel.switchMode(MainViewModel.DrawMode.GRID) }
        binding.btnModeFreehand.setOnClickListener { viewModel.switchMode(MainViewModel.DrawMode.FREEHAND) }
    }

    private fun setupGridView() {
        binding.pixelGridView.onPixelsChanged = { pixels ->
            viewModel.updatePixels(pixels)
            PixelStore.save(this, pixels, viewModel.brightness.value ?: 1.0f)
            refreshUndoRedo()
        }
        binding.pixelGridView.onPick = { picked ->
            val p = picked.coerceIn(0, 255)
            binding.pixelGridView.penValue = if (p == 0) 255 else p
            binding.seekIntensity.progress = binding.pixelGridView.penValue
            if (binding.pixelGridView.tool == DrawTool.DROPPER) selectTool(DrawTool.PEN)
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

        binding.seekIntensity.max = 255
        binding.seekIntensity.progress = 255
        binding.seekIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = progress.coerceIn(1, 255)
                binding.pixelGridView.penValue = v
                binding.tvIntensity.text = getString(R.string.intensity_value_format, v)
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

        binding.btnSaveGallery.setOnClickListener { promptSaveToGallery() }

        binding.btnClear.setOnClickListener {
            binding.pixelGridView.clearAll()
            binding.freehandCanvasView.clear()
            PixelStore.save(this, IntArray(625), viewModel.brightness.value ?: 1.0f)
            glyphController.clearMatrix()
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

        binding.btnClearFreehand.setOnClickListener { binding.freehandCanvasView.clear() }
    }

    private fun setupUndoRedo() {
        binding.btnUndo.setOnClickListener {
            if (binding.pixelGridView.undo()) refreshUndoRedo()
        }
        binding.btnRedo.setOnClickListener {
            if (binding.pixelGridView.redo()) refreshUndoRedo()
        }
        refreshUndoRedo()
    }

    private fun refreshUndoRedo() {
        binding.btnUndo.alpha = if (binding.pixelGridView.canUndo) 1f else 0.35f
        binding.btnRedo.alpha = if (binding.pixelGridView.canRedo) 1f else 0.35f
    }

    private fun setupTransforms() {
        binding.btnSymmetry.setOnClickListener {
            val next = when (binding.pixelGridView.symmetry) {
                Symmetry.NONE -> Symmetry.HORIZONTAL
                Symmetry.HORIZONTAL -> Symmetry.VERTICAL
                Symmetry.VERTICAL -> Symmetry.BOTH
                Symmetry.BOTH -> Symmetry.QUAD
                Symmetry.QUAD -> Symmetry.NONE
            }
            binding.pixelGridView.symmetry = next
            binding.btnSymmetry.text = getString(
                when (next) {
                    Symmetry.NONE -> R.string.sym_none
                    Symmetry.HORIZONTAL -> R.string.sym_h
                    Symmetry.VERTICAL -> R.string.sym_v
                    Symmetry.BOTH -> R.string.sym_both
                    Symmetry.QUAD -> R.string.sym_quad
                }
            )
        }

        binding.btnFlipH.setOnClickListener { binding.pixelGridView.flipH() }
        binding.btnFlipV.setOnClickListener { binding.pixelGridView.flipV() }
        binding.btnRotate.setOnClickListener { binding.pixelGridView.rotateCW() }
        binding.btnShiftLeft.setOnClickListener { binding.pixelGridView.shift(-1, 0) }
        binding.btnShiftRight.setOnClickListener { binding.pixelGridView.shift(1, 0) }
        binding.btnShiftUp.setOnClickListener { binding.pixelGridView.shift(0, -1) }
        binding.btnShiftDown.setOnClickListener { binding.pixelGridView.shift(0, 1) }
    }

    private fun setupNavButtons() {
        binding.btnGallery.setOnClickListener { galleryLauncher.launch(Intent(this, GalleryActivity::class.java)) }
        binding.btnAnimation.setOnClickListener { animationGalleryLauncher.launch(Intent(this, AnimationGalleryActivity::class.java)) }
        binding.btnImport.setOnClickListener { importLauncher.launch(Intent(this, ImageImportActivity::class.java)) }
        binding.btnText.setOnClickListener { textLauncher.launch(Intent(this, TextActivity::class.java)) }
        binding.btnEffects.setOnClickListener { effectsLauncher.launch(Intent(this, EffectsActivity::class.java)) }
    }

    private fun promptSaveToGallery() {
        val current = viewModel.currentPixels.value ?: return
        val brightness = viewModel.brightness.value ?: 1.0f
        val input = EditText(this).apply {
            hint = getString(R.string.pattern_name_hint)
            setText(getString(R.string.pattern_name_format, PatternStore.getAll(this@MainActivity).size + 1))
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.save_current_drawing)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().ifBlank { "Pattern" }
                PatternStore.save(this, Pattern(PatternStore.newId(), name, current.clone(), brightness))
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        refreshUndoRedo()
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
                    binding.toolScroll.visibility = View.VISIBLE
                    binding.transformScroll.visibility = View.VISIBLE
                    binding.intensityRow.visibility = View.VISIBLE
                    binding.btnModeGrid.setTextColor(0xFF000000.toInt())
                    binding.btnModeGrid.background = ContextCompat.getDrawable(this, R.drawable.tab_selected)
                    binding.btnModeFreehand.setTextColor(0xFF666666.toInt())
                    binding.btnModeFreehand.background = null
                }
                MainViewModel.DrawMode.FREEHAND -> {
                    binding.pixelGridView.visibility = View.GONE
                    binding.freehandCanvasView.visibility = View.VISIBLE
                    binding.gridControls.visibility = View.GONE
                    binding.freehandControls.visibility = View.VISIBLE
                    binding.toolScroll.visibility = View.GONE
                    binding.transformScroll.visibility = View.GONE
                    binding.intensityRow.visibility = View.GONE
                    binding.btnModeGrid.setTextColor(0xFF666666.toInt())
                    binding.btnModeGrid.background = null
                    binding.btnModeFreehand.setTextColor(0xFF000000.toInt())
                    binding.btnModeFreehand.background = ContextCompat.getDrawable(this, R.drawable.tab_selected)
                }
                null -> {}
            }
        }
    }
}
