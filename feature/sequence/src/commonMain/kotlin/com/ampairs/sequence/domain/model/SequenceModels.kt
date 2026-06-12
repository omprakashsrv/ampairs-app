package com.ampairs.sequence.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirror of the server `SequenceDefinitionResponse`/`Request` (snake_case contract). */
@Serializable
data class SequenceDefinition(
    val uid: String = "",
    @SerialName("entity_type") val entityType: String = "",
    val scope: String = SequenceScope.WORKSPACE,
    @SerialName("user_id") val userId: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    @SerialName("padding_length") val paddingLength: Int = 0,
    @SerialName("start_value") val startValue: Long = 1,
    @SerialName("increment_step") val incrementStep: Int = 1,
    @SerialName("current_value") val currentValue: Long = 0,
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

object SequenceScope {
    const val WORKSPACE = "WORKSPACE"
    const val USER = "USER"
}

object AllocationStatus {
    const val ACTIVE = "ACTIVE"
    const val EXHAUSTED = "EXHAUSTED"
    const val RELEASED = "RELEASED"
}

/** Mirror of the server `SequenceAllocationResponse` — carries the format snapshot at grant time. */
@Serializable
data class SequenceAllocation(
    val uid: String = "",
    @SerialName("definition_uid") val definitionUid: String = "",
    @SerialName("entity_type") val entityType: String = "",
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("range_start") val rangeStart: Long = 0,
    @SerialName("range_end") val rangeEnd: Long = 0,
    @SerialName("next_available") val nextAvailable: Long = 0,
    val status: String = AllocationStatus.ACTIVE,
    val prefix: String? = null,
    val suffix: String? = null,
    @SerialName("padding_length") val paddingLength: Int = 0,
    @SerialName("increment_step") val incrementStep: Int = 1,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SequenceAllocationRequest(
    @SerialName("entity_type") val entityType: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("block_size") val blockSize: Int = 50,
)

@Serializable
data class SequenceAllocationReportRequest(
    val uid: String,
    @SerialName("next_available") val nextAvailable: Long,
)

/**
 * Result of [com.ampairs.sequence.domain.SequenceNumberProvider.next].
 *
 * `provisional = true` means the device was offline with no usable block (FR-011): the
 * formatted value is a clearly-marked placeholder, and the consuming feature is responsible
 * for replacing it once connectivity returns.
 */
data class SequenceNumberResult(
    val entityType: String,
    val value: Long,
    val formatted: String,
    /** Definition prefix snapshot (consumers that keep a separate series column use this). */
    val prefix: String? = null,
    val provisional: Boolean = false,
)
