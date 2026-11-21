package com.copilotomotorista

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.copilotomotorista.service.ShiftSessionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.runner.RunWith

/**
 * Exercises the actual Android SharedPreferences-backed shift store.
 *
 * These tests use the isolated CI test application's preferences and clear
 * their own state before and after each case. No Uber/99 activity is needed.
 */
@RunWith(AndroidJUnit4::class)
class ShiftSessionStoreInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun clearTestShift() {
        context.getSharedPreferences("shift_session", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Before fun setUp() = clearTestShift()
    @After fun tearDown() = clearTestShift()

    @Test
    fun retryingStartDoesNotResetActiveShiftOrCountSameTripAgain() {
        val store = ShiftSessionStore(context)
        store.start(now = 1_000L)
        store.recordCompletedTrip("trip-1", 2_750L)
        store.start(now = 3_000L) // Duplicate click or stale UI callback.

        val afterRetry = store.snapshot()
        assertTrue(afterRetry.active)
        assertEquals(1_000L, afterRetry.startedAtEpochMillis)
        assertEquals(1, afterRetry.completedTrips)
        assertEquals(2_750L, afterRetry.receivedFareCents)

        store.recordCompletedTrip("trip-1", 2_750L)
        store.recordCompletedTrip("trip-2", 1_200L)
        assertEquals(2, store.snapshot().completedTrips)
        assertEquals(3_950L, store.snapshot().receivedFareCents)

        store.end(now = 5_000L)
        assertFalse(store.snapshot().active)
        store.start(now = 7_000L) // A new shift really does start fresh.
        assertEquals(7_000L, store.snapshot().startedAtEpochMillis)
        assertEquals(0, store.snapshot().completedTrips)
        assertEquals(0L, store.snapshot().receivedFareCents)
        store.recordCompletedTrip("trip-1", 2_750L)
        assertEquals(1, store.snapshot().completedTrips)
    }

    @Test
    fun separateStoreInstancesCannotLoseConcurrentTripsOrCountTwice() {
        val first = ShiftSessionStore(context)
        val second = ShiftSessionStore(context)
        first.start(now = 1_000L)
        second.start(now = 2_000L) // Other UI/service instance must not reset a shift.
        assertEquals(1_000L, first.snapshot().startedAtEpochMillis)

        val release = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        fun worker(store: ShiftSessionStore, prefix: String) = Thread {
            try {
                check(release.await(5, TimeUnit.SECONDS)) { "Parallel workers did not start" }
                repeat(40) { index -> store.recordCompletedTrip("${prefix}-${index}", 125L) }
                repeat(10) { store.recordCompletedTrip("same-trip", 600L) }
            } catch (error: Throwable) {
                failure.compareAndSet(null, error)
            }
        }

        val one = worker(first, "first")
        val two = worker(second, "second")
        one.start()
        two.start()
        release.countDown()
        one.join(15_000L)
        two.join(15_000L)
        assertFalse("First worker failed to finish", one.isAlive)
        assertFalse("Second worker failed to finish", two.isAlive)
        failure.get()?.let { throw AssertionError("Concurrent store writer failed", it) }

        val result = ShiftSessionStore(context).snapshot()
        assertEquals(81, result.completedTrips)
        assertEquals(10_600L, result.receivedFareCents)
        assertEquals(1_000L, result.startedAtEpochMillis)
    }

    @Test
    fun monitoringStateRejectsStaleOrFutureHeartbeatAfterClockChange() {
        val store = ShiftSessionStore(context)
        val preferences = context.getSharedPreferences("shift_session", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // A manually changed clock must not keep a disconnected service
        // showing as connected simply because its old heartbeat is future-dated.
        preferences.edit()
            .putBoolean("monitoring_connected", true)
            .putLong("monitoring_heartbeat", now + 60_000L)
            .commit()
        assertFalse(store.snapshot().monitoringConnected)

        preferences.edit().putLong("monitoring_heartbeat", now - 60_000L).commit()
        assertFalse(store.snapshot().monitoringConnected)

        store.setMonitoringConnected(true)
        assertTrue(store.snapshot().monitoringConnected)
        store.setMonitoringConnected(false)
        assertFalse(store.snapshot().monitoringConnected)
    }

    @Test
    fun pausedAndEndedShiftsRejectNewTripsAndExcludePausedTime() {
        val store = ShiftSessionStore(context)
        store.start(now = 1_000L)
        store.recordCompletedTrip("", 100L)
        store.recordCompletedTrip("invalid-fare", -1L)
        assertEquals(0, store.snapshot().completedTrips)

        store.pause(now = 2_000L)
        assertTrue(store.snapshot().paused)
        store.start(now = 2_500L) // Must not silently resume or reset.
        assertTrue(store.snapshot().paused)
        store.recordCompletedTrip("paused-trip", 900L)
        assertEquals(0, store.snapshot().completedTrips)
        assertEquals(1_000L, store.snapshot().elapsedMillis(now = 8_000L))

        store.resume(now = 4_000L)
        assertFalse(store.snapshot().paused)
        assertEquals(2_000L, store.snapshot().pausedDurationMillis)
        store.recordCompletedTrip("paused-trip", 900L)
        assertEquals(1, store.snapshot().completedTrips)

        store.end(now = 8_000L)
        assertFalse(store.snapshot().active)
        assertEquals(5_000L, store.snapshot().elapsedMillis(now = 30_000L))
        store.recordCompletedTrip("after-end", 700L)
        assertEquals(1, store.snapshot().completedTrips)
        assertEquals(900L, store.snapshot().receivedFareCents)
    }
}
