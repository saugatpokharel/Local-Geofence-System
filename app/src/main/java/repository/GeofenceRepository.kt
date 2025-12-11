package com.example.geofenceapplication.data

import kotlinx.coroutines.flow.Flow

class GeofenceRepository(
    private val dao: GeofenceDao
) {

    val allGeofences: Flow<List<GeofenceEntity>> = dao.getAllGeofences()

    suspend fun insert(geofence: GeofenceEntity) {
        dao.insertGeofence(geofence)
    }

    suspend fun delete(geofence: GeofenceEntity) {
        dao.deleteGeofence(geofence)
    }

    // Optional: delete by id, clear all, etc.
}
