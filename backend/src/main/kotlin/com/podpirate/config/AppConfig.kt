package com.podpirate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.Executor

@ConfigurationProperties(prefix = "podpirate")
data class PodPirateProperties(
    val audioDir: String = "/data/audio",
    val processedDir: String = "/data/processed",
    val whisperUrl: String = "http://whisper-service:8000",
    val ollamaUrl: String = "http://ollama:11434",
    val ollamaModel: String = "llama3.2",
)

@Configuration
@EnableConfigurationProperties(PodPirateProperties::class)
class AppConfig {

    @Bean
    fun webClient(): WebClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
        .build()

    @Bean("taskExecutor")
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("podpirate-")
        executor.initialize()
        return executor
    }
}
