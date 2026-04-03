package org.davidnardya.vocalcoach.domain.models

/** 50 levels per era; era index is 0-based: Era 1 → 0, Era 2 → 1, … */
const val VOCAL_LEVELS_PER_ERA: Int = 50

/** Shown on the welcome progress bar as the “full” milestone before open-ended content. */
const val VOCAL_ROADMAP_DISPLAY_MAX_LEVEL: Int = 200

/**
 * Current era index (0-based), same as `(currentLevel - 1) / 50` for level ≥ 1.
 */
fun eraIndexFromLevel(level: Int): Int =
    (level.coerceAtLeast(1) - 1) / VOCAL_LEVELS_PER_ERA

/**
 * Position within the current era: 1..50 (clamped).
 */
fun positionWithinEra(level: Int): Int {
    val idx = eraIndexFromLevel(level)
    val eraStart = idx * VOCAL_LEVELS_PER_ERA + 1
    return (level - eraStart + 1).coerceIn(1, VOCAL_LEVELS_PER_ERA)
}

/**
 * LiteRT confidence required for a successful growl detection, scaled every 10 levels inside an era.
 * Example: level 1 → 60%, level 49 → 85% (five 10-level bands).
 */
fun techniqueConfidenceThreshold01(level: Int): Float {
    val band = ((level.coerceAtLeast(1) - 1) % VOCAL_LEVELS_PER_ERA) / 10
    return 0.60f + (band / 4f) * 0.25f
}
