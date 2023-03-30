package com.makingmadness.dreamquill

class UndoRedoManager {
    private var undoStack = mutableListOf<String>()
    private var redoStack = mutableListOf<String>()
    private var inUndoRedoMode = false
    val isUserTyping: Boolean
        get() = !inUndoRedoMode

    fun addUndoableOperation(text: String) {
        if (!inUndoRedoMode) {
            undoStack.add(text)
            redoStack.clear()
        }
    }

    fun toggleUndoRedo(updateInputEditText: (String) -> Unit, updateUndoButtonText: (String) -> Unit, currentText: (String)) {
        inUndoRedoMode = true
        if (redoStack.isEmpty()) {
            performUndo(updateInputEditText, updateUndoButtonText, currentText)
        } else {
            performRedo(updateInputEditText, updateUndoButtonText, currentText)
        }
        inUndoRedoMode = false
    }

    private fun performUndo(updateInputEditText: (String) -> Unit, updateUndoButtonText: (String) -> Unit, currentText: (String)) {
        if (undoStack.isNotEmpty()) {
            val lastText = undoStack.removeLast()
            updateInputEditText (
                currentText.removeSuffix("\n\n$lastText\n\n")
            )
            redoStack.add(lastText)
            updateUndoButtonText("Redo")
        }
    }

    private fun performRedo(updateInputEditText: (String) -> Unit, updateUndoButtonText: (String) -> Unit, currentText: (String)) {
        if (redoStack.isNotEmpty()) {
            val lastText = redoStack.removeLast()
            updateInputEditText (
                "$currentText\n\n$lastText\n\n"
            )
            undoStack.add(lastText)
            updateUndoButtonText("Undo")
        }
    }
}