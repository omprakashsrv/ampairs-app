package com.ampairs.sequence.domain

import com.ampairs.common.DeviceService
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sequence.data.repository.SequenceAllocationRepository
import com.ampairs.sequence.data.repository.SequenceRepository
import com.ampairs.sequence.domain.model.SequenceNumberResult
import com.ampairs.sequence.util.SequenceLogger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Cross-feature entry point for sequence numbers (the StoreSettingsProvider of this module).
 * Inject into any ViewModel in the workspace graph and call [next] when creating an entity.
 *
 * Offline-first: numbers come from this device's server-granted block with zero network
 * calls. When the block is exhausted a new one is requested synchronously; if that fails
 * (offline), a clearly-marked provisional number is returned (FR-011) — the consuming
 * feature finalizes it after reconnect.
 *
 * Workspace-scoped singleton: the mutex must be shared by every caller in the workspace so
 * concurrent consumption from the same block can never double-issue a value.
 */
@OptIn(ExperimentalTime::class)
@Inject
@SingleIn(WorkspaceScope::class)
class SequenceNumberProvider(
    private val allocationRepository: SequenceAllocationRepository,
    private val definitionRepository: SequenceRepository,
    private val deviceService: DeviceService,
) {

    private val mutex = Mutex()

    suspend fun next(entityType: String, blockSize: Int = DEFAULT_BLOCK_SIZE): SequenceNumberResult =
        mutex.withLock {
            val deviceId = deviceService.getDeviceId()

            val allocation = allocationRepository.getUsableAllocation(entityType, deviceId)
                ?: allocationRepository.requestAllocation(entityType, deviceId, blockSize).getOrNull()
                ?: return provisional(entityType)

            val value = allocationRepository.consume(allocation).getOrElse { return provisional(entityType) }
            SequenceNumberResult(
                entityType = entityType,
                value = value,
                formatted = SequenceFormatter.format(
                    prefix = allocation.prefix,
                    suffix = allocation.suffix,
                    paddingLength = allocation.paddingLength,
                    value = value,
                ),
                prefix = allocation.prefix,
                provisional = false,
            )
        }

    /**
     * Preview the next number this device would issue, without consuming it. Requests a block
     * when the device has none (pre-warming the editor for the save that follows); returns null
     * only when offline with no capacity.
     */
    suspend fun peek(entityType: String, blockSize: Int = DEFAULT_BLOCK_SIZE): SequenceNumberResult? =
        mutex.withLock {
            val deviceId = deviceService.getDeviceId()
            val allocation = allocationRepository.getUsableAllocation(entityType, deviceId)
                ?: allocationRepository.requestAllocation(entityType, deviceId, blockSize).getOrNull()
                ?: return null
            SequenceNumberResult(
                entityType = entityType,
                value = allocation.nextAvailable,
                formatted = SequenceFormatter.format(
                    allocation.prefix, allocation.suffix, allocation.paddingLength, allocation.nextAvailable
                ),
                prefix = allocation.prefix,
                provisional = false,
            )
        }

    /**
     * Offline with no usable block: a placeholder marked with a `P` segment so it can never
     * collide with (or be mistaken for) a real sequence value.
     */
    private suspend fun provisional(entityType: String): SequenceNumberResult {
        val prefix = definitionRepository.getActiveWorkspaceDefinition(entityType)?.prefix
            ?: SequenceFormatter.defaultPrefix(entityType)
        val stamp = Clock.System.now().toEpochMilliseconds()
        SequenceLogger.w("SequenceNumberProvider", "No usable block for $entityType — issuing provisional number")
        return SequenceNumberResult(
            entityType = entityType,
            value = stamp,
            formatted = "$prefix-P$stamp",
            prefix = prefix,
            provisional = true,
        )
    }

    companion object {
        const val DEFAULT_BLOCK_SIZE = 50
    }
}
