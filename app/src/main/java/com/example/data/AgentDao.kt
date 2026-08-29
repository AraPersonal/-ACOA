package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>
    
    @Query("SELECT * FROM chat_sessions WHERE sessionType = :type ORDER BY timestamp DESC")
    fun getSessionsByType(type: String): Flow<List<ChatSession>>

    @Insert
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)
    
    @Query("SELECT * FROM asset_versions ORDER BY timestamp DESC")
    fun getAllAssetVersions(): Flow<List<AssetVersion>>
    
    @Query("SELECT * FROM asset_versions WHERE type = :type ORDER BY timestamp DESC")
    fun getAssetVersionsByType(type: String): Flow<List<AssetVersion>>
    
    @Query("SELECT * FROM asset_versions WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getAssetVersionsForSession(sessionId: Int): Flow<List<AssetVersion>>
    
    @Insert
    suspend fun insertAssetVersion(assetVersion: AssetVersion): Long
}
