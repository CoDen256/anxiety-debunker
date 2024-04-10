package coden.anxiety.debunker.postgres

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driverClassName: String? = null,
)