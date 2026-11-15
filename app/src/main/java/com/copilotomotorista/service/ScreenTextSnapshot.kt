package com.copilotomotorista.service

enum class ScreenTextSource {
    ACCESSIBILITY_TREE,
    OCR
}

data class ScreenTextSnapshot(
    val text: String,
    val source: ScreenTextSource,
    val capturedAtMillis: Long,
    val visitedNodeCount: Int = 0
) {
    val hasUsableText: Boolean
        get() = text.trim().length >= MINIMUM_USABLE_CHARACTERS

    companion object {
        const val MINIMUM_USABLE_CHARACTERS = 12

        fun normalize(rawText: String): String =
            rawText.lineSequence()
                .map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("\n")
    }
}
