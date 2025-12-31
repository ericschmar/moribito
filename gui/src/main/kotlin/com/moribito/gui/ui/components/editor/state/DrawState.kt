package com.moribito.gui.ui.components.editor.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import com.moribito.gui.ui.components.editor.highlighting.HighlightLayer

/**
 * Manages z-ordered highlight layers for the code editor.
 *
 * This class maintains a collection of named highlight layers (syntax, errors, search, etc.)
 * and provides them in z-order for rendering. Higher z-index layers are rendered last,
 * appearing on top of lower layers.
 *
 * Example usage:
 * ```
 * val drawState = DrawState()
 * drawState.addLayer("syntax", SyntaxHighlightLayer(text))
 * drawState.addLayer("errors", ErrorHighlightLayer(errors))
 * val layers = drawState.getLayers() // Returns sorted by z-index
 * ```
 */
@Stable
class DrawState {
    /**
     * Map of layer IDs to highlight layers.
     * Using mutableStateMapOf ensures Compose tracks changes to the map.
     */
    private val layers = mutableStateMapOf<String, HighlightLayer>()

    /**
     * Add or replace a highlight layer.
     *
     * @param id Unique identifier for this layer (e.g., "syntax", "errors", "search")
     * @param layer The highlight layer to add
     */
    fun addLayer(id: String, layer: HighlightLayer) {
        layers[id] = layer
    }

    /**
     * Remove a highlight layer by ID.
     *
     * @param id The layer identifier to remove
     */
    fun removeLayer(id: String) {
        layers.remove(id)
    }

    /**
     * Get all highlight layers sorted by z-index.
     *
     * Layers with lower z-index values come first (rendered first/bottom),
     * layers with higher z-index values come last (rendered last/top).
     *
     * @return List of highlight layers in rendering order
     */
    fun getLayers(): List<HighlightLayer> {
        return layers.values.sorted()
    }

    /**
     * Check if a specific layer exists.
     *
     * @param id The layer identifier to check
     * @return true if the layer exists, false otherwise
     */
    fun hasLayer(id: String): Boolean {
        return layers.containsKey(id)
    }

    /**
     * Clear all layers.
     */
    fun clearLayers() {
        layers.clear()
    }

    /**
     * Get the number of active layers.
     */
    val layerCount: Int
        get() = layers.size
}
