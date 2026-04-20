package com.kaelvalen.glyphmatrixdraw.ui

/**
 * Simple fixed-size undo/redo stack for 625-pixel snapshots.
 * A "commit" pushes a new state and clears the redo branch.
 */
class UndoStack(private val maxDepth: Int = 40) {
    private val undo = ArrayDeque<IntArray>()
    private val redo = ArrayDeque<IntArray>()

    val canUndo get() = undo.size > 1
    val canRedo get() = redo.isNotEmpty()

    /** Seed the stack with the initial state. Must be called once before commits. */
    fun reset(initial: IntArray) {
        undo.clear(); redo.clear()
        undo.addLast(initial.clone())
    }

    /** Push a new state onto the undo stack. */
    fun commit(state: IntArray) {
        undo.addLast(state.clone())
        while (undo.size > maxDepth) undo.removeFirst()
        redo.clear()
    }

    fun undo(): IntArray? {
        if (undo.size <= 1) return null
        redo.addLast(undo.removeLast())
        return undo.last().clone()
    }

    fun redo(): IntArray? {
        if (redo.isEmpty()) return null
        val next = redo.removeLast()
        undo.addLast(next)
        return next.clone()
    }
}
