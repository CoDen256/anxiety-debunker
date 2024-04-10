package coden.anxiety.debunker.postgres

data class DatasourceConfig(
    val url: String,
    val user: String,
    val password: String,
    val driverClassName: String? = null,
)