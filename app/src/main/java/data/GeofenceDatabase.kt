package com.example.geofenceapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.geofenceapplication.data.GeofenceDao
import com.example.geofenceapplication.data.GeofenceEntity

@Database(
    entities = [GeofenceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeofenceDatabase : RoomDatabase() {

    abstract fun geofenceDao(): GeofenceDao

    companion object {
        @Volatile
        private var INSTANCE: GeofenceDatabase? = null

        fun getInstance(context: Context): GeofenceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeofenceDatabase::class.java,
                    "geofence_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}