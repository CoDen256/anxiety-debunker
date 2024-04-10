package coden.anxiety.debunker.postgres

import org.jetbrains.exposed.sql.Database

fun database(config: DatabaseConfig): Database {
    return Database.connect(
        url = config.url,
        user = config.user,
        password = config.password
    )
}