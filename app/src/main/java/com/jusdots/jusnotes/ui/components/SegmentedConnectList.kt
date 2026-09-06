package com.jusdots.jusnotes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val GroupLargeCorner = 28.dp
private val GroupSmallCorner = 6.dp

private fun segmentedShape(index: Int, count: Int): Shape =
    when {
        count <= 1 -> RoundedCornerShape(GroupLargeCorner)
        index == 0 -> RoundedCornerShape(
            topStart = GroupLargeCorner,
            topEnd = GroupLargeCorner,
            bottomEnd = GroupSmallCorner,
            bottomStart = GroupSmallCorner,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = GroupSmallCorner,
            topEnd = GroupSmallCorner,
            bottomEnd = GroupLargeCorner,
            bottomStart = GroupLargeCorner,
        )
        else -> RoundedCornerShape(GroupSmallCorner)
    }

@Composable
fun <T> SegmentedConnectList(
    items: List<T>,
    key: (T) -> Any? = { it.hashCode() },
    content: @Composable (item: T, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp),
    ) {
        items.forEachIndexed { index, item ->
            Card(
                shape = segmentedShape(index = index, count = items.size),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                content(item, index)
            }
        }
    }
}
