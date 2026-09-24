package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FishInfoTest {
    @Test
    fun `fish list round trips through preferences format`() {
        val fish = listOf(FishInfo("구피", 3, 4.0), FishInfo("체리새우", 5, 3.0, FishKind.SHRIMP))

        assertEquals(fish, FishInfo.decode(FishInfo.encode(fish)))
    }

    @Test
    fun `invalid fish entries are ignored`() {
        val encoded = "[{\"name\":\"\",\"count\":2},{\"name\":\"구피\",\"count\":0}]"

        assertEquals(emptyList<FishInfo>(), FishInfo.decode(encoded))
    }

    @Test
    fun `lists saved before sizes were added still load`() {
        val encoded = "[{\"name\":\"구피\",\"count\":3}]"

        assertEquals(listOf(FishInfo("구피", 3, 0.0, FishKind.FISH)), FishInfo.decode(encoded))
    }

    @Test
    fun `merge adds counts for a species already on the list`() {
        val current = listOf(FishInfo("구피", 3, 4.0))
        val scanned = listOf(FishInfo("구피", 2, 4.0), FishInfo("네온테트라", 6, 3.5))

        assertEquals(
            listOf(FishInfo("구피", 5, 4.0), FishInfo("네온테트라", 6, 3.5)),
            FishInfo.merge(current, scanned),
        )
    }

    @Test
    fun `merge fills in a size the user never entered`() {
        val merged = FishInfo.merge(listOf(FishInfo("구피", 3)), listOf(FishInfo("구피", 1, 4.0)))

        assertEquals(listOf(FishInfo("구피", 4, 4.0)), merged)
    }
}
