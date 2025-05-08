package com.wonddak.fonthelper.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabelContent(
    title :String,
    otherContent : @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.width(200.dp)
        )
        Spacer(Modifier.width(20.dp))
        otherContent()
    }
}