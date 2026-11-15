package com.copilotomotorista.data

import android.content.Context
import com.copilotomotorista.data.local.AppDatabase
import com.copilotomotorista.data.repository.*

class AppContainer(context: Context) {
 private val database = AppDatabase.getInstance(context)
 val vehicleRepository: VehicleRepository = RoomVehicleRepository(database.vehicleDao())
 val tripRecordRepository: TripRecordRepository = RoomTripRecordRepository(database.tripRecordDao())
 val expenseRepository: ExpenseRepository = RoomExpenseRepository(database.expenseDao())
 val clientRepository: ClientRepository = RoomClientRepository(database.clientDao())
 val privateTripRepository: PrivateTripRepository = RoomPrivateTripRepository(database.privateTripDao())
}
