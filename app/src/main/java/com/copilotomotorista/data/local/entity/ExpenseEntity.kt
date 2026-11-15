package com.copilotomotorista.data.local.entity
import androidx.room.*
@Entity(tableName="expenses",foreignKeys=[ForeignKey(entity=VehicleEntity::class,parentColumns=["id"],childColumns=["vehicleId"],onDelete=ForeignKey.SET_NULL)],indices=[Index("vehicleId"),Index("occurredAtEpochMillis")])
data class ExpenseEntity(@PrimaryKey val id:String,val category:String,val description:String,val amountCents:Long,val occurredAtEpochMillis:Long,val vehicleId:String?,val odometerKm:Double?,val quantity:Double?,val unit:String?,val unitPriceCents:Long?,val notes:String?,val createdAtEpochMillis:Long)
