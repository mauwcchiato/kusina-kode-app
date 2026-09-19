package com.example.kusinakode.ui.components

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.RegionChipInk
import com.example.kusinakode.ui.theme.RegionChipOff
import com.example.kusinakode.ui.theme.RegionChipOffStroke
import com.example.kusinakode.ui.theme.RegionChipOn

/**
 * A scrolling strip of filter pills.
 *
 * The one pill the app uses everywhere it filters a list — the region chips on
 * Explore and Learn, the period chips on the KK ledger, the kind chips in the
 * inbox. It lives here rather than in any one screen because three private
 * copies had already drifted into three near-misses.
 *
 * Scrolls sideways: these strips outgrow a phone's width as soon as a label
 * gets long, and a wrapped row of pills reads as two unrelated rows.
 */
@Composable
fun <T> FilterPillRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val on = option == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) RegionChipOn else RegionChipOff,
                border = if (on) null else BorderStroke(1.dp, RegionChipOffStroke),
                modifier = Modifier.clickable(onClick = clickSfx { onSelect(option) })
            ) {
                Text(
                    labelOf(option),
                    color = if (on) Color.White else RegionChipInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
