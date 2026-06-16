package com.example.pup_lagoon_app.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream

class FoodRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: FoodRepository

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        val assetManager = mock(android.content.res.AssetManager::class.java)
        `when`(context.assets).thenReturn(assetManager)

        // Mock CSVs with landmarks needed for logic
        val mapLabelsCsv = """
            id,text,pixel_x,pixel_y,type,rotation
            L01,Gate 1,100,100,LANDMARK,0
            L02,Obelisk,2448,1360,BUILDING,0
            L03,NALLRC,1499,744,BUILDING,0
            L04,Gate 2,500,500,LANDMARK,0
            L05,Gate 3,300,300,LANDMARK,0
        """.trimIndent()

        `when`(assetManager.open("map_labels.csv")).thenReturn(ByteArrayInputStream(mapLabelsCsv.toByteArray()))
        `when`(assetManager.open("stall_locations.csv")).thenReturn(ByteArrayInputStream("id,pixel_x,pixel_y\nS01,150,150\nS02,450,450".toByteArray()))
        `when`(assetManager.open("food_records.csv")).thenReturn(ByteArrayInputStream("stall_id,stall_name,food_id,name,price,categories\nS01,Stall 1,F01,Burger,50,Fast Food".toByteArray()))

        repository = FoodRepository(context)
    }

    @Test
    fun testFindNearestGate() {
        val stallLocation = Offset(110f, 110f)
        val nearestGate = repository.findNearestGate(stallLocation)
        assertNotNull(nearestGate)
        assertEquals("Gate 1", nearestGate?.text)
    }

    @Test
    fun testCalculatePathWithLandmarks() {
        val gate1 = MapLabel("L01", "Gate 1", 100f, 100f, LabelType.LANDMARK, 0f)
        val stall = StallLocation("S01", 1902f, 744f)
        
        // Now has an exit waypoint
        val path = repository.calculatePath(gate1, stall)
        
        assertEquals(3, path.size)
        assertEquals(Offset(100f, 100f), path[0])
        assertEquals(Offset(2139f, 1360f), path[1])
        assertEquals(Offset(1902f, 744f), path[2])
    }

    @Test
    fun testGetDirectionText() {
        val gate1 = MapLabel("L01", "Gate 1", 2235f, 1360f, LabelType.LANDMARK, 0f)
        val stallNorth = StallLocation("S01", 1902f, 744f)
        
        val direction = repository.getDirectionText(gate1, stallNorth)
        assert(direction.contains("turn right (North)"))
        
        val gate3 = MapLabel("L09", "Gate 3", 1299f, 1214f, LabelType.LANDMARK, 0f)
        val stallSouth = StallLocation("S01", 1902f, 1500f)
        
        val direction3 = repository.getDirectionText(gate3, stallSouth)
        assert(direction3.contains("turn right (South)"))
    }
}
