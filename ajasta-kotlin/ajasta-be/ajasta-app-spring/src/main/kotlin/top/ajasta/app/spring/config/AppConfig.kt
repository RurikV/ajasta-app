package top.ajasta.app.spring.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import top.ajasta.app.common.AjastaProcessorImpl
import top.ajasta.app.common.AjastaProcessor
import top.ajasta.app.common.IAjastaAppSettings

@Configuration
class AppConfig {

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
