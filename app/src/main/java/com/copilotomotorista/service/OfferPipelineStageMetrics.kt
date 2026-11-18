package com.copilotomotorista.service

/**
 * Privacy-safe per-stage latency samples. No offer text, fare, addresses or identifiers
 * are retained; only route labels, stage names, outcomes and monotonic durations.
 */
internal class OfferPipelineStageMetrics(
    private val reportEvery: Int = 20,
    private val maxSamplesPerRoute: Int = 200
) {
    init {
        require(reportEvery > 0) { "reportEvery must be positive" }
        require(maxSamplesPerRoute > 0) { "maxSamplesPerRoute must be positive" }
    }

    data class Summary(
        val stage: String,
        val platform: String,
        val source: String,
        val outcome: String,
        val count: Int,
        val p50Millis: Long,
        val p95Millis: Long
    )

    private val samples = mutableMapOf<String, ArrayDeque<Long>>()
    private val counts = mutableMapOf<String, Int>()

    @Synchronized
    fun record(
        stage: String,
        platform: String,
        source: String,
        durationMillis: Long,
        succeeded: Boolean
    ): Summary? {
        if (stage.isBlank() || durationMillis < 0L) return null
        val outcome = if (succeeded) "success" else "failure"
        val key = listOf(stage.lowercase(), platform.lowercase(), source.lowercase(), outcome)
            .joinToString("|")
        val values = samples.getOrPut(key) { ArrayDeque() }
        values.addLast(durationMillis)
        if (values.size > maxSamplesPerRoute) values.removeFirst()
        val count = (counts[key] ?: 0) + 1
        counts[key] = count
        if (count % reportEvery != 0) return null

        val sorted = values.sorted()
        return Summary(
            stage = stage,
            platform = platform,
            source = source,
            outcome = outcome,
            count = count,
            p50Millis = percentile(sorted, 0.50),
            p95Millis = percentile(sorted, 0.95)
        )
    }

    private fun percentile(sorted: List<Long>, percentile: Double): Long {
        if (sorted.isEmpty()) return 0L
        val index = kotlin.math.ceil(percentile * sorted.size).toInt()
            .coerceIn(1, sorted.size) - 1
        return sorted[index]
    }
}
