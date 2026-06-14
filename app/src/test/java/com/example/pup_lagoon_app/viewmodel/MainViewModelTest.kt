package com.example.pup_lagoon_app.viewmodel

import androidx.compose.ui.geometry.Offset
import com.example.pup_lagoon_app.data.FoodRecord
import com.example.pup_lagoon_app.data.FoodRepository
import com.example.pup_lagoon_app.data.MergedRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FoodRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when searching, showResults is true`() {
        viewModel.onSearchQueryChange("Burger")
        assertTrue("showResults should be true when searching", viewModel.showResults)
    }

    @Test
    fun `when clearing search with no filters, showResults is false`() {
        // First search
        viewModel.onSearchQueryChange("Burger")
        assertTrue(viewModel.showResults)

        // Then clear
        viewModel.onSearchQueryChange("")
        assertFalse("showResults should be false when clearing search without filters", viewModel.showResults)
    }

    @Test
    fun `when clearing search with filters, showResults is true if it was already true`() {
        // Apply filter
        viewModel.onApplyFilters(setOf("Drinks"), "", "")
        
        // Typing something
        viewModel.onSearchQueryChange("Soda")
        assertTrue(viewModel.showResults)

        // Clear query
        viewModel.onSearchQueryChange("")
        assertTrue("showResults should remain true if filters are active", viewModel.showResults)
    }
    
    @Test
    fun `when a result is selected, showResults is false`() {
        val record = MergedRecords(
            id = "1_Burger",
            stallId = "1",
            baseName = "Burger",
            stallName = "Stall 1",
            categories = listOf("Fast Food"),
            sizePrices = listOf("S - 50"),
            priceRange = "50",
            displayCategories = "Fast Food",
            displaySizes = "S - 50"
        )
        
        `when`(repository.getStallLocation("1")).thenReturn(null)
        `when`(repository.getStallImages("1")).thenReturn(emptyList())
        
        viewModel.onSearchQueryChange("Burger")
        assertTrue(viewModel.showResults)
        
        viewModel.selectResult(record)
        assertFalse("showResults should be false when a result is selected", viewModel.showResults)
        
        // Clearing search now should NOT show results because filters are empty
        viewModel.onSearchQueryChange("")
        assertFalse("showResults should remain false when clearing search after selection", viewModel.showResults)
    }
}
