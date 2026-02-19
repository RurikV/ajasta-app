package top.ajasta.app.spring.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import top.ajasta.app.common.AjastaProcessorImpl
import top.ajasta.app.common.AjastaProcessor
import top.ajasta.app.common.IAjastaAppSettings
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource
import top.ajasta.repo.inmemory.RepoBookingInMemory
import top.ajasta.repo.inmemory.RepoResourceInMemory

@Configuration
class AppConfig {

    /**
     * Singleton in-memory repositories for development/testing.
     * In production, these would be replaced with PostgreSQL repositories.
     */
    @Bean
    fun repoBooking(): IRepoBooking = RepoBookingInMemory()

    @Bean
    fun repoResource(): IRepoResource = RepoResourceInMemory()

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
