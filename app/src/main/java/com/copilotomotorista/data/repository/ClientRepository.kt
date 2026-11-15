package com.copilotomotorista.data.repository
import com.copilotomotorista.domain.model.Client
import kotlinx.coroutines.flow.Flow
interface ClientRepository {
 fun observeClients(): Flow<List<Client>>
 suspend fun getClient(id: String): Client?
 suspend fun saveClient(client: Client)
 suspend fun deleteClient(id: String)
}
