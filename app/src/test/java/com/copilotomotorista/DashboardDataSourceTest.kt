package com.copilotomotorista

import com.copilotomotorista.data.preferences.DashboardPreferences
import com.copilotomotorista.data.repository.ExpenseRepository
import com.copilotomotorista.data.repository.TripRecordRepository
import com.copilotomotorista.data.repository.VehicleRepository
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.FinancialTargets
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts
import com.copilotomotorista.presentation.dashboard.DashboardDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardDataSourceTest {
    @Test
    fun emitsInitialSnapshotAndUpdatesWhenRoomOrPreferencesChange() = runBlocking {
        val trips = MutableStateFlow<List<TripRecord>>(emptyList())
        val expenses = MutableStateFlow<List<ExpenseRecord>>(emptyList())
        val vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
        val selectedVehicleId = MutableStateFlow<String?>(null)
        val targets = MutableStateFlow(FinancialTargets())
        val source = DashboardDataSource(
            tripRecordRepository = FakeTripRepository(trips),
            expenseRepository = FakeExpenseRepository(expenses),
            vehicleRepository = FakeVehicleRepository(vehicles),
            preferences = FakeDashboardPreferences(selectedVehicleId, targets)
        )

        val initial = withTimeout(2_000) { source.snapshot.first() }
        assertEquals(emptyList<TripRecord>(), initial.trips)
        assertEquals(emptyList<ExpenseRecord>(), initial.expenses)
        assertNull(initial.selectedVehicle)
        assertEquals(FinancialTargets(), initial.financialTargets)

        val vehicle = Vehicle(
            id = "vehicle-1",
            name = "Carro de teste",
            energy = VehicleEnergy.LiquidFuel(
                fuelType = com.copilotomotorista.domain.model.FuelType.GASOLINE,
                consumptionKmPerLiter = 12.0,
                pricePerLiterCents = 599L
            ),
            operatingCosts = VehicleOperatingCosts(maintenanceCentsPerKm = 10L, depreciationCentsPerKm = 20L)
        )
        vehicles.value = listOf(vehicle)
        selectedVehicleId.value = vehicle.id
        targets.value = FinancialTargets(
            excellentNetCentsPerKm = 500L,
            goodNetCentsPerKm = 300L,
            warningNetCentsPerKm = 150L,
            minimumNetCentsPerHour = 2_000L
        )
        val updated = withTimeout(2_000) {
            source.snapshot.first {
                it.selectedVehicle?.id == vehicle.id &&
                    it.financialTargets.minimumNetCentsPerHour == 2_000L
            }
        }
        assertEquals(vehicle, updated.selectedVehicle)

        val trip = TripRecord(
            id = "trip-1", platform = "Uber", status = "completed",
            receivedFareCents = 4_500L, pickupDistanceKm = 1.0,
            tripDistanceKm = 5.0, pickupTimeMinutes = 4, tripTimeMinutes = 16,
            origin = null, destination = null, passengerRating = null,
            paymentMethod = null, startedAtEpochMillis = null,
            completedAtEpochMillis = 1_000L, vehicleId = vehicle.id,
            evidenceUri = null, notes = null, createdAtEpochMillis = 1_000L
        )
        trips.value = listOf(trip)
        val tripUpdate = withTimeout(2_000) {
            source.snapshot.first { it.trips.singleOrNull()?.id == trip.id }
        }
        assertEquals(trip, tripUpdate.trips.single())

        val expense = ExpenseRecord(
            id = "expense-1", category = "fuel", description = "Abastecimento",
            amountCents = 1_000L, occurredAtEpochMillis = 2_000L,
            vehicleId = vehicle.id, odometerKm = null, quantity = null,
            unit = null, unitPriceCents = null, notes = null,
            createdAtEpochMillis = 2_000L
        )
        expenses.value = listOf(expense)
        val expenseUpdate = withTimeout(2_000) {
            source.snapshot.first { it.expenses.singleOrNull()?.id == expense.id }
        }
        assertEquals(expense, expenseUpdate.expenses.single())
    }

    private class FakeDashboardPreferences(
        override val selectedVehicleId: Flow<String?>,
        override val financialTargets: Flow<FinancialTargets>
    ) : DashboardPreferences

    private class FakeTripRepository(
        private val records: MutableStateFlow<List<TripRecord>>
    ) : TripRecordRepository {
        override fun observeTrips(): Flow<List<TripRecord>> = records
        override suspend fun getTrip(id: String): TripRecord? = records.value.firstOrNull { it.id == id }
        override suspend fun findRecentMatchingOffer(platform: String, fareCents: Long, pickupDistanceKm: Double, tripDistanceKm: Double, pickupTimeMinutes: Int, tripTimeMinutes: Int, sinceEpochMillis: Long): TripRecord? = null
        override suspend fun saveTrip(trip: TripRecord) { records.value = records.value.filterNot { it.id == trip.id } + trip }
        override suspend fun findOrInsertCapturedOffer(
            captured: TripRecord, sinceEpochMillis: Long
        ): TripRecord {
            val existing = records.value
                .filter { it.platform == captured.platform &&
                    it.status in setOf("captured", "accepted", "in_progress", "completed") &&
                    it.receivedFareCents == captured.receivedFareCents &&
                    kotlin.math.abs(it.pickupDistanceKm - captured.pickupDistanceKm) < 0.01 &&
                    kotlin.math.abs(it.tripDistanceKm - captured.tripDistanceKm) < 0.01 &&
                    it.pickupTimeMinutes == captured.pickupTimeMinutes &&
                    it.tripTimeMinutes == captured.tripTimeMinutes &&
                    it.createdAtEpochMillis >= sinceEpochMillis
                }.maxByOrNull { it.createdAtEpochMillis }
                ?: records.value
                    .filter {
                        it.platform == captured.platform &&
                            it.status in setOf("captured", "accepted", "in_progress", "completed") &&
                            !captured.rawOfferText.isNullOrBlank() &&
                            it.rawOfferText == captured.rawOfferText &&
                            it.createdAtEpochMillis >= sinceEpochMillis
                    }.maxByOrNull { it.createdAtEpochMillis }
                ?: records.value.firstOrNull { it.id == captured.id }
            if (existing != null) return existing
            records.value = records.value + captured
            return captured
        }
        override suspend fun enrichCapturedOfferIfMissing(
            id: String, rawText: String, origin: String?, destination: String?
        ): Boolean {
            val current = records.value.firstOrNull {
                it.id == id && it.status == "captured" && it.rawOfferText == null
            } ?: return false
            records.value = records.value.map {
                if (it.id == id) current.copy(
                    rawOfferText = rawText,
                    origin = current.origin ?: origin,
                    destination = current.destination ?: destination
                ) else it
            }
            return true
        }
        override suspend fun fillCapturedCostSnapshotIfMissing(
            id: String, vehicleId: String, costCents: Long, vehicleName: String
        ): Boolean {
            val current = records.value.firstOrNull {
                it.id == id && it.status == "captured" &&
                    (it.vehicleId == null || it.operatingCostCentsSnapshot == null ||
                        it.vehicleNameSnapshot == null)
            } ?: return false
            records.value = records.value.map {
                if (it.id == id) current.copy(
                    vehicleId = current.vehicleId ?: vehicleId,
                    operatingCostCentsSnapshot = current.operatingCostCentsSnapshot ?: costCents,
                    vehicleNameSnapshot = current.vehicleNameSnapshot ?: vehicleName
                ) else it
            }
            return true
        }
        override suspend fun attachEvidenceIfAbsent(id: String, path: String): Boolean {
            val record = records.value.firstOrNull { it.id == id && it.evidenceUri == null } ?: return false
            records.value = records.value.map { if (it.id == id) record.copy(evidenceUri = path) else it }
            return true
        }
        override suspend fun clearEvidenceIfMatches(id: String, path: String): Boolean {
            val record = records.value.firstOrNull { it.id == id && it.evidenceUri == path } ?: return false
            records.value = records.value.map { if (it.id == id) record.copy(evidenceUri = null) else it }
            return true
        }
        override suspend fun confirmOfferFromOverlay(
            id: String, requestedStatus: String, nowEpochMillis: Long,
            detectedOrigin: String?, detectedDestination: String?, paymentMethod: String?,
            vehicleId: String, vehicleName: String, costCents: Long
        ): Boolean {
            val trip = records.value.firstOrNull { it.id == id } ?: return false
            val allowed = when (requestedStatus) {
                "accepted" -> trip.status == "captured"
                "completed" -> trip.status == "accepted" || trip.status == "in_progress"
                else -> false
            }
            if (!allowed) return false
            records.value = records.value.map {
                if (it.id != id) it else it.copy(
                    status = requestedStatus,
                    origin = it.origin ?: detectedOrigin,
                    destination = it.destination ?: detectedDestination,
                    paymentMethod = it.paymentMethod ?: paymentMethod,
                    startedAtEpochMillis = it.startedAtEpochMillis
                        ?: if (requestedStatus == "completed") nowEpochMillis else null,
                    completedAtEpochMillis = it.completedAtEpochMillis
                        ?: if (requestedStatus == "completed") nowEpochMillis else null,
                    vehicleId = it.vehicleId ?: vehicleId,
                    vehicleNameSnapshot = it.vehicleNameSnapshot ?: vehicleName,
                    operatingCostCentsSnapshot = it.operatingCostCentsSnapshot ?: costCents,
                    notes = if (it.notes == null || it.notes == "Oferta capturada automaticamente.")
                        "Registro confirmado manualmente pelo motorista." else it.notes
                )
            }
            return true
        }
        override suspend fun deleteTrip(id: String) { records.value = records.value.filterNot { it.id == id } }
    }

    private class FakeExpenseRepository(
        private val records: MutableStateFlow<List<ExpenseRecord>>
    ) : ExpenseRepository {
        override fun observeExpenses(): Flow<List<ExpenseRecord>> = records
        override suspend fun getExpense(id: String): ExpenseRecord? = records.value.firstOrNull { it.id == id }
        override suspend fun saveExpense(expense: ExpenseRecord) { records.value = records.value.filterNot { it.id == expense.id } + expense }
        override suspend fun deleteExpense(id: String) { records.value = records.value.filterNot { it.id == id } }
    }

    private class FakeVehicleRepository(
        private val records: MutableStateFlow<List<Vehicle>>
    ) : VehicleRepository {
        override fun observeVehicles(): Flow<List<Vehicle>> = records
        override suspend fun getVehicle(id: String): Vehicle? = records.value.firstOrNull { it.id == id }
        override suspend fun saveVehicle(vehicle: Vehicle) { records.value = records.value.filterNot { it.id == vehicle.id } + vehicle }
        override suspend fun deleteVehicle(id: String) { records.value = records.value.filterNot { it.id == id } }
    }
}
