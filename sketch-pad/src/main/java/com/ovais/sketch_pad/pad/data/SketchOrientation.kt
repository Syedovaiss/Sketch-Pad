package com.ovais.sketch_pad.pad.data

/**
 * Controls how the SketchPad UI arranges its chrome (toolbar, overlays)
 * relative to the canvas, independent of system screen orientation.
 */
enum class SketchOrientation {
    /**
     * Automatically choose based on current device orientation.
     */
    Auto,

    /**
     * Force portrait-style layout (toolbar at the top).
     */
    Portrait,

    /**
     * Force landscape-style layout (toolbar as a side rail).
     */
    Landscape
}

