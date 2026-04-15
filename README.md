# Sketch-Pad SDK 🎨

<p align="left">
  <a href="https://jitpack.io/#Syedovaiss/Sketch-Pad"><img src="https://jitpack.io/v/Syedovaiss/Sketch-Pad.svg" alt="JitPack"></a>
  <img src="https://img.shields.io/badge/version-latest-green" alt="Version">
  <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build">
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue" alt="License"></a>
</p>

A professional-grade, high-performance, and highly customizable Android drawing library built with **Jetpack Compose**. Designed for infinite creativity, from simple signatures to complex illustrations.

## 📺 Demo

### [**🎥 Click to Watch the Demo Video (demo.webm)**](./demo.webm)

<p align="center">
  <video width="100%" height="auto" controls autoplay loop muted>
    <source src="./demo.webm" type="video/webm">
    Your browser does not support the video tag.
  </video>
  <br/>
  <em>Note: If the video player is not rendering in your IDE, please click the link above to open the video file directly.</em>
</p>

## ✨ Key Features

- **🚀 GPU-Accelerated Performance:** Silky smooth panning and zooming using `graphicsLayer` and intelligent `Path` caching.
- **📏 Flexible Canvas:** Support for standard paper sizes (**A3, A4**), **Screen** size, or **Free/Infinite** mode.
- **🎨 Dynamic Theme Sync:** Automatic ink visibility adjustment (white turns black in light mode and vice versa) and theme-aware grids.
- **🎞️ Robust History:** Complete **Undo/Redo** system for all actions (strokes, erasures, and clearing).
- **🔲 Precision Grid:** Toggleable background grid with adjustable size and color.
- **🖌️ Smooth Rendering:** Quadratic Bézier curves for a natural drawing feel.
- **🧽 Smart Eraser:** Circular eraser cursor with proximity-based stroke removal.
- **🌈 Professional Color Picker:** Integrated HSV picker for unlimited color selection.
- **📤 Pro-Grade Export:** Export your work as **High-Res Images**, **PDFs**, **SVGs**, or **JSON** for later editing.
- **💾 Session Management:** Easily save and load sketch data to/from local storage or databases.
- **🎛️ Customizable Toolbar Selection:** Configure selected tool highlight color using `toolbarSelectionColor`.

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
    // Check the JitPack badge at the top for the latest version
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
        gridEnabled = true,         // Optional: Show background grid
        toolbarSelectionColor = Color(0xFFFF6F00) // Optional: selected tool color
    )
}
```

### 🛠️ Professional Usage

#### ViewModel Integration (Best Practice)
For the best UX, host the `SketchController` in your `ViewModel` to survive configuration changes (like theme switching or rotation):

```kotlin
class SketchViewModel : ViewModel() {
    val controller = SketchController()
    
    // Manage your save/load logic here...
}

@Composable
fun SketchScreen(viewModel: SketchViewModel) {
    SketchPad(
        controller = viewModel.controller,
        onSave = { strokes -> viewModel.saveDraft(strokes) }
    )
}
```

#### Professional Exporting
Exporting to multiple formats is built-in via `SketchExporter`:

```kotlin
val file = SketchExporter.exportToFile(
    context = context,
    strokes = controller.strokes,
    canvasSize = CanvasSize.A4,
    fileType = SketchFileType.PDF, // Or SVG, JSON, etc.
    backgroundColor = Color.White
)
```

### ⚡ Performance Optimization
The SDK is built for speed:
- **`graphicsLayer`**: Used for zero-lag panning/zooming.
- **Path Caching**: Strokes are cached as `Path` objects in the controller to avoid redundant allocations.
- **Contrast Correction**: Exported files automatically adjust stroke colors to ensure visibility on the chosen background.

## 📝 Recent Improvements
- Fixed draw coordinate alignment after switching from move mode back to draw mode.
- Fixed eraser cursor position mismatch after canvas translation.
- Added `toolbarSelectionColor` to customize selected tool highlight color.
- Internal sample app migrated from Hilt to Koin for dependency injection.

## 📋 Requirements
- **Minimum SDK:** 24 (Android 7.0)
- **Jetpack Compose:** Modern Compose stack (BOM based).
- **Kotlin:** 2.2.0+

## 📄 License
```text
Copyright 2026 Syed Ovais Akhtar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
