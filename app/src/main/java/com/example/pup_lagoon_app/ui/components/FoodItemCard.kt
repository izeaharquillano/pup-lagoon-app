package com.example.pup_lagoon_app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pup_lagoon_app.data.MergedRecords
import com.example.pup_lagoon_app.ui.theme.Maroon

@Composable
fun FoodItemCard(record: MergedRecords) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = record.baseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = record.priceRange,
                    style = MaterialTheme.typography.titleMedium,
                    color = Maroon
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Stall: ${record.stallName}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Categories: ${record.categories.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            // this is for displaying sizes
            if (record.sizePrices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Sizes: ${record.sizePrices.joinToString(", ")}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
