package coden.anxiety.debunker.postgres

import org.jetbrains.exposed.sql.Database
import java.time.Instant

fun database(config: DatasourceConfig): Database {
    return Database.connect(
        url = config.url,
        user = config.user,
        password = config.password
    )
}

fun Instant.asDBInstant(): kotlinx.datetime.Instant{
    return kotlinx.datetime.Instant.fromEpochMilliseconds(toEpochMilli())
}

fun kotlinx.datetime.Instant.asInstant(): Instant{
    return Instant.ofEpochMilli(toEpochMilliseconds())
}