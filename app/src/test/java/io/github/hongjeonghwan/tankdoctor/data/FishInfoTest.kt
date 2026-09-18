package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FishInfoTest {
    @Test
    fun `fish list round trips through preferences format`() {
        val fish = listOf(FishInfo("구피", 3), FishInfo("네온테트라", 5))

        assertEquals(fish, FishInfo.decode(FishInfo.encode(fish)))
    }

    @Test
    fun `invalid fish entries are ignored`() {
        val encoded = "[{\"name\":\"\",\"count\":2},{\"name\":\"구피\",\"count\":0}]"

        assertEquals(emptyList<FishInfo>(), FishInfo.decode(encoded))
    }
}
