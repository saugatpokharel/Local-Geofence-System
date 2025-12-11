package data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple "database" wrapper for the app.
 *
 * Under the hood this uses SharedPreferences + JSON so that we don't
 * need any extra database dependencies (like Room) for the project.
 *
 * Usage from anywhere in the app:
 *
 *   val db = AppDatabase.getInstance(context)
 *   val dao = db.geofenceDao
 *   val allGeofences = dao.getAllGeofences()
 */
class AppDatabase private constructor(
    private val prefs: SharedPreferences
) {

    val geofenceDao: GeofenceDao = SharedPrefsGeofenceDao(prefs)

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get the singleton instance of AppDatabase.
         *
         * Always use applicationContext to avoid leaking an Activity.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val prefs = appContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                val instance = AppDatabase(prefs)
                INSTANCE = instance
                instance
            }
        }

        private const val PREFS_NAME = "geofence_database"
        internal const val KEY_GEOFENCES = "geofences_json"
        internal const val KEY_NEXT_ID = "next_geofence_id"
    }
}

/**
 * Private implementation of GeofenceDao that stores geofences
 * as a JSON array in SharedPreferences.
 *
 *  keeping things simple:
 * - All geofences in one JSON array.
 * - Each geofence is a JSON object with id, name, lat, lng, radius.
 */
private class SharedPrefsGeofenceDao(
    private val prefs: SharedPreferences
) : GeofenceDao {

    override fun getAllGeofences(): List<GeofenceEntity> {
        val json = prefs.getString(AppDatabase.KEY_GEOFENCES, null) ?: return emptyList()
        val array = try {
            JSONArray(json)
        } catch (e: Exception) {
            // Corrupt data? Start fresh.
            JSONArray()
        }

        val result = mutableListOf<GeofenceEntity>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val entity = jsonToEntity(obj)
            if (entity != null) {
                result.add(entity)
            }
        }
        return result
    }

    override fun getGeofenceById(id: Long): GeofenceEntity? {
        return getAllGeofences().firstOrNull { it.id == id }
    }

    override fun upsertGeofence(entity: GeofenceEntity): GeofenceEntity {
        val currentList = getAllGeofences().toMutableList()

        val finalEntity = if (entity.id == 0L) {
            // New entity: assign a fresh id
            val newId = generateNextId()
            entity.copy(id = newId)
        } else {
            entity
        }

        // Remove any existing entity with the same id
        val existingIndex = currentList.indexOfFirst { it.id == finalEntity.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = finalEntity
        } else {
            currentList.add(finalEntity)
        }

        saveAll(currentList)
        return finalEntity
    }

    override fun deleteGeofence(id: Long) {
        val currentList = getAllGeofences().filterNot { it.id == id }
        saveAll(currentList)
    }

    override fun clearAll() {
        prefs.edit()
            .remove(AppDatabase.KEY_GEOFENCES)
            .apply()
    }

    // ---------- Internal helpers ----------

    private fun saveAll(entities: List<GeofenceEntity>) {
        val array = JSONArray()
        for (entity in entities) {
            array.put(entityToJson(entity))
        }
        prefs.edit()
            .putString(AppDatabase.KEY_GEOFENCES, array.toString())
            .apply()
    }

    private fun generateNextId(): Long {
        val current = prefs.getLong(AppDatabase.KEY_NEXT_ID, 1L)
        val next = if (current <= 0L) 1L else current
        prefs.edit()
            .putLong(AppDatabase.KEY_NEXT_ID, next + 1L)
            .apply()
        return next
    }

    private fun entityToJson(entity: GeofenceEntity): JSONObject {
        return JSONObject().apply {
            put("id", entity.id)
            put("name", entity.name)
            put("latitude", entity.latitude)
            put("longitude", entity.longitude)
            put("radiusMeters", entity.radiusMeters.toDouble())
        }
    }

    private fun jsonToEntity(obj: JSONObject): GeofenceEntity? {
        return try {
            GeofenceEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                latitude = obj.getDouble("latitude"),
                longitude = obj.getDouble("longitude"),
                radiusMeters = obj.getDouble("radiusMeters").toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
}