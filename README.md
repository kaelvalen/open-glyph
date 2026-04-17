# Glyph Draw

A Glyph Toy for Nothing Phone (3) that lets you draw custom pixel art directly on the Glyph Matrix.

---

## What it does

Glyph Draw adds a new toy to your Glyph Toys list. Open the app, draw something on the 25×25 pixel canvas, save it — then select Glyph Draw from the Glyph button on the back of your phone to see your drawing on the matrix.

The canvas mirrors the exact circular shape of the Glyph Matrix, so what you draw is exactly what you see on the back of your phone.

**Features**

- Pixel grid editor — tap or drag to draw, with erase and invert tools
- Freehand mode — draw naturally, converted to pixel art automatically
- Image import — pick a photo or take one with the camera, preview and adjust threshold before converting
- Animation editor — create multi-frame animations with adjustable delay
- Gallery — save and manage multiple patterns, switch between them with the Glyph button
- Brightness control

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
Open the app and use the **GRID** mode to draw pixel by pixel. Tap a pixel to toggle it on or off. Hold and drag to paint continuously. Use **FREE** mode for freehand drawing — your strokes get converted to the pixel grid automatically.

The canvas shows only the pixels that exist on the Glyph Matrix — the circular boundary is accurate to the hardware, so there are no surprises.

### Saving to Glyph
Tap **GLYPH'E GÖNDER** to save your current drawing. Then select Glyph Draw from the Glyph button cycle on the back of your phone.

Long pressing the Glyph button cycles through your saved patterns.

### Animations
Tap **ANİMASYON** from the main screen. Draw each frame, add frames with **+ KARE**, set the delay with the slider, then send to Glyph. The animation loops automatically when the toy is active.

### Image Import
Tap **IMPORT**, pick a photo from your gallery or take one with the camera. Adjust the threshold slider to control which pixels light up, then confirm to send it to the grid.

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