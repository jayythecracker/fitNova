package uk.ncc.fitNova.workout

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Shared workout parent so each workout type only defines its unique data,
 * calorie rule, and extra payload fields while the common backend shape stays in one place.
 */
abstract class BaseWorkoutSession(
    val userId: Int,
    val workoutType: String,
    val durationSeconds: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    protected val userWeightKg: Int
) {
    abstract fun calculateCalories(): Double

    fun toJson(): Map<String, String> {
        return linkedMapOf(
            "phpFunction" to "saveWorkoutSession",
            "userId" to userId.toString(),
            "workoutType" to workoutType,
            "durationSeconds" to durationSeconds.toString(),
            "totalSets" to totalSets.toString(),
            "totalReps" to totalReps.toString(),
            "totalVolume" to formatDecimal(totalVolume),
            "caloriesBurned" to formatDecimal(calculateCalories())
        ).apply {
            putAll(extraJsonFields())
        }
    }

    protected open fun extraJsonFields(): Map<String, String> = emptyMap()

    protected fun formatDecimal(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}

class StrengthWorkoutSession(
    userId: Int,
    durationSeconds: Int,
    totalSets: Int,
    totalReps: Int,
    totalVolume: Double,
    userWeightKg: Int,
    private val setEntries: List<WeightLiftingSetEntry>
) : BaseWorkoutSession(
    userId = userId,
    workoutType = "weight_lifting",
    durationSeconds = durationSeconds,
    totalSets = totalSets,
    totalReps = totalReps,
    totalVolume = totalVolume,
    userWeightKg = userWeightKg
) {
    override fun calculateCalories(): Double = 0.0

    override fun extraJsonFields(): Map<String, String> {
        return mapOf("setLogJson" to buildSetLogJson())
    }

    private fun buildSetLogJson(): String {
        return JSONArray().apply {
            setEntries.forEachIndexed { index, entry ->
                put(
                    JSONObject().apply {
                        put("setNumber", index + 1)
                        put("exercise", entry.exercise)
                        put("weightKg", entry.weightKg)
                        put("reps", entry.reps)
                    }
                )
            }
        }.toString()
    }
}

class OutdoorWorkoutSession(
    userId: Int,
    workoutType: String,
    durationSeconds: Int,
    userWeightKg: Int,
    private val distanceMeters: Double,
    private val routeName: String,
    private val destinationPoint: LatLng?,
    private val remainingDistanceMeters: Double,
    private val destinationReached: Boolean,
    private val targetDurationSeconds: Int,
    private val targetDurationReached: Boolean,
    private val distanceGoalMeters: Double,
    private val caloriesGoal: Double,
    private val routePoints: List<LatLng>
) : BaseWorkoutSession(
    userId = userId,
    workoutType = workoutType,
    durationSeconds = durationSeconds,
    totalSets = 0,
    totalReps = 0,
    totalVolume = 0.0,
    userWeightKg = userWeightKg
) {
    override fun calculateCalories(): Double {
        if (userWeightKg <= 0) {
            return 0.0
        }

        val distanceKm = distanceMeters / 1000.0
        val multiplier = when (workoutType) {
            "running" -> 1.0
            "cycling" -> 0.6
            else -> 0.75
        }
        return distanceKm * userWeightKg * multiplier
    }

    override fun extraJsonFields(): Map<String, String> {
        return linkedMapOf(
            "distanceMeters" to formatDecimal(distanceMeters),
            "routeName" to routeName,
            "destinationLat" to destinationPoint?.latitude?.let(::formatDecimal).orEmpty(),
            "destinationLng" to destinationPoint?.longitude?.let(::formatDecimal).orEmpty(),
            "remainingDistanceMeters" to formatDecimal(remainingDistanceMeters),
            "destinationReached" to if (destinationReached) "1" else "0",
            "targetDurationSeconds" to targetDurationSeconds.toString(),
            "targetDurationReached" to if (targetDurationReached) "1" else "0",
            "distanceGoalMeters" to formatDecimal(distanceGoalMeters),
            "caloriesGoal" to formatDecimal(caloriesGoal),
            "routeJson" to buildRouteJson(),
            "setLogJson" to "[]"
        )
    }

    private fun buildRouteJson(): String {
        return JSONArray().apply {
            routePoints.forEachIndexed { index, point ->
                put(
                    JSONObject().apply {
                        put("pointNumber", index + 1)
                        put("lat", point.latitude)
                        put("lng", point.longitude)
                    }
                )
            }
        }.toString()
    }
}
