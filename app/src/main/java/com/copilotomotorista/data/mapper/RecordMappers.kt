package com.copilotomotorista.data.mapper

import com.copilotomotorista.data.local.entity.TripRecordEntity
import com.copilotomotorista.data.local.entity.ExpenseEntity
import com.copilotomotorista.data.local.entity.ClientEntity
import com.copilotomotorista.data.local.entity.PrivateTripEntity
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.model.ExpenseRecord
import com.copilotomotorista.domain.model.Client
import com.copilotomotorista.domain.model.PrivateTrip

fun TripRecord.toEntity() = TripRecordEntity(id, platform, status, receivedFareCents, pickupDistanceKm, tripDistanceKm, pickupTimeMinutes, tripTimeMinutes, origin, destination, passengerRating, paymentMethod, startedAtEpochMillis, completedAtEpochMillis, vehicleId, evidenceUri, notes, createdAtEpochMillis)
fun TripRecordEntity.toDomain() = TripRecord(id, platform, status, receivedFareCents, pickupDistanceKm, tripDistanceKm, pickupTimeMinutes, tripTimeMinutes, origin, destination, passengerRating, paymentMethod, startedAtEpochMillis, completedAtEpochMillis, vehicleId, evidenceUri, notes, createdAtEpochMillis)

fun ExpenseRecord.toEntity() = ExpenseEntity(id, category, description, amountCents, occurredAtEpochMillis, vehicleId, odometerKm, quantity, unit, unitPriceCents, notes, createdAtEpochMillis)
fun ExpenseEntity.toDomain() = ExpenseRecord(id, category, description, amountCents, occurredAtEpochMillis, vehicleId, odometerKm, quantity, unit, unitPriceCents, notes, createdAtEpochMillis)

fun Client.toEntity() = ClientEntity(id, name, phone, notes, createdAtEpochMillis, updatedAtEpochMillis)
fun ClientEntity.toDomain() = Client(id, name, phone, notes, createdAtEpochMillis, updatedAtEpochMillis)

fun PrivateTrip.toEntity() = PrivateTripEntity(id, clientId, title, origin, destination, scheduledAtEpochMillis, agreedFareCents, distanceKm, durationMinutes, status, paymentStatus, paymentMethod, notes, createdAtEpochMillis)
fun PrivateTripEntity.toDomain() = PrivateTrip(id, clientId, title, origin, destination, scheduledAtEpochMillis, agreedFareCents, distanceKm, durationMinutes, status, paymentStatus, paymentMethod, notes, createdAtEpochMillis)
