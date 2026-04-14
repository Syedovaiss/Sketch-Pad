# Sketch-Pad SDK 🎨

A powerful, lightweight, and highly customizable Android drawing library built with **Jetpack Compose**. Perfect for apps requiring sketch features, signatures, or whiteboard functionality.

## ✨ Features

- **Smooth Drawing:** Uses quadratic Bézier curves for high-quality, smooth stroke rendering.
- **Undo/Redo Support:** Fully integrated history management via `SketchController`.
- **Eraser Tool:** Smart erasure logic with adjustable thresholds.
- **Dark Mode Support:** Automatically adapts its theme and default colors for Dark Mode.
- **Color Picker:** Integrated HSV Color Picker (via Skydoves) for unlimited color choices.
- **Export Options:** Easily save your sketches as **PNG** images or **PDF** documents.
- **ViewModel Ready:** State is decoupled from the UI, making it compatible with ViewModels and configuration changes.
- **Highly Customizable:** Override icons, colors, and dialog texts to match your app's branding.

## 🚀 Installation

### 1. Add JitPack to your project
In your `settings.gradle.kts` (or `build.gradle` at the root):

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
Add this to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Syedovaiss:Sketch-Pad:<latest-version>")
}
```

## 🛠️ Usage

### Basic Implementation
Simply add the `SketchPad` composable to your UI:

```kotlin
@Composable
fun MyDrawingScreen() {
    SketchPad(
        modifier = Modifier.fillMaxSize(),
        onSave = { strokes ->
            // Handle saved strokes (List<ActiveStroke>)
        }
    )
}
```

### Using the Controller
For programmatic control over undo, redo, and clearing the canvas:

```kotlin
val controller = remember { SketchController() }

Column {
    Row {
        Button(onClick = { controller.undo() }) { Text("Undo") }
        Button(onClick = { controller.redo() }) { Text("Redo") }
    }

    SketchPad(
        controller = controller,
        modifier = Modifier.weight(1f)
    )
}
```

### Headless Canvas (`SketchCanvas`)
If you want to build your own custom toolbar and only need the drawing surface, use `SketchCanvas`:

```kotlin
val controller = remember { SketchController() }

Box(modifier = Modifier.fillMaxSize()) {
    SketchCanvas(
        controller = controller,
        modifier = Modifier.fillMaxSize(),
        onStrokeStarted = { /* Logic when drawing starts */ },
        onStrokeEnded = { stroke -> /* Logic when stroke is finished */ }
    )
    
    // Add your custom buttons, sliders, etc. here
}
```

### Exporting Content
You can use the built-in utility functions to save the sketch:

```kotlin
val context = LocalContext.current

SketchPad(
    onSave = { strokes ->
        // 1. Export as PDF
        exportAndSavePdf(context, strokes)
        
        // 2. Export as PNG
        val bitmap = exportImage(strokes)
        saveImageToGallery(context, bitmap)
    }
)
```

## 🎨 Customization

### Custom Icons & Dialogs
You can customize the UI by passing `SketchPadIcons` or `SketchPadDialogs`:

```kotlin
SketchPad(
    icons = SketchPadIcons(
        drawIcon = R.drawable.my_custom_draw,
        eraseIcon = R.drawable.my_custom_erase
    ),
    dialogs = SketchPadDialogs(
        clearTitle = "Delete everything?",
        confirmText = "Yes, Clear"
    )
)
```

## 📋 Requirements
- **Minimum SDK:** 26
- **Jetpack Compose:** 1.4.0+
- **Kotlin Serialization:** Required if you plan to persist stroke data.

## 📄 License
```text
Copyright 2026 Syed Ovais Akhtar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
