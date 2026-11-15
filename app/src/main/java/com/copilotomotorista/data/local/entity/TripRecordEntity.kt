package com.copilotomotorista.data.local.entity
import androidx.room.*
@Entity(tableName="trip_records", foreignKeys=[ForeignKey(entity=VehicleEntity::class,parentColumns=["id"],childColumns=["vehicleId"],onDelete=ForeignKey.SET_NULL)],indices=[Index("vehicleId"),Index("startedAtEpochMillis")])
data class TripRecordEntity(@PrimaryKey val id:String,val platform:String,val status:String,val receivedFareCents:Long,val pickupDistanceKm:Double,val tripDistanceKm:Double,val pickupTimeMinutes:Int,val tripTimeMinutes:Int,val origin:String?,val destination:String?,val passengerRating:Double?,val paymentMethod:String?,val startedAtEpochMillis:Long?,val completedAtEpochMillis:Long?,val vehicleId:String?,val evidenceUri:String?,val notes:String?,val createdAtEpochMillis:Long)
