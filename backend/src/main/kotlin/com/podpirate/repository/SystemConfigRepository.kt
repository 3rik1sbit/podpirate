package com.podpirate.repository

import com.podpirate.model.SystemConfig
import org.springframework.data.jpa.repository.JpaRepository

interface SystemConfigRepository : JpaRepository<SystemConfig, String>
