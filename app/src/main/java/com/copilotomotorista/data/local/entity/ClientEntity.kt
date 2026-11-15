package com.copilotomotorista.data.local.entity
import androidx.room.*
@Entity(tableName="clients",indices=[Index("name")])
data class ClientEntity(@PrimaryKey val id:String,val name:String,val phone:String?,val notes:String?,val createdAtEpochMillis:Long,val updatedAtEpochMillis:Long)
