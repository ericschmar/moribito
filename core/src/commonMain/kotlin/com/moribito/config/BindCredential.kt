package com.moribito.config

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class BindCredential @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toString(),
    val label: String,
    val bindUser: String,
    val bindPass: String,
    val isDefault: Boolean = false
)
