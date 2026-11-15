package com.copilotomotorista.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Extrai apenas texto visível/descrições da árvore atual.
 * Limites protegem o serviço contra árvores grandes ou cíclicas/malformadas.
 */
internal class AccessibilityNodeTextReader(
    private val maxVisitedNodes: Int = 160,
    private val maxTextEntries: Int = 60
) {
    init {
        require(maxVisitedNodes > 0)
        require(maxTextEntries > 0)
    }

    fun read(root: AccessibilityNodeInfo?, capturedAtMillis: Long): ScreenTextSnapshot {
        if (root == null) {
            return ScreenTextSnapshot("", ScreenTextSource.ACCESSIBILITY_TREE, capturedAtMillis)
        }

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val collected = LinkedHashSet<String>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < maxVisitedNodes && collected.size < maxTextEntries) {
            val node = queue.removeFirst()
            visited++

            node.text?.toString()?.let(::addTextIfUseful)?.let(collected::add)
            node.contentDescription?.toString()?.let(::addTextIfUseful)?.let(collected::add)

            for (index in 0 until node.childCount) {
                if (visited + queue.size >= maxVisitedNodes) break
                node.getChild(index)?.let(queue::addLast)
            }
        }

        val text = ScreenTextSnapshot.normalize(collected.joinToString("\n"))
        return ScreenTextSnapshot(
            text = text,
            source = ScreenTextSource.ACCESSIBILITY_TREE,
            capturedAtMillis = capturedAtMillis,
            visitedNodeCount = visited
        )
    }

    private fun addTextIfUseful(value: String): String? =
        value.trim().takeIf { it.isNotEmpty() }
}
