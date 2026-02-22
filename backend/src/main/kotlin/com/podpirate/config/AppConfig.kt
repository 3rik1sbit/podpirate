package com.podpirate.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

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
    fun webClient(objectMapper: ObjectMapper): WebClient = WebClient.builder()
        .codecs {
            it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
            it.defaultCodecs().jackson2JsonDecoder(
                Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType("text", "javascript"))
            )
        }
        .build()

    @Bean("downloadExecutor")
    fun downloadExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 10000
        executor.setThreadNamePrefix("download-")
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }

    @Bean("processingExecutor")
    fun processingExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 10000
        executor.setThreadNamePrefix("processing-")
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }

    @Bean("aiExecutor")
    fun aiExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 3
        executor.maxPoolSize = 4
        executor.queueCapacity = 10000
        executor.setThreadNamePrefix("ai-")
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }
}
