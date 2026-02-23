package com.podpirate.model

import jakarta.persistence.*

@Entity
@Table(name = "system_config")
data class SystemConfig(
    @Id
    val key: String = "",
    val value: String = "",
)
