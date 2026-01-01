package com.moribito.gui.ui.components.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSizes
import com.moribito.ldap.TreeNode
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontallyScrollableContainer
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer

/**
 * Data class to hold the layout information for a node.
 */
data class NodeLayout(
    val node: TreeNode,
    val position: Offset,
    val children: List<NodeLayout>
)

/**
 * Recursively shifts all node positions by the given offset.
 */
private fun normalizeLayout(layout: NodeLayout, offsetX: Float, offsetY: Float): NodeLayout {
    return NodeLayout(
        node = layout.node,
        position = Offset(layout.position.x - offsetX, layout.position.y - offsetY),
        children = layout.children.map { normalizeLayout(it, offsetX, offsetY) }
    )
}

/**
 * A graph view for the directory tree.
 */
@Composable
fun DirectoryGraphView(
    rootNode: TreeNode?,
    onNodeClick: (TreeNode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (rootNode == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Connect to a server to view the directory graph")
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Simple tree layout (includes UID grouping)
    val graphData = remember(rootNode) {
        val rawLayout = calculateLayout(rootNode, 0f, 0f, 150f, 120f)

        var minX = 0f
        var minY = 0f
        fun findMin(l: NodeLayout) {
            minX = minOf(minX, l.position.x)
            minY = minOf(minY, l.position.y)
            l.children.forEach { findMin(it) }
        }
        findMin(rawLayout)

        normalizeLayout(rawLayout, minX, minY)
    }

    // Calculate bounds of the graph
    val bounds = remember(graphData) {
        var minX = 0f
        var maxX = 0f
        var minY = 0f
        var maxY = 0f
        
        fun updateBounds(layout: NodeLayout) {
            minX = minOf(minX, layout.position.x)
            maxX = maxOf(maxX, layout.position.x)
            minY = minOf(minY, layout.position.y)
            maxY = maxOf(maxY, layout.position.y)
            layout.children.forEach { updateBounds(it) }
        }
        
        updateBounds(graphData)
        // Add some margin
        Rect(minX - 200f, minY - 100f, maxX + 200f, maxY + 300f)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val viewportWidthDp = maxWidth
        val viewportHeightDp = maxHeight
        
        // Calculate the virtual content size based on bounds and scale (in Dp)
        val contentWidthDp = (bounds.width.dp * scale).coerceAtLeast(viewportWidthDp)
        val contentHeightDp = (bounds.height.dp * scale).coerceAtLeast(viewportHeightDp)
        
        // Initial scroll to center if needed
        LaunchedEffect(graphData) {
            horizontalScrollState.scrollTo(
                with(density) { ((contentWidthDp - viewportWidthDp) / 2).toPx().toInt() }
            )
        }

        // Background dots move with scroll
        DotBackground(
            offset = Offset(
                -horizontalScrollState.value.toFloat() / scale,
                -verticalScrollState.value.toFloat() / scale
            ),
            scale = scale
        )

        VerticallyScrollableContainer(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            scrollState = verticalScrollState
        ) {
            HorizontallyScrollableContainer(
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                scrollState = horizontalScrollState
            ) {
                Box(
                    modifier = Modifier
                        .size(contentWidthDp, contentHeightDp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.2f, 3f)
                                coroutineScope.launch {
                                    horizontalScrollState.scrollBy(-pan.x)
                                    verticalScrollState.scrollBy(-pan.y)
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = (contentWidthDp.toPx() / 2f) - (bounds.center.x.dp.toPx() * scale)
                                translationY = (contentHeightDp.toPx() / 2f) - (bounds.center.y.dp.toPx() * scale)
                            }
                    ) {
                        // Draw edges
                        EdgeCanvas(graphData)
                        
                        // Draw nodes
                        DrawNodes(graphData, onNodeClick)
                    }
                }
            }
        }
    }
}

private fun calculateLayout(
    node: TreeNode,
    x: Float,
    y: Float,
    horizontalSpacing: Float,
    verticalSpacing: Float
): NodeLayout {
    val childrenLayouts = mutableListOf<NodeLayout>()
    val rawChildren = node.children ?: emptyList()
    
    // Group UIDs if there are many of them
    val uids = rawChildren.filter { it.dn.startsWith("uid=", ignoreCase = true) }
    val others = rawChildren.filter { !it.dn.startsWith("uid=", ignoreCase = true) }
    
    val children = if (uids.size > 3) {
        val groupedNode = TreeNode(
            dn = "group:uids:${node.dn}",
            name = "Users (${uids.size})",
            children = emptyList(),
            isLoaded = true
        )
        listOf(groupedNode) + others
    } else {
        rawChildren
    }
    
    if (children.isNotEmpty()) {
        val totalWidth = (children.size - 1) * horizontalSpacing
        var currentX = x - totalWidth / 2
        
        for (child in children) {
            childrenLayouts.add(calculateLayout(child, currentX, y + verticalSpacing, horizontalSpacing, verticalSpacing))
            currentX += horizontalSpacing
        }
    }
    
    return NodeLayout(node, Offset(x, y), childrenLayouts)
}

private val NodeHeight = AppSizes.buttonHeightLarge

@Composable
private fun DrawNodes(
    layout: NodeLayout,
    onNodeClick: (TreeNode) -> Unit
) {
    GraphNode(
        name = layout.node.name,
        isLoaded = layout.node.isLoaded,
        canHaveChildren = layout.node.hasChildren(),
        modifier = Modifier
            .offset(x = layout.position.x.dp, y = layout.position.y.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    // Center horizontally by shifting left by half the width
                    placeable.place(-placeable.width / 2, 0)
                }
            }
            .height(NodeHeight)
            .defaultMinSize(minWidth = NodeHeight),
        onClick = { onNodeClick(layout.node) }
    )
    
    layout.children.forEach { child ->
        DrawNodes(child, onNodeClick)
    }
}

@Composable
private fun EdgeCanvas(layout: NodeLayout) {
    val edgeColor = Color(0xFF666666) // Brighter, more prominent edge color
    val density = LocalDensity.current
    
    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        with(density) {
            drawEdgesRecursive(layout, edgeColor)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEdgesRecursive(
    layout: NodeLayout,
    color: Color
) {
    // Start from the bottom middle of the current node
    val startPx = Offset(
        layout.position.x.dp.toPx(),
        (layout.position.y.dp + NodeHeight).toPx()
    )
    
    layout.children.forEach { child ->
        // End at the top middle of the child node
        val endPx = Offset(
            child.position.x.dp.toPx(),
            child.position.y.dp.toPx()
        )
        
        drawLine(
            color = color,
            start = startPx,
            end = endPx,
            strokeWidth = 3.dp.toPx()
        )
        drawEdgesRecursive(child, color)
    }
}
