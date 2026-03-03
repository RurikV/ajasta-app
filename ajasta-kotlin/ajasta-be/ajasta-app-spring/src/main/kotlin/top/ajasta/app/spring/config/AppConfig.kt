package top.ajasta.app.spring.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import top.ajasta.app.common.AjastaProcessorImpl
import top.ajasta.app.common.AjastaProcessor
import top.ajasta.app.common.IAjastaAppSettings
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource
import top.ajasta.repo.inmemory.RepoBookingInMemory
import top.ajasta.repo.inmemory.RepoResourceInMemory
import top.ajasta.repo.pg.RepoBookingSql
import top.ajasta.repo.pg.RepoResourceSql
import top.ajasta.repo.pg.SqlProperties

@Configuration
class AppConfig {

    /**
     * PostgreSQL repositories for production.
     * Uses Exposed SQL framework with JDBC connection.
     */
    @Configuration
    @Profile("prod")
    class ProdConfig {
        @Bean
        fun sqlProperties(
            @Value("\${spring.datasource.host:ajasta-postgres}") host: String,
            @Value("\${spring.datasource.port:5432}") port: Int,
            @Value("\${spring.datasource.username:admin}") user: String,
            @Value("\${spring.datasource.password:adminpw}") password: String,
            @Value("\${spring.datasource.database:ajastadb}") database: String,
            @Value("\${ajasta.db.schema:public}") schema: String,
            @Value("\${ajasta.db.tables.resources:resources}") resourcesTable: String,
            @Value("\${ajasta.db.tables.bookings:bookings}") bookingsTable: String,
        ): SqlProperties = SqlProperties(
            host = host,
            port = port,
            user = user,
            password = password,
            database = database,
            schema = schema,
            resourcesTable = resourcesTable,
            bookingsTable = bookingsTable,
        )

        @Bean
        fun repoBooking(sqlProperties: SqlProperties): IRepoBooking = RepoBookingSql(sqlProperties)

        @Bean
        fun repoResource(sqlProperties: SqlProperties): IRepoResource = RepoResourceSql(sqlProperties)
    }

    /**
     * In-memory repositories for development/testing.
     * Data is lost on restart.
     */
    @Configuration
    @Profile("!prod")
    class DevConfig {
        @Bean
        fun repoBooking(): IRepoBooking = RepoBookingInMemory()

        @Bean
        fun repoResource(): IRepoResource = RepoResourceInMemory()
    }

    @Bean
    fun appSettings(): AjastaAppSettings = AjastaAppSettings()

    @Bean
    fun processor(): AjastaProcessor = AjastaProcessorImpl()
}

/**
 * Application settings implementation.
 * Provides business logic processor for handling requests.
 */
class AjastaAppSettings(
    override val processor: AjastaProcessor = AjastaProcessorImpl()
) : IAjastaAppSettings
