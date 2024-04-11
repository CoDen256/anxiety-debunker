package io.github.coden.anxiety.debunker.postgres

data class DatasourceConfig(
    val inmemory: Boolean = true,
    val url: String,
    val user: String,
    val password: String,
    val driverClassName: String? = null,
)