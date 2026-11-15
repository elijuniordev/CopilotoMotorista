package com.copilotomotorista.data.local.entity
import androidx.room.*
@Entity(tableName="private_trips",foreignKeys=[ForeignKey(entity=ClientEntity::class,parentColumns=["id"],childColumns=["clientId"],onDelete=ForeignKey.SET_NULL)],indices=[Index("clientId"),Index("scheduledAtEpochMillis")])
data class PrivateTripEntity(@PrimaryKey val id:String,val clientId:String?,val title:String,val origin:String?,val destination:String?,val scheduledAtEpochMillis:Long,val agreedFareCents:Long,val distanceKm:Double?,val durationMinutes:Int?,val status:String,val paymentStatus:String,val paymentMethod:String?,val notes:String?,val createdAtEpochMillis:Long)
