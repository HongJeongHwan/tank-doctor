package io.github.hongjeonghwan.tankdoctor.data

import kotlin.math.roundToInt

enum class StockingLevel(val label: String, val tone: Level) {
    ROOMY("여유", Level.GOOD),
    OK("적정", Level.GOOD),
    WATCH("주의", Level.CAUTION),
    OVER("과밀", Level.DANGER),
}

/**
 * Rough stocking-density estimate: the bioload-weighted adult body length of
 * everything living in the tank, divided by the water the tank actually holds.
 *
 * It is a planning aid, not a water test - filtration, plants and feeding all
 * move the real limit.
 */
data class Stocking(
    /** Bioload-weighted total body length in cm. */
    val loadCm: Double,
    val totalCount: Int,
    val effectiveLiters: Double,
    val perLiter: Double,
    val maxPerLiter: Double,
    val percent: Int,
    val level: StockingLevel,
    /** True when at least one species had no adult size, so a default was assumed. */
    val estimated: Boolean,
) {
    /** Weighted body length that still fits under the recommended limit; 0 when already over. */
    val headroomCm: Double get() = (effectiveLiters * maxPerLiter - loadCm).coerceAtLeast(0.0)

    /** One line for the summary card and the Gemini prompt. */
    val summary: String
        get() = "환산 체장 ${trimNumber(loadCm)}cm ÷ 실수량 약 ${trimNumber(effectiveLiters)}L " +
            "= ${String.format("%.2f", perLiter)}cm/L · 권장 상한 ${trimNumber(maxPerLiter)}cm/L 대비 ${percent}% (${level.label})"

    companion object {
        /** Substrate, rocks and the gap below the rim take roughly 15% of the outer volume. */
        const val WATER_RATIO = 0.85

        fun maxPerLiter(tankType: TankType): Double = when (tankType) {
            TankType.FRESH -> 0.6
            TankType.PLANTED -> 0.7 // plants take up nitrate, so a little more fits
            TankType.MARINE -> 0.3 // marine needs far more water per fish
        }

        fun of(fish: List<FishInfo>, tankSize: TankSize, tankType: TankType): Stocking? {
            val valid = fish.filter { it.isValid }
            if (!tankSize.isSet || valid.isEmpty()) return null
            val liters = tankSize.liters * WATER_RATIO
            if (liters <= 0) return null
            val loadCm = valid.sumOf { it.count * it.effectiveSizeCm * it.kind.bioload }
            val perLiter = loadCm / liters
            val max = maxPerLiter(tankType)
            val percent = (perLiter / max * 100).roundToInt()
            return Stocking(
                loadCm = loadCm,
                totalCount = valid.sumOf { it.count },
                effectiveLiters = liters,
                perLiter = perLiter,
                maxPerLiter = max,
                percent = percent,
                level = when {
                    percent < 60 -> StockingLevel.ROOMY
                    percent <= 100 -> StockingLevel.OK
                    percent <= 130 -> StockingLevel.WATCH
                    else -> StockingLevel.OVER
                },
                estimated = valid.any { it.sizeCm <= 0 },
            )
        }
    }
}
