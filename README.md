# GlyphMatrixDraw

Nothing Phone (3) için 25×25 Glyph Matrix çizim uygulaması.

## Kurulum

### 1. SDK'yı ekle
`glyph-matrix-sdk-2.0.aar` dosyasını [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) reposundan indir.
`app/libs/` klasörüne koy.

### 2. Debug modu (Nothing Phone (3) gerekmez geliştirme aşamasında)
```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```
48 saat sonra otomatik kapanır.

### 3. Build
```
./gradlew assembleDebug
```

## Özellikler

| | |
|---|---|
| **Grid Modu** | 25×25 piksel editör, tap/drag ile çiz |
| **Freehand Modu** | Serbest çizim → 25×25 threshold dönüşümü |
| **Grid'e Aktar** | Freehand çizimi grid'e taşı, piksel düzelt |
| **Brightness** | LED yoğunluğu 0–100% |
| **Invert** | Tüm pikselleri ters çevir |
| **Glyph'e Gönder** | `GlyphMatrixManager.setMatrixFrame()` ile doğrudan push |

## Mimari

```
GlyphController       → SDK lifecycle wrapper (init/session/destroy)
PixelGridView         → Custom View, 25×25 bool array, tap+drag input
FreehandCanvasView    → Canvas + Path, bitmap → scale → threshold
MainViewModel         → Mode, pixels, brightness state
MainActivity          → View binding, observer pattern
```

## Notlar

- `minSdk 33` — Android 14 zorunlu (Glyph SDK kısıtı)
- Sadece **foreground** uygulamalar Glyph'e erişebilir
- Production için `AndroidManifest.xml`'deki `"test"` key'i gerçek API key ile değiştir
- Phone (4a) Pro için `Common.DEVICE_23112` → `Common.DEVICE_25111p` ve matrix length 25 → 13
