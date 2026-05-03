package com.example.pup_lagoon_app
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.pup_lagoon_app.data.FoodRepository
import com.example.pup_lagoon_app.ui.theme.PuplagoonappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuplagoonappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context) }

    LaunchedEffect(Unit) {
        Log.d("FoodSearch", "--- Search by Category: Rice Meal ---")
        val categoryResults = repository.searchByCategory("Rice Meal")
        categoryResults?.forEach { record ->
            Log.d("FoodSearch", "Found: ${record.name} - ${record.price}")
        }

        Log.d("FoodSearch", "--- Search by Price Range: ₱50 to ₱70 ---")
        val priceResults = repository.searchByPriceRange(50.0, 70.0)
        priceResults.forEach { record ->
            Log.d("FoodSearch", "Found: ${record.name} - ${record.price} at ${record.stallName}")
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Greeting("Android")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun MainScreenPreview() {
    PuplagoonappTheme {
        MainScreen()
    }
}