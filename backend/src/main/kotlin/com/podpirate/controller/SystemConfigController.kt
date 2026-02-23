package com.podpirate.controller

import com.podpirate.service.SystemConfigService
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
    fun setAiPaused(@RequestBody body: Map<String, Boolean>): Map<String, Boolean> {
        val paused = body["paused"] ?: false
        systemConfigService.setAiPaused(paused)
        return mapOf("paused" to systemConfigService.isAiPaused())
    }
}
