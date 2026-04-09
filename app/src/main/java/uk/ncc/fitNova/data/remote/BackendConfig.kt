package uk.ncc.fitNova.data.remote

object BackendConfig {
    private const val BASE_URL = "http://10.0.2.2/backend"

    const val LOGIN_URL = "$BASE_URL/LoginDAO.php"
    const val REGISTRATION_URL = "$BASE_URL/RegistrationDAO.php"
    const val WORKOUT_URL = "$BASE_URL/WorkoutDAO.php"
    const val PROFILE_URL = "$BASE_URL/ProfileDAO.php"
}
