package data

/**
 * Data access object (DAO) for geofences.
 *
 * This is an abstraction over storage.
 * The implementation will live inside AppDatabase.
 */
interface GeofenceDao {

    /**
     * Get all saved geofences.
     */
    fun getAllGeofences(): List<GeofenceEntity>

    /**
     * Get a single geofence by its id, or null if it doesn't exist.
     */
    fun getGeofenceById(id: Long): GeofenceEntity?

    /**
     * Insert a new geofence or update an existing one.
     *
     *
     *
     * Returns the saved entity (with a non-zero id).
     */
    fun upsertGeofence(entity: GeofenceEntity): GeofenceEntity

    /**
     * Delete a geofence by id.
     */
    fun deleteGeofence(id: Long)

    /**

     */
    fun clearAll()
}