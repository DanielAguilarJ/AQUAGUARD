package com.tuempresa.fugas.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tuempresa.fugas.model.AlertStatus

@Database(entities = [AlertStatus::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertStatusDao(): AlertStatusDao
}
