package data

/**
 * Represents a single saved geofence in the app.
 *
 * This is a plain Kotlin data class – will manually
 * save/load it using SharedPreferences + JSON in AppDatabase.
 */
data class GeofenceEntity(
    val id: Long = 0L,              // 0L means "not yet stored" – we'll assign an id when saving
    val name: String,               // e.g. "Home", "Work"
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float         // radius in meters
)