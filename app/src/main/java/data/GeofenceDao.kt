package data

/**
 * Data access object (DAO) for geofences.
 *
 * This is an abstraction over our storage.
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
     * - If entity.id == 0L -> assign a new id and insert.
     * - Otherwise -> replace the existing geofence with the same id.
     *
     * Returns the saved entity (with a non-zero id).
     */
    fun upsertGeofence(entity: GeofenceEntity): GeofenceEntity

    /**
     * Delete a geofence by id.
     */
    fun deleteGeofence(id: Long)

    /**
     * Remove all stored geofences.
     * (Probably not needed often, but useful for debugging / reset.)
     */
    fun clearAll()
}