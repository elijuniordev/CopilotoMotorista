package com.copilotomotorista.service

import android.content.Context

data class ShiftSnapshot(
    val active: Boolean,
    val paused: Boolean,
    val startedAtEpochMillis: Long,
    val pausedDurationMillis: Long,
    val pauseStartedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val completedTrips: Int,
    val receivedFareCents: Long,
    val monitoringConnected: Boolean
) {
    fun elapsedMillis(now: Long = System.currentTimeMillis()): Long {
        if (startedAtEpochMillis <= 0L) return 0L
        val end = when {
            paused && pauseStartedAtEpochMillis > 0L -> pauseStartedAtEpochMillis
            !active && endedAtEpochMillis > 0L -> endedAtEpochMillis
            else -> now
        }
        return (end - startedAtEpochMillis - pausedDurationMillis).coerceAtLeast(0L)
    }

    val canProcessOffers: Boolean get() = active && !paused && monitoringConnected
}

class ShiftSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("shift_session", Context.MODE_PRIVATE)

    @Synchronized
    fun snapshot(): ShiftSnapshot = ShiftSnapshot(
        active = preferences.getBoolean(KEY_ACTIVE, false),
        paused = preferences.getBoolean(KEY_PAUSED, false),
        startedAtEpochMillis = preferences.getLong(KEY_STARTED_AT, 0L),
        pausedDurationMillis = preferences.getLong(KEY_PAUSED_DURATION, 0L),
        pauseStartedAtEpochMillis = preferences.getLong(KEY_PAUSE_STARTED_AT, 0L),
        endedAtEpochMillis = preferences.getLong(KEY_ENDED_AT, 0L),
        completedTrips = preferences.getInt(KEY_COMPLETED_TRIPS, 0),
        receivedFareCents = preferences.getLong(KEY_RECEIVED_FARE, 0L),
        monitoringConnected = preferences.getBoolean(KEY_MONITORING_CONNECTED, false) &&
            System.currentTimeMillis() - preferences.getLong(KEY_MONITORING_HEARTBEAT, 0L) <= MONITORING_HEARTBEAT_TIMEOUT_MILLIS
    )

    @Synchronized
    fun start(now: Long = System.currentTimeMillis()) {
        // Repeated taps, stale UI callbacks or a restored screen must not
        // reset a running shift's revenue, completed-trip IDs or elapsed time.
        // Pause/resume and ending a shift have their own explicit actions.
        if (snapshot().active) return
        preferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_PAUSED, false)
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_PAUSED_DURATION, 0L)
            .putLong(KEY_PAUSE_STARTED_AT, 0L)
            .putLong(KEY_ENDED_AT, 0L)
            .putInt(KEY_COMPLETED_TRIPS, 0)
            .putLong(KEY_RECEIVED_FARE, 0L)
            .putStringSet(KEY_COUNTED_TRIPS, emptySet())
            .apply()
    }

    @Synchronized
    fun pause(now: Long = System.currentTimeMillis()) {
        val current = snapshot()
        if (!current.active || current.paused) return
        preferences.edit()
            .putBoolean(KEY_PAUSED, true)
            .putLong(KEY_PAUSE_STARTED_AT, now)
            .apply()
    }

    @Synchronized
    fun resume(now: Long = System.currentTimeMillis()) {
        val current = snapshot()
        if (!current.active || !current.paused) return
        val pauseDuration = (now - current.pauseStartedAtEpochMillis).coerceAtLeast(0L)
        preferences.edit()
            .putBoolean(KEY_PAUSED, false)
            .putLong(KEY_PAUSED_DURATION, current.pausedDurationMillis + pauseDuration)
            .putLong(KEY_PAUSE_STARTED_AT, 0L)
            .apply()
    }

    @Synchronized
    fun end(now: Long = System.currentTimeMillis()) {
        val current = snapshot()
        if (!current.active) return
        val extraPause = if (current.paused) (now - current.pauseStartedAtEpochMillis).coerceAtLeast(0L) else 0L
        preferences.edit()
            .putBoolean(KEY_ACTIVE, false)
            .putBoolean(KEY_PAUSED, false)
            .putLong(KEY_PAUSED_DURATION, current.pausedDurationMillis + extraPause)
            .putLong(KEY_PAUSE_STARTED_AT, 0L)
            .putLong(KEY_ENDED_AT, now)
            .apply()
    }

    @Synchronized
    fun setMonitoringConnected(connected: Boolean) {
        preferences.edit()
            .putBoolean(KEY_MONITORING_CONNECTED, connected)
            .putLong(KEY_MONITORING_HEARTBEAT, if (connected) System.currentTimeMillis() else 0L)
            .apply()
    }

    @Synchronized
    fun heartbeat() {
        if (preferences.getBoolean(KEY_MONITORING_CONNECTED, false)) {
            preferences.edit().putLong(KEY_MONITORING_HEARTBEAT, System.currentTimeMillis()).apply()
        }
    }

    @Synchronized
    fun recordCompletedTrip(tripId: String, fareCents: Long) {
        val current = snapshot()
        if (!current.active || current.paused || tripId.isBlank() || fareCents < 0L) return
        val counted = preferences.getStringSet(KEY_COUNTED_TRIPS, emptySet()).orEmpty()
        if (tripId in counted) return
        val updatedIds = counted.toMutableSet().apply { add(tripId) }
        preferences.edit()
            .putStringSet(KEY_COUNTED_TRIPS, updatedIds)
            .putInt(KEY_COMPLETED_TRIPS, current.completedTrips + 1)
            .putLong(KEY_RECEIVED_FARE, current.receivedFareCents + fareCents)
            .apply()
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_PAUSED = "paused"
        const val KEY_STARTED_AT = "started_at"
        const val KEY_PAUSED_DURATION = "paused_duration"
        const val KEY_PAUSE_STARTED_AT = "pause_started_at"
        const val KEY_ENDED_AT = "ended_at"
        const val KEY_COMPLETED_TRIPS = "completed_trips"
        const val KEY_RECEIVED_FARE = "received_fare_cents"
        const val KEY_COUNTED_TRIPS = "counted_trip_ids"
        const val KEY_MONITORING_CONNECTED = "monitoring_connected"
        const val KEY_MONITORING_HEARTBEAT = "monitoring_heartbeat"
        const val MONITORING_HEARTBEAT_TIMEOUT_MILLIS = 5_000L
    }
}
