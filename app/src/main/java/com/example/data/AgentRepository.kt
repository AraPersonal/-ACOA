package com.example.data

import kotlinx.coroutines.flow.Flow

class AgentRepository(private val dao: AgentDao) {
    val allSessions: Flow<List<ChatSession>> = dao.getAllSessions()
    
    fun getSessionsByType(type: String): Flow<List<ChatSession>> = dao.getSessionsByType(type)

    suspend fun insertSession(session: ChatSession): Int {
        return dao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: ChatSession) = dao.updateSession(session)

    suspend fun deleteSession(session: ChatSession) {
        dao.deleteMessagesForSession(session.id)
        dao.deleteSession(session)
    }

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessageEntity>> {
        return dao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: ChatMessageEntity) = dao.insertMessage(message)
    
    val allAssetVersions: Flow<List<AssetVersion>> = dao.getAllAssetVersions()
    
    fun getAssetVersionsByType(type: String): Flow<List<AssetVersion>> = dao.getAssetVersionsByType(type)
    
    fun getAssetVersionsForSession(sessionId: Int): Flow<List<AssetVersion>> = dao.getAssetVersionsForSession(sessionId)
    
    suspend fun insertAssetVersion(assetVersion: AssetVersion): Int {
        return dao.insertAssetVersion(assetVersion).toInt()
    }
}
