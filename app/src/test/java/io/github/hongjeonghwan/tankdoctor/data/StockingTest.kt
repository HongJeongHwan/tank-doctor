package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockingTest {
    // 60x30x36cm = 64L outer, 54.4L of water after the 15% allowance.
    private val tank = TankSize(60, 30, 36)

    @Test
    fun `density needs both a tank size and at least one species`() {
        assertNull(Stocking.of(emptyList(), tank, TankType.FRESH))
        assertNull(Stocking.of(listOf(FishInfo("구피", 3, 4.0)), TankSize(), TankType.FRESH))
    }

    @Test
    fun `body length is summed and compared with the freshwater limit`() {
        val fish = listOf(FishInfo("구피", 6, 4.0), FishInfo("네온테트라", 10, 3.0))

        val stocking = requireNotNull(Stocking.of(fish, tank, TankType.FRESH))

        assertEquals(54.0, stocking.loadCm, 0.01) // 24 + 30
        assertEquals(16, stocking.totalCount)
        assertEquals(54.4, stocking.effectiveLiters, 0.01)
        assertEquals(165, stocking.percent) // 0.99cm/L against a 0.6cm/L limit
        assertEquals(StockingLevel.OVER, stocking.level)
        assertEquals(0.0, stocking.headroomCm, 0.01)
    }

    @Test
    fun `shrimp count far less than a fish of the same length`() {
        val shrimp = requireNotNull(
            Stocking.of(listOf(FishInfo("체리새우", 10, 3.0, FishKind.SHRIMP)), tank, TankType.FRESH)
        )

        assertEquals(6.0, shrimp.loadCm, 0.01) // 30cm of shrimp at a 0.2 bioload
        assertEquals(StockingLevel.ROOMY, shrimp.level)
    }

    @Test
    fun `a missing adult size falls back to the default for that kind`() {
        val stocking = requireNotNull(Stocking.of(listOf(FishInfo("이름 모를 물고기", 4)), tank, TankType.FRESH))

        assertEquals(20.0, stocking.loadCm, 0.01) // 4 fish at the 5cm default
        assertTrue(stocking.estimated)
    }

    @Test
    fun `marine tanks hit the limit with half the fish`() {
        val fish = listOf(FishInfo("니모", 4, 5.0))

        val fresh = requireNotNull(Stocking.of(fish, tank, TankType.FRESH))
        val marine = requireNotNull(Stocking.of(fish, tank, TankType.MARINE))

        assertEquals(61, fresh.percent)
        assertEquals(StockingLevel.OK, fresh.level)
        assertEquals(123, marine.percent) // the same fish, against a limit half as high
        assertEquals(StockingLevel.WATCH, marine.level)
    }

    @Test
    fun `headroom says how much more fits`() {
        val stocking = requireNotNull(Stocking.of(listOf(FishInfo("구피", 5, 4.0)), tank, TankType.FRESH))

        assertEquals(StockingLevel.OK, stocking.level)
        assertEquals(12.64, stocking.headroomCm, 0.01) // 54.4L * 0.6 - 20cm
    }
}
