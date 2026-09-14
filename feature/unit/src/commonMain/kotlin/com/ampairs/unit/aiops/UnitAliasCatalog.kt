package com.ampairs.unit.aiops

/**
 * Bundled, curated unit-of-measure alias table for AI Ops unit standardization (Slice 1, deliverable E).
 *
 * Maps a normalized short-name spelling (e.g. `KGS`, `KILO`, `LTR`, `LITRES`) to a single canonical
 * short name (`KG`, `L`, …). This is deterministic reference data — no LLM — so the capability that
 * uses it can score matches at HIGH confidence.
 *
 * It ships in the binary for now (small, hand-curated, extensible later / could move server-driven).
 * Keep it conservative: only add an alias when the canonical target is unambiguous.
 */
object UnitAliasCatalog {

    /** Canonical short names this catalog standardizes toward. */
    val canonicals: Set<String> = setOf("KG", "G", "L", "ML", "PCS", "DZN", "BOX", "M", "CM", "MM")

    /**
     * normalized spelling → canonical short name. The keys are already [normalize]d; the current
     * short name is normalized before lookup. Canonicals map to themselves so an exact-but-lowercase
     * spelling (e.g. `kg`) still resolves.
     */
    private val aliases: Map<String, String> = buildMap {
        fun group(canonical: String, vararg spellings: String) {
            put(normalize(canonical), canonical)
            spellings.forEach { put(normalize(it), canonical) }
        }
        group("KG", "kgs", "kg", "kilo", "kilos", "kilogram", "kilograms", "kgm", "kilo gram")
        group("G", "g", "gm", "gms", "gram", "grams", "gramme", "grammes")
        group("L", "l", "lt", "ltr", "ltrs", "litre", "litres", "liter", "liters")
        group("ML", "ml", "millilitre", "millilitres", "milliliter", "milliliters")
        group("PCS", "pc", "pcs", "piece", "pieces", "nos", "no", "unit", "units", "qty")
        group("DZN", "dz", "dzn", "doz", "dozen", "dozens")
        group("BOX", "box", "boxes", "bx")
        group("M", "m", "mtr", "mtrs", "metre", "metres", "meter", "meters")
        group("CM", "cm", "centimetre", "centimetres", "centimeter", "centimeters")
        group("MM", "mm", "millimetre", "millimetres", "millimeter", "millimeters")
    }

    /**
     * Normalize a short name for comparison: trim, uppercase, drop a trailing period, and collapse
     * internal whitespace. Pure — safe for all KMP targets.
     */
    fun normalize(shortName: String): String =
        shortName.trim().trimEnd('.').replace(Regex("\\s+"), " ").uppercase()

    /**
     * The canonical short name for [shortName], or `null` if this spelling isn't a known alias.
     * Returns the canonical even when [shortName] is already canonical — callers compare against the
     * current value to decide whether a change is actually needed.
     */
    fun canonicalFor(shortName: String): String? {
        if (shortName.isBlank()) return null
        return aliases[normalize(shortName)]
    }
}
