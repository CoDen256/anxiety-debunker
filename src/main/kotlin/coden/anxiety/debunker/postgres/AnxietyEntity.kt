package coden.anxiety.debunker.postgres

import io.ebean.DatabaseFactory
import io.ebean.Model
import io.ebean.config.DatabaseConfig
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "anxieties")
class AnxietyEntity(
    id: String,
    description: String,
    created: Instant,
    ): Model() {

    @Id
    var id: String = id

    @Column
    var description: String = description

    @Column
    var created: Instant = created
}

fun main() {
    val cfg = DatabaseConfig().loadFromProperties(Properties().apply {
        put("datasource.db.username", "dev")
        put("datasource.db.password", "odLnwItWkE2jV0oagMFLjw")
        put("datasource.db.databaseUrl", "jdbc:postgresql://alpha-cluster-9383.7tc.aws-eu-central-1.cockroachlabs.cloud:26257/defaultdb")
        put("datasource.db.databaseDriver", "org.postgresql.Driver")
    })
    val db = DatabaseFactory.create(cfg);

    db.name()

    val anxietyEntity = AnxietyEntity(
        "test",
        "text",
        Instant.now()
    )


}