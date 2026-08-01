package com.ampairs.agent.llm

/**
 * Device RAM for on-device model gating (FR-012). The raw read is platform-specific (expect/actual);
 * the RAM→tier policy is in [RamTiers] (pure/testable). Returns 0 when RAM can't be determined — the
 * caller treats 0 as "rule-based only".
 */
expect object DeviceCapability {
    fun totalRamBytes(): Long
}
