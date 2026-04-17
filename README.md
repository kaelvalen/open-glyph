# GlyphMatrixDraw

A 25x25 Glyph Matrix drawing app for Nothing Phone (3).

## Setup

### 1. Add the SDK
Download `glyph-matrix-sdk-2.0.aar` from the [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) repository.
Place it in `app/libs/`.

### 2. Debug mode (Nothing Phone (3) not required during development)
```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```
Automatically disables after 48 hours.

### 3. Build
```
./gradlew assembleDebug
```

## Features

| | |
|---|---|
| **Grid Mode** | Draw in a 25x25 pixel editor using tap/drag |
| **Freehand Mode** | Free drawing -> 25x25 threshold conversion |
| **Apply to Grid** | Move freehand drawing into the grid for pixel-level fixes |
| **Brightness** | LED intensity 0-100% |
| **Invert** | Invert all pixels |
| **Send to Glyph** | Direct push with `GlyphMatrixManager.setMatrixFrame()` |

## Mimari

```
GlyphController       → SDK lifecycle wrapper (init/session/destroy)
PixelGridView         → Custom View, 25×25 bool array, tap+drag input
FreehandCanvasView    → Canvas + Path, bitmap → scale → threshold
MainViewModel         → Mode, pixels, brightness state
MainActivity          → View binding, observer pattern
```

## Notes

- `minSdk 33` - Android 14 is required (Glyph SDK constraint)
- Only **foreground** apps can access Glyph
- For production, replace the `"test"` key in `AndroidManifest.xml` with a real API key
- For Phone (4a) Pro, switch `Common.DEVICE_23112` -> `Common.DEVICE_25111p` and matrix length 25 -> 13
