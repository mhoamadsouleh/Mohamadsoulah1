package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM user_sessions WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentSession(): Flow<UserSession?>

    @Query("SELECT * FROM user_sessions WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSessionSync(): UserSession?

    @Query("SELECT * FROM user_sessions ORDER BY loginTimestamp DESC")
    fun getAllSessions(): Flow<List<UserSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)

    @Query("UPDATE user_sessions SET isCurrent = 0")
    suspend fun clearCurrentSessionFlag()

    @Transaction
    suspend fun setCurrentSession(session: UserSession) {
        clearCurrentSessionFlag()
        insertSession(session.copy(isCurrent = true))
    }

    @Query("DELETE FROM user_sessions WHERE phone = :phone")
    suspend fun deleteSession(phone: String)

    @Query("DELETE FROM user_sessions")
    suspend fun clearAll()
}

@Dao
interface ActivationDao {
    @Query("SELECT * FROM activation_records ORDER BY timestamp DESC")
    fun getAllActivations(): Flow<List<ActivationRecord>>

    @Query("SELECT * FROM activation_records WHERE offerType = :type ORDER BY timestamp DESC")
    fun getActivationsByType(type: String): Flow<List<ActivationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivation(record: ActivationRecord)

    @Query("SELECT COUNT(*) FROM activation_records WHERE isSuccess = 1")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activation_records WHERE offerType = :type AND isSuccess = 1")
    fun getSuccessCountByType(type: String): Flow<Int>

    @Query("DELETE FROM activation_records")
    suspend fun clearHistory()
}
