package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** Kullanıcı profili modeli. */
@Serializable
data class User(
    val id: String,
    val displayName: String,
    val email: String = "",
    val familyId: String? = null
)

/** Aile havuzu grubu modeli. */
@Serializable
data class FamilyGroup(
    val id: String,
    val name: String,
    val inviteCode: String,
    val memberIds: List<String>,
    val createdAt: Long
)
