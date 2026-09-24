package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CurrentFishTest {
    private val countedOn = LocalDate.of(2026, 9, 20)
    private val registered = listOf(FishInfo("네온테트라", 3, 3.5), FishInfo("구피", 2, 4.0))

    private fun fishEntry(day: Int, vararg changes: FishChange) = LogEntry(
        id = day.toLong(),
        date = LocalDate.of(2026, 9, day),
        category = LogCategory.FISH,
        note = "",
        changesJson = FishChange.encode(changes.toList()),
    )

    @Test
    fun `the registered list stands on its own`() {
        assertEquals(registered, currentFish(registered, countedOn, emptyList()))
    }

    @Test
    fun `entries written before the count are already included`() {
        val entries = listOf(fishEntry(18, FishChange("네온테트라", 2, FishEvent.ADD)))

        assertEquals(registered, currentFish(registered, countedOn, entries))
    }

    @Test
    fun `an entry on the count day does not double up`() {
        val entries = listOf(fishEntry(20, FishChange("네온테트라", 2, FishEvent.ADD)))

        assertEquals(registered, currentFish(registered, countedOn, entries))
    }

    @Test
    fun `later additions and losses move the count`() {
        val entries = listOf(
            fishEntry(21, FishChange("네온테트라", 2, FishEvent.ADD)),
            fishEntry(22, FishChange("구피", 1, FishEvent.LOSS)),
        )

        assertEquals(
            listOf(FishInfo("네온테트라", 5, 3.5), FishInfo("구피", 1, 4.0)),
            currentFish(registered, countedOn, entries),
        )
    }

    @Test
    fun `a species stocked after the count joins the list`() {
        val entries = listOf(fishEntry(23, FishChange("코리도라스", 4, FishEvent.ADD)))

        assertEquals(
            registered + FishInfo("코리도라스", 4),
            currentFish(registered, countedOn, entries),
        )
    }

    @Test
    fun `a species that is all gone drops off the list`() {
        val entries = listOf(fishEntry(21, FishChange("구피", 5, FishEvent.LOSS)))

        assertEquals(
            listOf(FishInfo("네온테트라", 3, 3.5)),
            currentFish(registered, countedOn, entries),
        )
    }

    @Test
    fun `without a count date every entry applies`() {
        val entries = listOf(fishEntry(18, FishChange("구피", 1, FishEvent.BIRTH)))

        assertEquals(
            listOf(FishInfo("네온테트라", 3, 3.5), FishInfo("구피", 3, 4.0)),
            currentFish(registered, null, entries),
        )
    }

    @Test
    fun `other categories are left alone`() {
        val entries = listOf(
            LogEntry(1, LocalDate.of(2026, 9, 22), LogCategory.WATER, "구피 2마리 추가라고 적어도 환수 기록"),
        )

        assertEquals(registered, currentFish(registered, countedOn, entries))
    }
}
