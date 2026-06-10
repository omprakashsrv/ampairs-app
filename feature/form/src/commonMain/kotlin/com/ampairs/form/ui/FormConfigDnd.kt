package com.ampairs.form.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Minimal drag-and-drop engine for the form editor (handle-driven, mirrors the design prototype).
 *
 * Rows/sections register their root-space vertical bounds via [registerBounds]; a drag started from a
 * handle tracks an absolute [pointerY]; insertion indices are derived by comparing pointerY against
 * row centers. Cross-section field moves are supported (target = section whose bounds contain the
 * pointer). Commit is delegated to the screen via the onDrop lambdas passed to the handle modifiers.
 */
@Stable
class FormDndState {
    var dragFieldUid by mutableStateOf<String?>(null)
        private set
    var dragSectionUid by mutableStateOf<String?>(null)
        private set
    var pointerY by mutableStateOf(0f)
        private set

    /** key: "f:<uid>" for field rows, "s:<uid>" for whole section cards. value: top..bottom in root. */
    private val bounds = mutableStateMapOf<String, Pair<Float, Float>>()

    val isDraggingField get() = dragFieldUid != null
    val isDraggingSection get() = dragSectionUid != null

    fun registerBounds(key: String, top: Float, bottom: Float) {
        bounds[key] = top to bottom
    }

    fun startField(uid: String) {
        dragFieldUid = uid
        pointerY = bounds["f:$uid"]?.let { (it.first + it.second) / 2f } ?: 0f
    }

    fun startSection(uid: String) {
        dragSectionUid = uid
        pointerY = bounds["s:$uid"]?.let { (it.first + it.second) / 2f } ?: 0f
    }

    fun dragBy(dy: Float) {
        pointerY += dy
    }

    fun clear() {
        dragFieldUid = null
        dragSectionUid = null
    }

    private fun center(key: String): Float? = bounds[key]?.let { (it.first + it.second) / 2f }

    /** Section whose card bounds contain the pointer (target for a field drop). */
    fun hoveredSection(orderedSectionUids: List<String>): String? {
        if (!isDraggingField) return null
        return orderedSectionUids.firstOrNull { uid ->
            bounds["s:$uid"]?.let { pointerY >= it.first && pointerY <= it.second } == true
        }
    }

    /**
     * Insertion index for the dragged field inside [sectionUid], or null when the pointer isn't over
     * that section. Index counts rows (excluding the dragged one) whose center is above the pointer.
     */
    fun fieldInsertionIn(sectionUid: String, orderedFieldUids: List<String>): Int? {
        val dragging = dragFieldUid ?: return null
        val sec = bounds["s:$sectionUid"] ?: return null
        if (pointerY < sec.first || pointerY > sec.second) return null
        return orderedFieldUids.filter { it != dragging }
            .count { (center("f:$it") ?: Float.MAX_VALUE) < pointerY }
    }

    /** Insertion index for the dragged section among [orderedSectionUids] (excluding itself). */
    fun sectionInsertion(orderedSectionUids: List<String>): Int? {
        val dragging = dragSectionUid ?: return null
        return orderedSectionUids.filter { it != dragging }
            .count { (center("s:$it") ?: Float.MAX_VALUE) < pointerY }
    }
}

/** Registers an element's vertical bounds (root space) under [key]. */
fun Modifier.dndBounds(dnd: FormDndState, key: String): Modifier = onGloballyPositioned { c ->
    val top = c.positionInRoot().y
    dnd.registerBounds(key, top, top + c.size.height)
}

/** Drag-handle behavior for a field row. [onDrop] fires with the dragged uid after a completed drag. */
fun Modifier.fieldDragHandle(dnd: FormDndState, fieldUid: String, onDrop: (String) -> Unit): Modifier =
    pointerInput(fieldUid) {
        detectDragGestures(
            onDragStart = { dnd.startField(fieldUid) },
            onDrag = { change, amount -> change.consume(); dnd.dragBy(amount.y) },
            onDragEnd = { onDrop(fieldUid); dnd.clear() },
            onDragCancel = { dnd.clear() },
        )
    }

/** Drag-handle behavior for a section card. */
fun Modifier.sectionDragHandle(dnd: FormDndState, sectionUid: String, onDrop: (String) -> Unit): Modifier =
    pointerInput(sectionUid) {
        detectDragGestures(
            onDragStart = { dnd.startSection(sectionUid) },
            onDrag = { change, amount -> change.consume(); dnd.dragBy(amount.y) },
            onDragEnd = { onDrop(sectionUid); dnd.clear() },
            onDragCancel = { dnd.clear() },
        )
    }
