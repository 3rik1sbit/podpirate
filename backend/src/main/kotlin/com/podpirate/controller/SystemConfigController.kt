package com.podpirate.controller

import com.podpirate.service.SystemConfigService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/config")
class SystemConfigController(
    private val systemConfigService: SystemConfigService,
) {

    @GetMapping("/ai-paused")
    fun getAiPaused(): Map<String, Boolean> {
        return mapOf("paused" to systemConfigService.isAiPaused())
    }

    @PostMapping("/ai-paused")
    fun setAiPaused(@RequestBody body: Map<String, Boolean>): ResponseEntity<Map<String, Any>> {
        val paused = body["paused"] ?: false
        return try {
            systemConfigService.setAiPaused(paused)
            ResponseEntity.ok(mapOf("paused" to systemConfigService.isAiPaused()))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf("error" to "AI services unavailable", "details" to (e.message ?: "Unknown error"))
            )
        }
    }
}
