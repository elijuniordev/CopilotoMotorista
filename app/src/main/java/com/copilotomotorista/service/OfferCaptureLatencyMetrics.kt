package com.copilotomotorista.service

/**
 * In-memory, privacy-safe latency samples. Only durations and source labels are retained.
 */
internal class OfferCaptureLatencyMetrics(
    private val reportEvery: Int = 20,
    private val maxSamplesPerRoute: Int = 200
) {
    data class Summary(
        val platform: String,
        val source: String,
        val count: Int,
        val p50Millis: Long,
        val p95Millis: Long
    )

    private val samples = mutableMapOf<String, ArrayDeque<Long>>()
    private val counts = mutableMapOf<String, Int>()

    @Synchronized
    fun record(platform: String, source: String, durationMillis: Long): Summary? {
        if (durationMillis < 0L) return null
        val key = "${platform.lowercase()}|${source.lowercase()}"
        val values = samples.getOrPut(key) { ArrayDeque() }
        values.addLast(durationMillis)
        if (values.size > maxSamplesPerRoute) values.removeFirst()
        val count = (counts[key] ?: 0) + 1
        counts[key] = count
        if (count % reportEvery != 0) return null
        val sorted = values.sorted()
        return Summary(
            platform = platform,
            source = source,
            count = count,
            p50Millis = percentile(sorted, 0.50),
            p95Millis = percentile(sorted, 0.95)
        )
    }

    private fun percentile(sorted: List<Long>, percentile: Double): Long {
        if (sorted.isEmpty()) return 0L
        val index = kotlin.math.ceil(percentile * sorted.size).toInt().coerceIn(1, sorted.size) - 1
        return sorted[index]
    }
}
