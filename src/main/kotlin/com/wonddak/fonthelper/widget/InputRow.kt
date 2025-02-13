package com.wonddak.fonthelper.widget

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.material.TextField
import androidx.compose.material.Text

@Composable
fun InputRow(
    title: String,
    text: String,
    onValueChange: (text: String) -> Unit,
    otherContent : @Composable RowScope.() -> Unit = {}
) {
    LabelContent(
        title
    ) {
        TextField(
            value = text,
            onValueChange = onValueChange,
            placeholder = {
                Text(title)
            }
        )
        otherContent()
    }
}