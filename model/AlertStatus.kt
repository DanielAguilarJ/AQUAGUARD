package com.tuempresa.fugas.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_status")
data class AlertStatus(
    @PrimaryKey val timestamp: String, // Usamos timestamp como ID única
    val revisada: Boolean = false,
    val eliminada: Boolean = false
)
