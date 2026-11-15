package com.copilotomotorista.data.repository
import com.copilotomotorista.data.local.dao.ClientDao
import com.copilotomotorista.data.mapper.toDomain
import com.copilotomotorista.data.mapper.toEntity
import com.copilotomotorista.domain.model.Client
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class RoomClientRepository(private val dao: ClientDao) : ClientRepository {
 override fun observeClients(): Flow<List<Client>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
 override suspend fun getClient(id: String): Client? = dao.findById(id)?.toDomain()
 override suspend fun saveClient(client: Client) = dao.upsert(client.toEntity())
 override suspend fun deleteClient(id: String) = dao.deleteById(id)
}
