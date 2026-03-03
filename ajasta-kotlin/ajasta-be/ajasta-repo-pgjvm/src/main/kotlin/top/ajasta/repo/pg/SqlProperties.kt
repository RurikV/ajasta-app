package top.ajasta.repo.pg

/**
 * Configuration properties for PostgreSQL connection.
 */
data class SqlProperties(
    val host: String = "localhost",
    val port: Int = 5432,
    val user: String = "postgres",
    val password: String = "ajasta-pass",
    val database: String = "ajasta_db",
    val schema: String = "public",
    val bookingsTable: String = "bookings",
    val resourcesTable: String = "resources",
) {
    val url: String
        get() = "jdbc:postgresql://${host}:${port}/${database}"
}
