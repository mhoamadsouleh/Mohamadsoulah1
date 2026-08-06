package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val phone: String, // e.g. 0792123456
    val msisdn: String, // e.g. 213792123456
    val accessToken: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = true
)

@Entity(tableName = "activation_records")
data class ActivationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val msisdn: String,
    val phone: String,
    val offerName: String,
    val offerCode: String,
    val offerType: String, // "2go_walk", "paid_offer", "mgm", "migration"
    val isSuccess: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
