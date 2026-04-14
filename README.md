# Sketch-Pad SDK 🎨

A professional-grade, high-performance, and highly customizable Android drawing library built with **Jetpack Compose**. Designed for infinite creativity, from simple signatures to complex illustrations.

## ✨ Key Features

- **🚀 Infinite Canvas:** Pan and pinch-to-zoom across an endless drawing surface.
- **📏 Canvas Sizes:** Support for standard paper sizes (**A3, A4**), **Screen** size, or **Free/Infinite** mode.
- **🎞️ Robust History:** Complete **Undo/Redo** system for all actions (strokes, erasures, and clearing).
- **🔲 Customizable Grid:** Toggleable background grid with adjustable size and color for precision drawing.
- **🖌️ Smooth Rendering:** Quadratic Bézier curves for natural, pressure-sensitive feel.
- **🧽 Smart Eraser:** Circular eraser cursor with dynamic z-index management.
- **🌈 Professional Color Picker:** Integrated HSV picker for unlimited color selection.
- **📤 High-Res Export:** Generate **ImageBitmaps** with customizable density, background, and grid visibility.
- **🌓 Theme Aware:** Full support for **Dark Mode** with auto-adapting default colors and grid.
- **📱 Broad Compatibility:** Optimized for Android 7.0 (API 24) and above.

## 🚀 Installation

### 1. Add JitPack to your project
In your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency
In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Syedovaiss:Sketch-Pad:<latest-version>")
}
```

## 🛠️ Usage

### Basic Implementation
Add the `SketchPad` composable with zero configuration:

```kotlin
@Composable
fun MyDrawingScreen() {
    SketchPad(
        modifier = Modifier.fillMaxSize(),
        canvasSize = CanvasSize.A4, // Optional: Set a fixed paper size
        gridEnabled = true         // Optional: Show background grid
    )
}
```

### Advanced Configuration
Fine-tune the experience using `SketchToolbarOptions` and `SketchController`:

```kotlin
val controller = remember { SketchController() }

SketchPad(
    controller = controller,
    toolbarOptions = SketchToolbarOptions(
        showSave = true,
        showDownloadImage = true,
        showColorPalette = true
    ),
    onDownloadImage = { strokes ->
        // Generate a high-res bitmap for export
        val bitmap = generateBitmap(
            strokes = strokes,
            width = 2480,  // A4 width at 300 DPI
            height = 3508, // A4 height at 300 DPI
            density = 3f,  // High-DPI scaling
            canvasSize = CanvasSize.A4
        )
        // Save your bitmap to storage...
    }
)
```

### Professional Export (`generateBitmap`)
The SDK provides a powerful headless rendering engine:

```kotlin
val bitmap = generateBitmap(
    strokes = controller.strokes,
    width = 1920,
    height = 1080,
    backgroundColor = Color.White,
    gridEnabled = false,
    canvasSize = CanvasSize.Screen
)
```

## 📋 Requirements
- **Minimum SDK:** 24 (Android 7.0)
- **Jetpack Compose:** 1.5.0+
- **Kotlin:** 1.9.0+

## 📄 License
```text
Copyright 2026 Syed Ovais Akhtar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
