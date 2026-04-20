# Glyph Draw

A Glyph Toy for Nothing Phone (3) that lets you draw custom pixel art directly on the Glyph Matrix.

---

## What it does

Glyph Draw adds a new toy to your Glyph Toys list. Open the app, draw something on the 25×25 pixel canvas, save it — then select Glyph Draw from the Glyph button on the back of your phone to see your drawing on the matrix.

The canvas mirrors the exact circular shape of the Glyph Matrix, so what you draw is exactly what you see on the back of your phone.

**Features (v2)**

- Full drawing toolkit — pen, eraser, fill bucket, line, rectangle, disc, circle, dropper
- Symmetry modes — horizontal, vertical, 4‑way and 8‑way (radial) mirroring
- Transforms — flip, rotate 90°, shift, invert
- Undo / redo with 40‑step history
- Grayscale brush with per‑pixel intensity
- Freehand canvas — draws naturally and quantizes into pixel art
- Text mode — 3x5 bitmap font with static centring or scrolling marquee (auto‑exported as an animation)
- Effects generator — breathing, pulse, wave, shimmer, spiral, starfield, rain, heartbeat; uses the current drawing as a seed
- Image import — multi‑algorithm dither (threshold, Floyd‑Steinberg, Atkinson, Bayer) plus grayscale, rotate, contrast/brightness and invert
- Animation editor — onion skin, drag‑to‑reorder frames, duplicate frames, ping‑pong playback, live preview
- Gallery — favourites, rename, share as PNG, per‑pattern metadata, empty‑state guidance
- Backup — single‑file `.glyph` export/import covering all patterns and animations
- Glyph service — cycles saved patterns with the Glyph button, plays back animations in straight or ping‑pong mode
- Full English UI with optional Turkish localization

---

## Installation

1. Download the APK from the releases page
2. Install it on your Nothing Phone (3) — you may need to allow installation from unknown sources
3. Open the app and draw something
4. Go to **Settings → Glyph Interface → Glyph Toys** and add **Glyph Draw** to your active toys
5. Press the Glyph button on the back of your phone to activate it

---

## Usage

### Drawing
Open the app and use the **GRID** mode to draw pixel by pixel. Pick a tool from the palette (pen, eraser, fill, line, rect, disc, circle, picker), optionally enable a symmetry mode, and tap or drag on the canvas. Use **FREE** mode for freehand — your strokes are rendered into the pixel grid automatically. The intensity slider controls pen grayscale.

The canvas shows only the pixels that exist on the Glyph Matrix — the circular boundary is accurate to the hardware, so there are no surprises.

### Sending to Glyph
Tap **SEND TO GLYPH** to publish your current drawing to the Glyph Matrix toy. Then select Glyph Draw from the Glyph button on the back of your phone.

Pressing the Glyph button cycles through your saved patterns.

### Animations
Tap **ANIMATION** from the main screen. Draw each frame, add new ones with **+ FRAME**, optionally enable **ONION** to see the previous frame faded beneath the current one, drag frames in the strip to reorder them, adjust the delay slider, and toggle **PING‑PONG** for a bouncing loop. Hit play to preview, then send to Glyph.

### Text
Tap **TEXT** to render a typed message in the 3x5 bitmap font. Static mode centres the text on the matrix; scroll mode packages it as a looping marquee animation.

### Effects
Tap **FX** to generate animations procedurally — breathing, pulse, wave, shimmer, spiral, starfield, rain and heartbeat. You can seed effects with your current drawing and tune frame count and delay before saving.

### Image Import
Tap **IMPORT**, pick a photo from your gallery or take one with the camera. Choose a dither algorithm (gray, 1‑bit, Floyd, Atkinson, Bayer), adjust threshold and contrast, rotate or invert as needed, then apply to the grid.

### Gallery and backup
The gallery keeps your patterns sorted with favourites first. Use the ⋯ button on any pattern for rename, share or delete. **EXPORT BACKUP** writes a single `.glyph` JSON file that contains every saved pattern and animation; **IMPORT BACKUP** restores it on another device.

---

## Building from source

Requires Android SDK, Java 21, and the GlyphMatrix SDK AAR.

```bash
# Clone the repo
git clone https://github.com/kaelvalen/glyph-draw
cd glyph-draw

# Download the SDK AAR from Nothing's developer kit and place it here:
# app/libs/glyph-matrix-sdk-2.0.aar
# https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit

# Set your SDK path
echo "sdk.dir=/path/to/android-sdk" > local.properties

# Build
./gradlew assembleDebug
```

---

## Device support

| Device | Status |
|--------|--------|
| Nothing Phone (3) | ✓ Supported |
| Nothing Phone (4a) Pro | Not tested |
| Other Nothing phones | Not supported (no Glyph Matrix) |

---

## Credits

Built by [Kael Valen](https://github.com/kaelvalen) using the [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) by Nothing.

The circular pixel mask is derived directly from Nothing's official spec SVG to ensure pixel-perfect accuracy with the hardware.

---

*Share your creations on [nothing.community](https://nothing.community)*

And If you like this app, leave a star for future development!